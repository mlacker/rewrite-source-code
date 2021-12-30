package com.mlacker.samples.cloud.loadbalancer.blocking.client

import com.mlacker.samples.cloud.client.loadbalancer.LoadBalancerClient
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory
import reactor.core.publisher.Mono
import java.net.URI

class BlockingLoadBalancerClient(
    private val loadBalancerClientFactory: LoadBalancerClientFactory
) : LoadBalancerClient {

    override fun <T> execute(serviceId: String, request: LoadBalancerRequest<T>): T {
        val serviceInstance = choose(serviceId)
            ?: throw IllegalStateException("No instance available for $serviceId")

        return execute(serviceId, serviceInstance, request)
    }

    override fun <T> execute(serviceId: String, serviceInstance: ServiceInstance, request: LoadBalancerRequest<T>): T {
        return request.apply(serviceInstance)
    }

    override fun reconstructURI(instance: ServiceInstance, original: URI): URI {
        return LoadBalancerUriTools.reconstructURI(instance, original)
    }

    override fun choose(serviceId: String): ServiceInstance? {
        val loadBalancer = loadBalancerClientFactory.getInstance(serviceId) ?: return null

        val loadBalancerResponse = Mono.from(loadBalancer.choose()).block() ?: return null

        return loadBalancerResponse.server
    }
}