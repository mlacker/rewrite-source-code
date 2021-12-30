package com.mlacker.samples.netflix.discovery

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.mlacker.samples.netflix.appinfo.ActionType
import com.mlacker.samples.netflix.appinfo.ApplicationInfoManager
import com.mlacker.samples.netflix.appinfo.InstanceInfo
import com.mlacker.samples.netflix.appinfo.InstanceStatus
import com.mlacker.samples.netflix.discovery.shared.Application
import com.mlacker.samples.netflix.discovery.shared.Applications
import com.netflix.discovery.EurekaClientConfig
import com.netflix.discovery.TimedSupervisorTask
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.PreDestroy
import javax.inject.Singleton
import javax.ws.rs.core.Response


@Singleton
class DiscoveryClient(
    override val applicationInfoManager: ApplicationInfoManager,
    override val clientConfig: EurekaClientConfig,
    private val eurekaTransport: EurekaTransport,
) : EurekaClient {

    companion object {
        private const val PREFIX = "DiscoveryClient"
    }

    private val scheduler: ScheduledExecutorService?
    private val cacheRefreshExecutor: ThreadPoolExecutor?
    private val heartbeatExecutor: ThreadPoolExecutor?
    private lateinit var cacheRefreshTask: TimedSupervisorTask
    private lateinit var heartbeatTask: TimedSupervisorTask

    private val localRegionApps: AtomicReference<Applications> = AtomicReference(Applications())
    private var instanceInfo: InstanceInfo = applicationInfoManager.info


    private lateinit var instanceInfoReplicator: InstanceInfoReplicator

    @Volatile
    private var lastRemoteInstanceStatus: InstanceStatus = InstanceStatus.UNKNOWN
    private var lastSuccessfulRegistryFetchTimestamp = -1L
    private var lastSuccessfulHeartbeatTimestamp = -1L


    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        logger.info("Initializing Eureka")

        if (clientConfig.shouldRegisterWithEureka() && clientConfig.shouldFetchRegistry()) {
            try {
                // default size of 2 - 1 each for heartbeat and cacheRefresh
                this.scheduler = Executors.newScheduledThreadPool(
                    2, ThreadFactoryBuilder()
                        .setNameFormat("$PREFIX-%d")
                        .setDaemon(true)
                        .build()
                )

                this.heartbeatExecutor = ThreadPoolExecutor(
                    1, clientConfig.heartbeatExecutorThreadPoolSize, 0, TimeUnit.SECONDS,
                    SynchronousQueue(),
                    ThreadFactoryBuilder()
                        .setNameFormat("$PREFIX-HeartbeatExecutor-%d")
                        .setDaemon(true)
                        .build()
                )

                this.cacheRefreshExecutor = ThreadPoolExecutor(
                    1, clientConfig.cacheRefreshExecutorThreadPoolSize, 0, TimeUnit.SECONDS,
                    SynchronousQueue(),
                    ThreadFactoryBuilder()
                        .setNameFormat("$PREFIX-CacheRefreshExecutor-%d")
                        .setDaemon(true)
                        .build()
                )
            } catch (ex: Throwable) {
                throw RuntimeException("Failed to initialize DiscoverClient!", ex)
            }

            if (clientConfig.shouldFetchRegistry()) {
                try {
                    val primaryFetchRegistryResult = fetchRegistry()
                    if (!primaryFetchRegistryResult) {
                        logger.info("Initial registry fetch from primary servers failed")
                    }
                } catch (th: Throwable) {
                    logger.error("Fetch registry error at startup: {}", th.message)
                    throw IllegalStateException(th)
                }
            }

            // finally, init the schedule task (e.g. cluster resolvers, heartbeat, instanceInfo replicator, fetch
            initScheduledTasks()
        } else {
            logger.info("Client configured to neither register nor query for data.")

            // no need to setup up an network tasks and we are done
            this.scheduler = null
            this.heartbeatExecutor = null
            this.cacheRefreshExecutor = null
        }

        val initTimestampMs = System.currentTimeMillis()
        val initRegistrySize = this.getApplications().size()
        logger.info("Discovery Client initialized at timestamp {} with initial instances count: {}",
            initTimestampMs, initRegistrySize)
    }

    // 611
    override fun getApplication(appName: String): Application? {
        return getApplications().getRegisteredApplications(appName)
    }

    // 621
    override fun getApplications(): Applications {
        return localRegionApps.get()
    }

    // 651
    override fun getInstancesById(id: String): List<InstanceInfo> {
        return getApplications().registeredApplications
            .mapNotNull { it.getInstanceById(id) }
            .toList()
    }

    // 719
    override fun getInstancesByVipAddress(vipAddress: String?): List<InstanceInfo> {
        if (vipAddress == null) {
            throw IllegalArgumentException("Supplied VIP Address cannot be null")
        }

        return this.localRegionApps.get().getInstancesByVirtualHostName(vipAddress)
    }

    // 830
    override fun getNextServerFromEureka(virtualHostname: String): InstanceInfo {
        val instanceInfoList = this.getInstancesByVipAddress(virtualHostname)
        if (instanceInfoList == null || instanceInfoList.isEmpty()) {
            throw RuntimeException("No matchers for the virtual host name: $virtualHostname")
        }
        val apps = this.localRegionApps.get()
        val index = (apps.getNextIndex(virtualHostname)
            .incrementAndGet() % instanceInfoList.size).toInt()

        return instanceInfoList[index]
    }

    //851
    override fun getApplications(serviceUrl: String): Applications? {
        val response = (clientConfig.registryRefreshSingleVipAddress
            ?.let { eurekaTransport.queryClient.getVip(it) }
            ?: eurekaTransport.queryClient.getApplications())

        if (response.statusCode == Response.Status.OK.statusCode) {
            return response.entity
        }

        return null
    }

    /**
     * Register with the eureka service by making the appropriate REST call.
     * 872
     */
    fun register(): Boolean {
        return eurekaTransport.registrationClient.register(instanceInfo)
            .statusCode == HttpStatus.NO_CONTENT.value()
    }

    // 890
    private fun renew(): Boolean {
        val response = eurekaTransport.registrationClient.sendHeartBeat(
            instanceInfo.appName, instanceInfo.id, instanceInfo, null)

        if (response.statusCode == Response.Status.NOT_FOUND.statusCode) {
            val success = register()
            if (success) {
                instanceInfo.unsetIsDirty(instanceInfo.setIsDirtyWithTime())
            }
            return success
        }

        return response.statusCode == Response.Status.OK.statusCode
    }

    // 933
    @PreDestroy
    @Synchronized
    override fun shutdown() {
        cancelScheduledTasks()

        if (clientConfig.shouldRegisterWithEureka()) {
            applicationInfoManager.setInstanceStatus(InstanceStatus.DOWN)
            unregister()
        }

        eurekaTransport.shutdown()

        logger.info("Completed shut down of DiscoveryClient")
    }

    // 967
    private fun unregister() {
        eurekaTransport.registrationClient
            .cancel(instanceInfo.appName, instanceInfo.id)
    }

    // 992
    /**
     * Fetches the registry information
     *
     * <p>
     *     This method tries to get only deltas after the first fetch unless there
     *     is an issue in reconciling eureka server and client registry information.
     * </p>
     */
    private fun fetchRegistry(): Boolean {
        try {
            val applications = getApplications()

            if (clientConfig.shouldDisableDelta()
                || applications.registeredApplications.isEmpty()
            ) {
                getAndStoreFullRegistry()
            } else {
                getAndUpdateDelta()
            }
            applications.appsHashCode = applications.getReconcileHashCode()
        } catch (ex: Throwable) {
            return false
        }

        // Update remote status based on refreshed data held in the cache
        updateInstanceRemoteStatus()

        // registry was fetched successfully, so return true
        return true
    }

    // 1040
    private fun updateInstanceRemoteStatus() {
        // Determine this instance's status for this app and set to UNKNOWN if not found
        var currentRemoteInstanceStatus: InstanceStatus? = null
        if (instanceInfo.appName != null) {
            val app = getApplication(instanceInfo.appName)
            if (app != null) {
                val remoteInstanceInfo = app.getInstanceById(instanceInfo.id)
                if (remoteInstanceInfo != null) {
                    currentRemoteInstanceStatus = remoteInstanceInfo.status
                }
            }
        }
        if (currentRemoteInstanceStatus == null) {
            currentRemoteInstanceStatus = InstanceStatus.UNKNOWN
        }

        if (lastRemoteInstanceStatus != currentRemoteInstanceStatus) {
            lastRemoteInstanceStatus = currentRemoteInstanceStatus
        }
    }

    // 1094
    private fun getAndStoreFullRegistry() {
        logger.info("Getting all instance registry info from the eureka server")

        var apps: Applications? = null
        val httpResponse = eurekaTransport.queryClient.getApplications()
        if (httpResponse.statusCode == Response.Status.OK.statusCode) {
            apps = httpResponse.entity
        }

        if (apps != null) {
            localRegionApps.set(apps)
        }
    }

    private fun getAndUpdateDelta() {
        var delta: Applications? = null
        val httpResponse = eurekaTransport.queryClient.getDelta()
        if (httpResponse.statusCode == Response.Status.OK.statusCode) {
            delta = httpResponse.entity
        }

        if (delta == null) {
            getAndStoreFullRegistry()
        } else {
            updateDelta(delta)
        }
    }

    // 1237
    private fun updateDelta(delta: Applications) {
        var deltaCount = 0
        for (app in delta.registeredApplications) {
            for (instance in app.instances) {
                val applications = getApplications()

                deltaCount++
                if (instance.actionType == ActionType.ADDED
                    || instance.actionType == ActionType.MODIFIED
                ) {
                    val existingApp = applications.getRegisteredApplications(instance.appName)
                    if (existingApp == null) {
                        applications.addApplication(app)
                    }

                    applications.getRegisteredApplications(instance.appName)!!.addInstance(instance)
                } else if (instance.actionType == ActionType.DELETED) {
                    val existingApp = applications.getRegisteredApplications(instance.appName)
                    if (existingApp != null) {
                        existingApp.removeInstance(instance)

                        if (existingApp.getInstancesAsIsFromEureka().isEmpty()) {
                            applications.removeApplication(existingApp)
                        }
                    }
                }
            }
        }

        getApplications().shuffleInstances(clientConfig.shouldFilterOnlyUpInstances())
    }

    // 1299
    /**
     * Initializes all scheduled tasks
     */
    private fun initScheduledTasks() {
        if (clientConfig.shouldFetchRegistry()) {
            val registryFetchIntervalSeconds = clientConfig.registryFetchIntervalSeconds
            val expBackOfBound = clientConfig.cacheRefreshExecutorExponentialBackOffBound

            // registry cache refresh timer
            this.cacheRefreshTask = TimedSupervisorTask(
                "cacheRefresh",
                scheduler,
                cacheRefreshExecutor,
                registryFetchIntervalSeconds,
                TimeUnit.SECONDS,
                expBackOfBound
            ) {
                refreshRegistry()
            }

            scheduler!!.schedule(cacheRefreshTask, registryFetchIntervalSeconds.toLong(), TimeUnit.SECONDS)
        }

        if (clientConfig.shouldRegisterWithEureka()) {
            val renewalIntervalInSecs = instanceInfo.leaseInfo.renewalIntervalInSecs
            val expBackOffBound = clientConfig.heartbeatExecutorExponentialBackOffBound

            // Heartbeat timer
            this.heartbeatTask = TimedSupervisorTask(
                "heartbeat",
                scheduler,
                heartbeatExecutor,
                renewalIntervalInSecs,
                TimeUnit.SECONDS,
                expBackOffBound
            ) {
                if (renew()) {
                    this.lastSuccessfulHeartbeatTimestamp = System.currentTimeMillis()
                }
            }

            scheduler!!.schedule(heartbeatTask, renewalIntervalInSecs.toLong(), TimeUnit.SECONDS)

            // InstanceInfo replicator
            this.instanceInfoReplicator = InstanceInfoReplicator(
                this, instanceInfo, clientConfig.instanceInfoReplicationIntervalSeconds)

            instanceInfoReplicator.start(clientConfig.initialInstanceInfoReplicationIntervalSeconds)
        }
    }

    // 1367
    private fun cancelScheduledTasks() {
        instanceInfoReplicator.stop()
        cacheRefreshExecutor?.shutdownNow()
        heartbeatExecutor?.shutdownNow()
        scheduler?.shutdownNow()
        cacheRefreshTask.cancel()
        heartbeatTask.cancel()
    }

    // 1503
    private fun refreshRegistry() {
        val success = fetchRegistry()
        if (success) {
            lastSuccessfulRegistryFetchTimestamp = System.currentTimeMillis()
        }
    }
}
