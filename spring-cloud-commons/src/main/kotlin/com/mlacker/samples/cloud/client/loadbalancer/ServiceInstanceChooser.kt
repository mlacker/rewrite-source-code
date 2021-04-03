package com.mlacker.samples.cloud.client.loadbalancer

import org.springframework.cloud.client.ServiceInstance

interface ServiceInstanceChooser {

    fun choose(serviceId: String): ServiceInstance?
}
