package com.mlacker.samples.netflix.discovery.endpoint

import com.netflix.discovery.EurekaClientConfig

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