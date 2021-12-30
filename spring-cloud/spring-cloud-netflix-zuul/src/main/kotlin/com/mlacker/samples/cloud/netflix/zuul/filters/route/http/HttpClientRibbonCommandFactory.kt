package com.mlacker.samples.cloud.netflix.zuul.filters.route.http

import com.mlacker.samples.cloud.netflix.ribbon.http.RibbonLoadBalancingHttpClient
import com.mlacker.samples.cloud.netflix.zuul.filters.route.support.AbstractRibbonCommandFactory
import org.springframework.cloud.netflix.ribbon.SpringClientFactory
import org.springframework.cloud.netflix.ribbon.support.RibbonCommandContext
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties
import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider

class HttpClientRibbonCommandFactory(
    private val clientFactory: SpringClientFactory,
    private val properties: ZuulProperties,
    zuulFallbackProviders: Set<FallbackProvider>,
) : AbstractRibbonCommandFactory<HttpClientRibbonCommand>(zuulFallbackProviders) {

    override fun create(context: RibbonCommandContext): HttpClientRibbonCommand {
        val serviceId = context.serviceId
        val fallbackProvider = getFallbackProvider(serviceId)
        val client = this.clientFactory.getClient(
            serviceId, RibbonLoadBalancingHttpClient::class.java)
        client.loadBalancer = this.clientFactory.getLoadBalancer(serviceId)
        return HttpClientRibbonCommand(serviceId, client, context, this.properties,
            fallbackProvider, clientFactory.getClientConfig(serviceId))
    }
}