package com.mlacker.samples.netflix.eureka.registry

import com.google.common.cache.CacheBuilder
import com.mlacker.samples.netflix.appinfo.ActionType
import com.mlacker.samples.netflix.appinfo.InstanceInfo
import com.mlacker.samples.netflix.appinfo.InstanceStatus
import com.mlacker.samples.netflix.discovery.shared.Application
import com.mlacker.samples.netflix.discovery.shared.Applications
import com.netflix.appinfo.LeaseInfo
import com.netflix.discovery.EurekaClientConfig
import com.netflix.eureka.EurekaServerConfig
import com.netflix.eureka.lease.Lease
import com.netflix.eureka.registry.ResponseCache
import com.netflix.eureka.resources.ServerCodecs
import com.netflix.eureka.util.MeasuredRate
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.HashMap
import kotlin.math.min
import kotlin.random.Random

abstract class AbstractInstanceRegistry(
    protected val serverConfig: EurekaServerConfig,
    protected val clientConfig: EurekaClientConfig,
    protected val serverCodecs: ServerCodecs,
) : InstanceRegistry {

    private val registry: ConcurrentHashMap<String, MutableMap<String, Lease<InstanceInfo>>?> = ConcurrentHashMap()
    protected val overriddenInstanceStatusMap: ConcurrentMap<String, InstanceStatus> = CacheBuilder
        .newBuilder().initialCapacity(500)
        .expireAfterAccess(Duration.ofHours(1))
        .build<String, InstanceStatus>().asMap()

    private val readWriteLock: ReadWriteLock = ReentrantReadWriteLock()
    protected val read: Lock = readWriteLock.readLock()
    private val write: Lock = readWriteLock.writeLock()
    protected val lock: Any = Any()

    private val evictionTimer = Timer("Eureka-EvictionTimer", true)
    private val renewsLastMin: MeasuredRate = MeasuredRate(TimeUnit.MINUTES.toMillis(1))

    private val evictionTaskRef: AtomicReference<EvictionTask> = AtomicReference()

    @Volatile
    protected var numberOfRenewsPerMinThreshold: Int = 0

    @Volatile
    protected var expectedNumberOfClientsSendingRenews: Int = 0

    @Volatile
    private var responseCache: ResponseCache? = null

    // 131
    override fun initializedResponseCache() {
        if (responseCache == null) {
            // responseCache = ResponseCacheImpl(serverConfig, serverCodecs, this)
        }
    }

    protected fun initRemoteRegionRegistry() {
        TODO()
    }

    // 158
    override fun getResponseCache(): ResponseCache? = responseCache

    // 162
    fun getRegistrySize(): Long {
        return registry.values.sumBy { it!!.size }.toLong()
    }

    // 174
    override fun clearRegistry() {
        overriddenInstanceStatusMap.clear()
        registry.clear()
    }

    // 184
    override fun overriddenInstanceStatusesSnapshot(): Map<String, InstanceStatus> {
        return HashMap(overriddenInstanceStatusMap)
    }

    //184
    override fun register(r: InstanceInfo, leaseDuration: Int, isReplication: Boolean) {
        var registrant = r

        read.lock()
        try {
            var gMap = registry[registrant.appName]
            if (gMap == null) {
                val gNewMap = ConcurrentHashMap<String, Lease<InstanceInfo>>()
                gMap = registry.putIfAbsent(registrant.appName, gNewMap)
                if (gMap == null) {
                    gMap = gNewMap
                }
            }
            val existingLease = gMap[registrant.id]
            if (existingLease != null && existingLease.holder != null) {
                val existingLastDirtyTimestamp = existingLease.holder.lastDirtyTimestamp
                val registrationLastDirtyTimestamp = registrant.lastDirtyTimestamp

                if (existingLastDirtyTimestamp > registrationLastDirtyTimestamp) {
                    registrant = existingLease.holder
                }
            } else {
                synchronized(lock) {
                    // The lease does not exist and hence it is a new registration
                    if (this.expectedNumberOfClientsSendingRenews > 0) {
                        // Since the client wants to register it, increase the number of clients sending renews
                        this.expectedNumberOfClientsSendingRenews++
                        updateRenewsPerMinThreshold()
                    }
                }
            }
            val lease = Lease(registrant, leaseDuration)
            if (existingLease != null) {
                lease.serviceUpTimestamp = existingLease.serviceUpTimestamp
            }
            gMap[registrant.id] = lease
            if (InstanceStatus.UNKNOWN != registrant.overriddenStatus) {
                if ((!overriddenInstanceStatusMap.containsKey(registrant.id))) {
                    overriddenInstanceStatusMap[registrant.id] = registrant.overriddenStatus
                }
            }
            val overriddenStatusFormMap = overriddenInstanceStatusMap[registrant.id]
            if (overriddenStatusFormMap != null) {
                registrant.overriddenStatus = overriddenStatusFormMap
            }

            if (InstanceStatus.UP == registrant.status) {
                lease.serviceUp()
            }
            registrant.actionType = ActionType.ADDED
            registrant.setLastUpdatedTimestamp()
            invalidateCache(registrant.appName, registrant.vipAddress)
        } finally {
            read.unlock()
        }
    }

    // 288
    override fun cancel(appName: String, id: String, isReplication: Boolean): Boolean {
        return internalCancel(appName, id, isReplication)
    }

    // 297
    protected open fun internalCancel(appName: String, id: String, isReplication: Boolean): Boolean {
        read.lock()
        try {
            val gMap = registry[appName]
            val leaseToCancel = gMap?.remove(id) ?: return false
            overriddenInstanceStatusMap.remove(id)

            leaseToCancel.cancel()
            val instanceInfo = leaseToCancel.holder
            if (instanceInfo != null) {
                instanceInfo.actionType = ActionType.DELETED
                instanceInfo.setLastUpdatedTimestamp()
            }
            invalidateCache(appName, instanceInfo?.vipAddress)
        } finally {
            read.unlock()
        }

        synchronized(lock) {
            if ((this.expectedNumberOfClientsSendingRenews > 0)) {
                this.expectedNumberOfClientsSendingRenews--
                updateRenewsPerMinThreshold()
            }
        }

        return true
    }

    // 351
    override fun renew(appName: String, id: String, isReplication: Boolean): Boolean {
        val gMap = registry[appName]
        val leaseToRenew = gMap?.get(id) ?: return false

        renewsLastMin.increment()
        leaseToRenew.renew()
        return true
    }

    // 432
    override fun storeOverriddenStatusIfRequired(appName: String, id: String, overriddenStatus: InstanceStatus) {
        val instanceStatus = overriddenInstanceStatusMap[id]
        if (instanceStatus == null || overriddenStatus != instanceStatus) {
            overriddenInstanceStatusMap[id] = overriddenStatus
            val instanceInfo = this.getInstanceByAppAndId(appName, id)
            instanceInfo!!.overriddenStatus = overriddenStatus
        }
    }

    // 462
    override fun statusUpdate(appName: String, id: String, newStatus: InstanceStatus, lastDirtyTimestamp: String?, isReplication: Boolean): Boolean {
        read.lock()
        try {
            val gMap = registry[appName]
            val lease = gMap?.get(id) ?: return false

            lease.renew()
            val info = lease.holder
            if (info.status != newStatus) {
                if (InstanceStatus.UP == newStatus) {
                    lease.serviceUp()
                }
                overriddenInstanceStatusMap[id] = newStatus
                info.overriddenStatus = newStatus
                var replicaDirtyTimestamp = 0L
                info.setStatusWithoutDirty(newStatus)
                if (lastDirtyTimestamp != null) {
                    replicaDirtyTimestamp = lastDirtyTimestamp.toLong()
                }
                if (replicaDirtyTimestamp > info.lastDirtyTimestamp) {
                    info.lastDirtyTimestamp = replicaDirtyTimestamp
                }
                info.actionType = ActionType.MODIFIED
                info.setLastUpdatedTimestamp()
                invalidateCache(appName, info.vipAddress)
            }
            return true
        } finally {
            read.unlock()
        }
    }

    // 527
    override fun deleteStatusOverride(appName: String, id: String, newStatus: InstanceStatus, lastDirtyTimestamp: String?, isReplication: Boolean): Boolean {
        read.lock()
        try {
            val lease = registry[appName]?.get(id) ?: return false

            lease.renew()
            val info = lease.holder

            val currentOverride = overriddenInstanceStatusMap.remove(id)
            if (currentOverride != null && info != null) {
                info.overriddenStatus = InstanceStatus.UNKNOWN
                info.setStatusWithoutDirty(newStatus)
                lastDirtyTimestamp?.let {
                    val replicaDirtyTimestamp = it.toLong()
                    if ((replicaDirtyTimestamp > info.lastDirtyTimestamp)) {
                        info.lastDirtyTimestamp = replicaDirtyTimestamp
                    }
                }
                info.actionType = ActionType.MODIFIED
                info.setLastUpdatedTimestamp()
                invalidateCache(appName, info.vipAddress)
            }
            return true
        } finally {
            read.unlock()
        }
    }

    // 582
    override fun evict() {
        evict(0L)
    }

    // 586
    private fun evict(additionalLeaseMs: Long) {
        // Running the evict task

        if (!isLeaseExpirationEnabled()) {
            return
        }

        // We collect first all expired items, to evict them in random order. For large eviction sets,
        // if we do net that, we might wipe out whole apps before self preservation kicks in. By randomizing it,
        // the impact should be evenly distributed across all applications.
        val expiredLeases: MutableList<Lease<InstanceInfo>> = mutableListOf()
        for (leaseMap in registry.values) {
            if (leaseMap != null) {
                for (lease in leaseMap.values) {
                    if (lease.isExpired(additionalLeaseMs)) {
                        expiredLeases.add(lease)
                    }
                }
            }
        }

        // To compensate for GC pauses or drifting local time, we need to use current registry size as base for
        // triggering self-preservation. Without that we would wipe out full registry.
        val registrySize = getRegistrySize().toInt()
        val registrySizeThreshold = (registrySize * serverConfig.renewalPercentThreshold).toInt()
        val evictionLimit = registrySize - registrySizeThreshold

        val toEvict = min(expiredLeases.size, evictionLimit)
        if (toEvict > 0) {
            val random = Random(System.currentTimeMillis())
            for (i in 0 until toEvict) {
                val next = i + random.nextInt(expiredLeases.size - i)
                Collections.swap(expiredLeases, i, next)
                val lease = expiredLeases[i]

                val appName = lease.holder.appName
                val id = lease.holder.id
                internalCancel(appName, id, false)
            }
        }
    }

    // 662
    override fun getApplication(appName: String): Application? {
        var app: Application? = null
        val leaseMap = registry[appName]
        if (leaseMap != null && leaseMap.isNotEmpty()) {
            for (lease in leaseMap.values) {
                if (app == null) {
                    app = Application(appName)
                }
                app.addInstance(decorateInstanceInfo(lease))
            }
        }
        return app
    }

    // 692
    override fun getApplications(): Applications {
        val apps = Applications()

        for (entry in registry.entries) {
            var app: Application? = null

            if (entry.value != null) {
                for (lease in entry.value!!.values) {
                    if (app == null) {
                        app = Application(lease.holder.appName)
                    }
                    app.addInstance(decorateInstanceInfo(lease))
                }
            }
            if (app != null) {
                apps.addApplication(app)
            }
        }
        apps.appsHashCode = apps.getReconcileHashCode()
        return apps
    }

    // 1015
    override fun getInstanceByAppAndId(appName: String, id: String): InstanceInfo? {
        val lease = registry[appName]?.get(id) ?: return null

        if (!isLeaseExpirationEnabled() || !lease.isExpired) {
            return decorateInstanceInfo(lease)
        }

        return null
    }

    // 1058
    @Deprecated("Try getInstanceByAppAndId instead")
    override fun getInstancesById(id: String): List<InstanceInfo> {
        return registry.values.mapNotNull { it?.get(id) }
            .filter { !(isLeaseExpirationEnabled() && it.isExpired) }
            .map { decorateInstanceInfo(it) }
    }

    // 1109
    private fun decorateInstanceInfo(lease: Lease<InstanceInfo>): InstanceInfo {
        val info = lease.holder

        // client app settings
        var renewalInterval = LeaseInfo.DEFAULT_LEASE_RENEWAL_INTERVAL
        var leaseDuration = LeaseInfo.DEFAULT_LEASE_DURATION

        if (info.leaseInfo != null) {
            renewalInterval = info.leaseInfo.renewalIntervalInSecs
            leaseDuration = info.leaseInfo.durationInSecs
        }

        info.leaseInfo = LeaseInfo.Builder.newBuilder()
            .setRegistrationTimestamp(lease.registrationTimestamp)
            .setRenewalTimestamp(lease.lastRenewalTimestamp)
            .setServiceUpTimestamp(lease.serviceUpTimestamp)
            .setRenewalIntervalInSecs(renewalInterval)
            .setDurationInSecs(leaseDuration)
            .setEvictionTimestamp(lease.evictionTimestamp)
            .build()

        info.setIsCoordinatingDiscoveryServer()
        return info
    }

    // 1142
    override fun getNumOfRenewsInLastMin(): Long {
        return renewsLastMin.count
    }

    // 1155
    override fun getNumOfRenewsPerMinThreshold(): Int {
        return numberOfRenewsPerMinThreshold
    }

    override fun getLastNRegisteredInstances(): List<Pair<Long, String>> {
        TODO("Not yet implemented")
    }

    override fun getLastNCanceledInstances(): List<Pair<Long, String>> {
        TODO("Not yet implemented")
    }

    // 1183
    private fun invalidateCache(appName: String, vipAddress: String?) {
        responseCache!!.invalidate(appName, vipAddress, vipAddress)
    }

    // 1188
    protected fun updateRenewsPerMinThreshold() {
        this.numberOfRenewsPerMinThreshold = (this.expectedNumberOfClientsSendingRenews
                * (60.0 / serverConfig.expectedClientRenewalIntervalSeconds)
                * serverConfig.renewalPercentThreshold).toInt()
    }

    // 1212
    protected fun postInit() {
        renewsLastMin.start()
        evictionTaskRef.get()?.cancel()
        evictionTaskRef.set(EvictionTask())
        evictionTimer.schedule(evictionTaskRef.get(),
            serverConfig.evictionIntervalTimerInMs,
            serverConfig.evictionIntervalTimerInMs)
    }

    // 1227
    override fun shutdown() {
        evictionTimer.cancel()
        renewsLastMin.stop()
        responseCache!!.stop()
    }

    // 1239
    private inner class EvictionTask : TimerTask() {

        private val lastExecutionNanosRef = AtomicLong(0)

        override fun run() {
            try {
                val compensationTimeMs = getCompensationTimeMs()
                evict(compensationTimeMs)
            } catch (ex: Throwable) {
                println("Cloud not run the evict task")
            }
        }

        private fun getCompensationTimeMs(): Long {
            val currNanos = System.nanoTime()
            val lastNanos = lastExecutionNanosRef.getAndSet(currNanos)
            if (lastNanos == 0L) {
                return lastNanos
            }

            val elapsedMs = TimeUnit.NANOSECONDS.toMillis(currNanos - lastNanos)
            val compensationTime = elapsedMs - serverConfig.evictionIntervalTimerInMs
            return if (compensationTime > 0L) {
                compensationTime
            } else {
                0L
            }
        }
    }
}
