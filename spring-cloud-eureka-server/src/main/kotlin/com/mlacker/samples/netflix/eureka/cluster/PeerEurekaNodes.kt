package com.mlacker.samples.netflix.eureka.cluster

import com.mlacker.samples.netflix.appinfo.ApplicationInfoManager
import com.mlacker.samples.netflix.eureka.registry.PeerAwareInstanceRegistry
import com.netflix.discovery.EurekaClientConfig
import com.netflix.eureka.EurekaServerConfig
import com.netflix.eureka.resources.ServerCodecs
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

open class PeerEurekaNodes(
    protected val registry: PeerAwareInstanceRegistry,
    protected val serverConfig: EurekaServerConfig,
    protected val clientConfig: EurekaClientConfig,
    protected val serverCodecs: ServerCodecs,
    private val applicationInfoManager: ApplicationInfoManager,
) {
    var peerEurekaNodes: List<PeerEurekaNode> = emptyList()
    private var peerEurekaNodeUrls: Set<String> = emptySet()

    private lateinit var taskExecutor: ScheduledExecutorService

    fun start() {
        taskExecutor = Executors.newSingleThreadScheduledExecutor {
            Thread(it, "Eureka-PeerNodesUpdater").apply { isDaemon = true }
        }
        updatePeerEurekaNodes(resolvePeerUrls())
        val peersUpdateTask = Runnable {
            try {
                updatePeerEurekaNodes(resolvePeerUrls())
            } catch (e: Throwable) {
            }
        }
        taskExecutor.scheduleWithFixedDelay(
            peersUpdateTask,
            serverConfig.peerEurekaNodesUpdateIntervalMs.toLong(),
            serverConfig.peerEurekaNodesUpdateIntervalMs.toLong(),
            TimeUnit.MILLISECONDS
        )
    }

    fun shutdown() {
        taskExecutor.shutdown()
        val toRemove = this.peerEurekaNodes

        this.peerEurekaNodes = emptyList()
        this.peerEurekaNodeUrls = emptySet()

        toRemove.forEach { it.shutdown() }
    }

    protected open fun resolvePeerUrls(): List<String> {
        TODO("Not yet implemented")
    }

    protected open fun updatePeerEurekaNodes(newPeerUrls: List<String>) {
        TODO("Not yet implemented")
    }

    fun isThisMyUrl(url: String): Boolean {
        TODO("Not yet implemented")
    }
}