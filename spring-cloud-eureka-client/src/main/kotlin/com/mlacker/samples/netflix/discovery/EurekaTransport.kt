package com.mlacker.samples.netflix.discovery

import com.mlacker.samples.netflix.discovery.shared.transport.EurekaHttpClient

// DiscoveryClient.java:197
class EurekaTransport(
    val registrationClient: EurekaHttpClient,
    val queryClient: EurekaHttpClient,
) {
    fun shutdown() {
        registrationClient.shutdown()
        queryClient.shutdown()
    }
}