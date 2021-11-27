package com.mlacker.samples.netflix.discovery

import com.mlacker.samples.netflix.appinfo.InstanceInfo
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * A task for updating and replicating the local instanceInfo to the remote server.
 */
class InstanceInfoReplicator(
    private val discoveryClient: DiscoveryClient,
    private val instanceInfo: InstanceInfo,
    private val replicationIntervalSeconds: Int
) : Runnable {

    private val scheduler = Executors.newScheduledThreadPool(1)

    fun start(initialDelayMs: Int) {
        scheduler.schedule(this, initialDelayMs.toLong(), TimeUnit.SECONDS)
    }

    fun stop() {
        instanceInfo.setIsDirty()
        scheduler.shutdown()
    }

    override fun run() {
        try {
            instanceInfo.isDirtyWithTime?.let {
                discoveryClient.register()
                instanceInfo.unsetIsDirty(it)
            }
        } finally {
            scheduler.schedule(this, replicationIntervalSeconds.toLong(), TimeUnit.SECONDS)
        }
    }
}