package com.mlacker.samples.cloud.client.loadbalancer

import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest
import java.net.URI

interface LoadBalancerClient : ServiceInstanceChooser {

    fun <T> execute(serviceId: String, request: LoadBalancerRequest<T>): T

    fun <T> execute(serviceId: String, serviceInstance: ServiceInstance, request: LoadBalancerRequest<T>): T

    fun reconstructURI(instance: ServiceInstance, original: URI): URI
}