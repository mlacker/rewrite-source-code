package com.mlacker.samples.netflix.eureka.cluster

import com.mlacker.samples.netflix.appinfo.InstanceInfo
import com.mlacker.samples.netflix.appinfo.InstanceStatus
import com.mlacker.samples.netflix.eureka.registry.PeerAwareInstanceRegistryImpl
import com.netflix.discovery.shared.transport.EurekaHttpResponse
import com.netflix.eureka.EurekaServerConfig
import com.netflix.eureka.util.batcher.TaskDispatcher
import com.netflix.eureka.util.batcher.TaskDispatchers
import java.net.URI

class PeerEurekaNode(
    private val targetHost: String,
    val serviceUrl: String,
    private val replicationClient: HttpReplicationClient,
    config: EurekaServerConfig,
    batchSize: Int = 250,
    maxBatchingDelayMs: Int = 500,
    retrySleepTimeMs: Long = 100,
    serverUnavailableSleepTimeMs: Long = 1000,
) {
    private val maxProcessingDelayMs: Long = config.maxTimeForReplication.toLong()
    private val batchingDispatcher: TaskDispatcher<String, ReplicationTask>

    init {
        val batcherName = "target_${URI(serviceUrl).host}"
        val taskProcessor = ReplicationTaskProcessor()
        this.batchingDispatcher = TaskDispatchers.createBatchingTaskDispatcher(
            batcherName,
            config.maxElementsInPeerReplicationPool,
            batchSize,
            config.maxThreadsForPeerReplication,
            maxBatchingDelayMs.toLong(),
            serverUnavailableSleepTimeMs,
            retrySleepTimeMs,
            taskProcessor
        )
    }

    fun register(info: InstanceInfo) {
        val expiryTime = System.currentTimeMillis() + getLeaseRenewalOf(info)
        batchingDispatcher.process(
            taskId("register", info),
            object : InstanceReplicationTask(targetHost, PeerAwareInstanceRegistryImpl.Action.Register,
                info, null, true) {
                override fun execute(): EurekaHttpResponse<*> {
                    return replicationClient.register(info)
                }
            },
            expiryTime
        )
    }

    fun cancel(appName: String, id: String) {
        val expiryTime = System.currentTimeMillis() - maxProcessingDelayMs
        batchingDispatcher.process(
            taskId("cancel", appName, id),
            object : InstanceReplicationTask(targetHost, PeerAwareInstanceRegistryImpl.Action.Cancel,
                appName, id) {
                override fun execute(): EurekaHttpResponse<*> {
                    return replicationClient.cancel(appName, id)
                }
            },
            expiryTime
        )
    }

    fun heartbeat(appName: String, id: String, info: InstanceInfo, overriddenStatus: InstanceStatus?, primeConnection: Boolean) {
        if (primeConnection) {
            replicationClient.sendHeartBeat(appName, id, info, overriddenStatus)
            return
        }

        val expiryTime = System.currentTimeMillis() - getLeaseRenewalOf(info)
        batchingDispatcher.process(
            taskId("heartbeat", info),
            object : InstanceReplicationTask(targetHost, PeerAwareInstanceRegistryImpl.Action.Heartbeat,
                info, overriddenStatus, false) {
                override fun execute(): EurekaHttpResponse<*> {
                    return replicationClient.sendHeartBeat(appName, id, info, overriddenStatus)
                }

                override fun handleFailure(statusCode: Int, responseEntity: Any) {
                    if (statusCode == 404) {
                        register(info)
                    }
                }
            },
            expiryTime
        )
    }

    fun statusUpdate(appName: String, id: String, newStatus: InstanceStatus, info: InstanceInfo) {
        val expiryTime = System.currentTimeMillis() + maxProcessingDelayMs
        batchingDispatcher.process(
            taskId("statusUpdate", appName, id),
            object : InstanceReplicationTask(targetHost, PeerAwareInstanceRegistryImpl.Action.StatusUpdate,
                info, null, false) {
                override fun execute(): EurekaHttpResponse<*> {
                    return replicationClient.statusUpdate(appName, id, newStatus, info)
                }
            },
            expiryTime
        )
    }

    fun deleteStatusOverride(appName: String, id: String, info: InstanceInfo) {
        val expiryTime = System.currentTimeMillis() + maxProcessingDelayMs
        batchingDispatcher.process(
            taskId("deleteStatusOverride", appName, id),
            object : InstanceReplicationTask(targetHost, PeerAwareInstanceRegistryImpl.Action.DeleteStatusOverride,
                info, null, false) {
                override fun execute(): EurekaHttpResponse<Void> {
                    return replicationClient.deleteStatusOverride(appName, id, info)
                }
            },
            expiryTime)
    }

    fun shutdown() {
        batchingDispatcher.shutdown()
        replicationClient.shutdown()
    }

    private fun taskId(requestType: String, appName: String, id: String) =
        "$requestType#$appName/$id"

    private fun taskId(requestType: String, info: InstanceInfo) =
        taskId(requestType, info.appName, info.id)

    private fun getLeaseRenewalOf(info: InstanceInfo): Int {
        return info.leaseInfo.renewalIntervalInSecs * 1000
    }
}

