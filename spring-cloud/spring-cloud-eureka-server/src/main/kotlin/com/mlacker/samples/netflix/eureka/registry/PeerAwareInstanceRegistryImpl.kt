package com.mlacker.samples.netflix.eureka.registry

import com.mlacker.samples.netflix.appinfo.ApplicationInfoManager
import com.mlacker.samples.netflix.appinfo.InstanceInfo
import com.mlacker.samples.netflix.appinfo.InstanceStatus
import com.mlacker.samples.netflix.discovery.EurekaClient
import com.mlacker.samples.netflix.discovery.shared.Application
import com.mlacker.samples.netflix.eureka.cluster.PeerEurekaNode
import com.mlacker.samples.netflix.eureka.cluster.PeerEurekaNodes
import com.netflix.discovery.EurekaClientConfig
import com.netflix.eureka.EurekaServerConfig
import com.netflix.eureka.lease.Lease
import com.netflix.eureka.resources.ServerCodecs
import com.netflix.eureka.util.MeasuredRate
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

open class PeerAwareInstanceRegistryImpl(
    serverConfig: EurekaServerConfig,
    clientConfig: EurekaClientConfig,
    serverCodecs: ServerCodecs,
    protected val eurekaClient: EurekaClient,
) : AbstractInstanceRegistry(serverConfig, clientConfig, serverCodecs), PeerAwareInstanceRegistry {

    private var startupTime: Long = 0
    private var peerInstancesTransferEmptyOnStartup = true

    enum class Action {
        Heartbeat, Register, Cancel, StatusUpdate, DeleteStatusOverride
    }

    private val numberOfReplicationsLastMin: MeasuredRate = MeasuredRate(TimeUnit.MINUTES.toMillis(1))

    protected lateinit var peerEurekaNodes: PeerEurekaNodes

    private val timer: Timer = Timer("ReplicaAwareInstanceRegistry - RenewalThresholdUpdater", true)

    // 151
    override fun init(peerEurekaNodes: PeerEurekaNodes) {
        this.numberOfReplicationsLastMin.start()
        this.peerEurekaNodes = peerEurekaNodes
        scheduleRenewalThresholdUpdateTask()
        initRemoteRegionRegistry()
    }

    // 169
    override fun shutdown() {
        peerEurekaNodes.shutdown()
        numberOfReplicationsLastMin.stop()
        timer.cancel()

        super.shutdown()
    }

    // 193
    private fun scheduleRenewalThresholdUpdateTask() {
        timer.schedule(object : TimerTask() {
            override fun run() {
                updateRenewalThreshold()
            }
        }, serverConfig.renewalThresholdUpdateIntervalMs.toLong(),
            serverConfig.renewalThresholdUpdateIntervalMs.toLong())
    }

    // 209
    // Populates the registry information from a peer eureka node. This operation fails over to other nodes
    // until the list is exhausted if the communication fails.
    override fun syncUp(): Int {
        var count = 0

        for (i in 0 until serverConfig.registrySyncRetries) {
            if (count != 0) {
                break
            }

            if (i > 0) {
                try {
                    Thread.sleep(serverConfig.registrySyncRetryWaitMs)
                } catch (ex: InterruptedException) {
                    break
                }
            }

            for (app in eurekaClient.getApplications().registeredApplications) {
                for (instance in app.instances) {
                    try {
                        register(instance, instance.leaseInfo.durationInSecs, true)
                        count++
                    } catch (t: Throwable) {
                    }
                }
            }
        }

        return count
    }

    // 240
    override fun openForTraffic(applicationInfoManager: ApplicationInfoManager, count: Int) {
        this.expectedNumberOfClientsSendingRenews = count
        updateRenewsPerMinThreshold()
        this.startupTime = System.currentTimeMillis()
        if (count > 0) {
            this.peerInstancesTransferEmptyOnStartup = false
        }
        applicationInfoManager.setInstanceStatus(InstanceStatus.UP)
        super.postInit()
    }

    // 338
    override fun shouldAllowAccess(): Boolean {
        if (this.peerInstancesTransferEmptyOnStartup) {
            if (!(System.currentTimeMillis() > this.startupTime + serverConfig.waitTimeInMsWhenSyncEmpty)) {
                return false
            }
        }
        return true
    }

    // 384
    override fun cancel(appName: String, id: String, isReplication: Boolean): Boolean {
        if (super.cancel(appName, id, isReplication)) {
            replicateToPeers(Action.Cancel, appName, id, null, null, isReplication)
            return true
        }
        return false
    }

    // 406
    override fun register(info: InstanceInfo, isReplication: Boolean) {
        var leaseDuration = Lease.DEFAULT_DURATION_IN_SECS
        if (info.leaseInfo.durationInSecs > 0) {
            leaseDuration = info.leaseInfo.durationInSecs
        }
        super.register(info, leaseDuration, isReplication)
        replicateToPeers(Action.Register, info.appName, info.id, null, null, isReplication)
    }

    // 421
    override fun renew(appName: String, id: String, isReplication: Boolean): Boolean {
        if (super.renew(appName, id, isReplication)) {
            replicateToPeers(Action.Heartbeat, appName, id, null, null, isReplication)
            return true
        }
        return false
    }

    // 437
    override fun statusUpdate(appName: String, id: String, newStatus: InstanceStatus, lastDirtyTimestamp: String?, isReplication: Boolean): Boolean {
        if (super.statusUpdate(appName, id, newStatus, lastDirtyTimestamp, isReplication)) {
            replicateToPeers(Action.StatusUpdate, appName, id, null, newStatus, isReplication)
            return true
        }
        return false
    }

    // 448
    override fun deleteStatusOverride(appName: String, id: String, newStatus: InstanceStatus, lastDirtyTimestamp: String?, isReplication: Boolean): Boolean {
        if (super.deleteStatusOverride(appName, id, newStatus, lastDirtyTimestamp, isReplication)) {
            replicateToPeers(Action.DeleteStatusOverride, appName, id, null, null, isReplication)
            return true
        }
        return false
    }

    // 481
    override fun isLeaseExpirationEnabled(): Boolean {
        if (!isSelfPreservationModeEnabled()) {
            return true
        }
        return numberOfRenewsPerMinThreshold > 0 && getNumOfRenewsInLastMin() > numberOfRenewsPerMinThreshold
    }

    // 511
    override fun isSelfPreservationModeEnabled(): Boolean {
        return serverConfig.shouldEnableSelfPreservation()
    }

    // 521
    override fun getNextServerFromEureka(virtualHostname: String): InstanceInfo? {
        // TODO Auto-generated method stub
        return null
    }

    // 532
    private fun updateRenewalThreshold() {
        try {
            val apps = eurekaClient.getApplications()
            var count = 0
            for (app in apps.registeredApplications) {
                for (instance in app.instances) {
                    ++count
                }
            }
            synchronized(lock) {
                // Update threshold only if the threshold is greater than the
                // current expected threshold or if self preservation is disabled.
                if ((count) > (serverConfig.renewalPercentThreshold * expectedNumberOfClientsSendingRenews)
                    || (!this.isSelfPreservationModeEnabled())
                ) {
                    this.expectedNumberOfClientsSendingRenews = count
                    updateRenewsPerMinThreshold()
                }
            }
        } catch (e: Throwable) {
        }
    }

    // 565
    override fun getSortedApplications(): List<Application> {
        val apps = ArrayList(getApplications().registeredApplications)
        apps.sortBy { it.name }
        return apps
    }

    // 579
    fun getNumOfReplicationsInLastMin(): Long {
        return numberOfReplicationsLastMin.count
    }

    // 591
    override fun isBelowRenewThreshold(): Int {
        return if ((getNumOfRenewsInLastMin() <= numberOfRenewsPerMinThreshold)
            && ((this.startupTime > 0) && System.currentTimeMillis() > this.startupTime + serverConfig.waitTimeInMsWhenSyncEmpty)
        ) 1 else 0
    }

    // 629
    private fun replicateToPeers(action: Action, appName: String, id: String, info: InstanceInfo?, newStatus: InstanceStatus?, isReplication: Boolean) {
        if (isReplication) {
            numberOfReplicationsLastMin.increment()
        }
        if (peerEurekaNodes == Collections.EMPTY_LIST || isReplication) {
            return
        }

        for (node in peerEurekaNodes.peerEurekaNodes) {
            if (peerEurekaNodes.isThisMyUrl(node.serviceUrl)) {
                continue
            }
            replicateInstanceActionsToPeers(action, appName, id, info, newStatus, node)
        }
    }

    private fun replicateInstanceActionsToPeers(
        action: Action, appName: String, id: String, info: InstanceInfo?, newStatus: InstanceStatus?, node: PeerEurekaNode,
    ) {
        try {
            when (action) {
                Action.Cancel -> node.cancel(appName, id)
                Action.Heartbeat -> {
                    val overriddenStatus = overriddenInstanceStatusMap[id]
                    node.heartbeat(appName, id, getInstanceByAppAndId(appName, id)!!, overriddenStatus, false)
                }
                Action.Register -> node.register(info!!)
                Action.StatusUpdate -> node.statusUpdate(appName, id, newStatus!!, getInstanceByAppAndId(appName, id)!!)
                Action.DeleteStatusOverride -> node.deleteStatusOverride(appName, id, getInstanceByAppAndId(appName, id)!!)
            }
        } catch (t: Throwable) {
        }
    }
}
