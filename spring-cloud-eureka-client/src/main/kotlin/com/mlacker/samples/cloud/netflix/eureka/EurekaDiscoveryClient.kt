package com.mlacker.samples.cloud.netflix.eureka

import com.mlacker.samples.cloud.client.ServiceInstance
import com.mlacker.samples.cloud.client.discovery.DiscoveryClient
import com.mlacker.samples.netflix.appinfo.InstanceInfo
import com.mlacker.samples.netflix.discovery.EurekaClient


class EurekaDiscoveryClient(
    private val eurekaClient: EurekaClient
) : DiscoveryClient {

    override val description: String = "Spring Cloud Eureka Discovery Client"

    override fun getInstances(serviceId: String): List<ServiceInstance> {
        val infos = this.eurekaClient.getInstancesByVipAddress(serviceId)

        return infos.map<InstanceInfo, ServiceInstance> { EurekaServiceInstance(it) }
    }

    override fun getServices(): List<String> {
        return this.eurekaClient
            .getApplications().registeredApplications
            .filter { it.instances.isNotEmpty() }
            .map { it.name.toLowerCase() }
    }
}