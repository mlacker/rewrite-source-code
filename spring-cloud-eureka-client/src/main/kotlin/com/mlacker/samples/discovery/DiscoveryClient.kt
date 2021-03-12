package com.mlacker.samples.discovery

import com.netflix.appinfo.ApplicationInfoManager
import com.netflix.appinfo.InstanceInfo
import com.netflix.discovery.EurekaClientConfig
import com.netflix.discovery.TimedSupervisorTask
import com.netflix.discovery.shared.Applications
import com.netflix.discovery.shared.transport.EurekaHttpClient
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.annotation.PreDestroy
import javax.inject.Singleton


@Singleton
class DiscoveryClient {

    private val scheduler: ScheduledExecutorService
    private val cacheRefreshExecutor: ThreadPoolExecutor
    private val heartbeatExecutor: ThreadPoolExecutor
    private lateinit var cacheRefreshTask: TimedSupervisorTask
    private lateinit var heartbeatTask: TimedSupervisorTask

    private val localRegionApps: Applications
    private val applicationInfoManager: ApplicationInfoManager
    private val instanceInfo: InstanceInfo

    private val eurekaTransport: EurekaTransport

    private lateinit var instanceInfoReplicator: InstanceInfoReplicator
    private var lastSuccessfulRegistryFetchTimestamp = -1L
    private var lastSuccessfulHeartbeatTimestamp = -1L

    private val clientConfig: EurekaClientConfig

    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        eurekaTransport = EurekaTransport()
    }

    // 197
    private class EurekaTransport {
        val registrationClient: EurekaHttpClient
        val queryClient: EurekaHttpClient

        fun shutdown() {
            registrationClient.shutdown()
            queryClient.shutdown()
        }
    }

    /**
     * Initializes all scheduled tasks
     * 1299
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

            scheduler.schedule(cacheRefreshTask, registryFetchIntervalSeconds.toLong(), TimeUnit.SECONDS)
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

            scheduler.schedule(heartbeatTask, renewalIntervalInSecs.toLong(), TimeUnit.SECONDS)

            // InstanceInfo replicator
            this.instanceInfoReplicator = InstanceInfoReplicator(
                    this, instanceInfo, clientConfig.instanceInfoReplicationIntervalSeconds)

            instanceInfoReplicator.start(clientConfig.initialInstanceInfoReplicationIntervalSeconds)
        }
    }

    // 890
    private fun renew(): Boolean {
        TODO("Not yet implemented")
    }

    // 933
    @PreDestroy
    private fun shutdown() {
        cancelScheduledTasks()

        if (clientConfig.shouldRegisterWithEureka()) {
            applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.DOWN)
            unregister()
        }

        eurekaTransport.shutdown()
    }

    // 967
    private fun unregister() {
        eurekaTransport.registrationClient.cancel(instanceInfo.appName, instanceInfo.id)
    }

    // 1367
    private fun cancelScheduledTasks() {
        instanceInfoReplicator.stop()
        cacheRefreshExecutor.shutdownNow()
        heartbeatExecutor.shutdownNow()
        scheduler.shutdownNow()
        cacheRefreshTask.cancel()
        heartbeatTask.cancel()
    }

    // 1503
    private fun refreshRegistry() {
        TODO("Not yet implemented")
    }

    /**
     * Register with the eureka service by making the appropriate REST call.
     */
    fun register(): Boolean {
        return eurekaTransport.registrationClient.register(instanceInfo)
                .statusCode == HttpStatus.NO_CONTENT.value()
    }
}

class EndpointUtils {
    companion object {

        private const val DEFAULT_ZONE = "default"

        /**
         * Get the list of all eureka service urls from properties file for the eureka client to talk to.
         */
        fun getServiceUrlsFromConfig(clientConfig: EurekaClientConfig, instanceZone: String?, preferSameZone: Boolean): List<String> {
            val orderedUrls = mutableListOf<String>()
            var availZones = clientConfig.getAvailabilityZones(clientConfig.region)
            if (availZones == null || availZones.isEmpty()) {
                availZones = arrayOf(DEFAULT_ZONE)
            }

            val myZoneOffset = availZones
                    .indexOfFirst { instanceZone != null && (instanceZone == it) == preferSameZone }
                    .let { if (it > 0) it else 0 }

            clientConfig.getEurekaServerServiceUrls(availZones[myZoneOffset])?.let {
                orderedUrls.addAll(it)
            }

            var currentOffset = if (myZoneOffset != availZones.size - 1) myZoneOffset + 1 else 0
            while (currentOffset != myZoneOffset) {
                clientConfig.getEurekaServerServiceUrls(availZones[currentOffset])?.let {
                    orderedUrls.addAll(it)
                }
                currentOffset = if (myZoneOffset != availZones.size - 1) currentOffset + 1 else 0
            }

            if (orderedUrls.isEmpty()) {
                throw IllegalArgumentException()
            }

            return orderedUrls
        }
    }
}
