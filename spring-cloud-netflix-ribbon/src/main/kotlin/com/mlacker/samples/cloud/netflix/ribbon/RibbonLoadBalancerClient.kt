package com.mlacker.samples.cloud.netflix.ribbon

import com.mlacker.samples.cloud.client.loadbalancer.LoadBalancerClient
import com.mlacker.samples.netflix.loadbalaance.ILoadBalancer
import com.netflix.loadbalancer.Server
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest
import org.springframework.cloud.netflix.ribbon.DefaultServerIntrospector
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient
import org.springframework.cloud.netflix.ribbon.RibbonStatsRecorder
import org.springframework.cloud.netflix.ribbon.ServerIntrospector
import org.springframework.cloud.netflix.ribbon.SpringClientFactory
import java.net.URI

class RibbonLoadBalancerClient(
    private val clientFactory: SpringClientFactory
) : LoadBalancerClient {

    override fun reconstructURI(instance: ServiceInstance, original: URI): URI {
        val context = this.clientFactory.getLoadBalancerContext(instance.serviceId)

        val server = if (instance is RibbonLoadBalancerClient.RibbonServer) {
            instance.server
        } else {
            Server(instance.scheme, instance.host, instance.port)
        }

        return context.reconstructURIWithServer(server, original)
    }

    override fun choose(serviceId: String): ServiceInstance? {
        val server = getLoadBalancer(serviceId)?.let { getServer(it) } ?: return null

        return RibbonLoadBalancerClient.RibbonServer(
            serviceId, server, false,
            serverIntrospector(serviceId).getMetadata(server)
        )
    }

    override fun <T> execute(serviceId: String, request: LoadBalancerRequest<T>): T {
        val ribbonServer = choose(serviceId) ?: throw IllegalStateException("No instance available for $serviceId")

        return execute(serviceId, ribbonServer, request)
    }

    override fun <T> execute(serviceId: String, serviceInstance: ServiceInstance, request: LoadBalancerRequest<T>): T {
        val server = if (serviceInstance is RibbonLoadBalancerClient.RibbonServer) {
            serviceInstance.server
        } else {
            throw IllegalStateException("No instance available for $serviceId")
        }

        val context = this.clientFactory.getLoadBalancerContext(serviceId)
        val statsRecorder = RibbonStatsRecorder(context, server)

        try {
            val returnVal = request.apply(serviceInstance)
            statsRecorder.recordStats(returnVal)
            return returnVal
        } catch (ex: Exception) {
            statsRecorder.recordStats(ex)
            throw ex
        }
    }

    private fun serverIntrospector(serviceId: String): ServerIntrospector {
        return this.clientFactory.getInstance(serviceId, ServerIntrospector::class.java)
            ?: DefaultServerIntrospector()
    }

    private fun getServer(loadBalancer: ILoadBalancer): Server? {
        return loadBalancer.chooseServer("default")
    }

    private fun getLoadBalancer(serviceId: String): ILoadBalancer? {
        // this.clientFactory.getLoadBalancer(serviceId)
        return this.clientFactory.getInstance(serviceId, ILoadBalancer::class.java)
    }
}