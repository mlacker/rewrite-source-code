package com.mlacker.samples.cloud.client.discovery

import com.mlacker.samples.cloud.client.ServiceInstance

interface DiscoveryClient {

    val description: String

    fun getInstances(serviceId: String): List<ServiceInstance>

    fun getServices(): List<String>

    val order: Int
        get() = DEFAULT_ORDER

    companion object {
        const val DEFAULT_ORDER: Int = 0
    }
}