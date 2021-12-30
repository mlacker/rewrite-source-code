package com.mlacker.samples.cloud.netflix.zuul.filters.route.http

import com.mlacker.samples.cloud.netflix.ribbon.http.HttpRibbonRequest
import com.mlacker.samples.cloud.netflix.ribbon.http.HttpRibbonResponse
import com.mlacker.samples.cloud.netflix.ribbon.http.RibbonLoadBalancingHttpClient
import com.mlacker.samples.cloud.netflix.zuul.filters.route.support.AbstractRibbonCommand
import com.netflix.client.config.IClientConfig
import org.springframework.cloud.netflix.ribbon.support.RibbonCommandContext
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties
import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider

class HttpClientRibbonCommand(
    commandKey: String,
    client: RibbonLoadBalancingHttpClient,
    context: RibbonCommandContext,
    properties: ZuulProperties,
    fallbackProvider: FallbackProvider?,
    config: IClientConfig,
) : AbstractRibbonCommand<RibbonLoadBalancingHttpClient, HttpRibbonRequest, HttpRibbonResponse>(
    commandKey, client, context, properties, fallbackProvider, config
) {

    override fun createRequest(): HttpRibbonRequest {
        return HttpRibbonRequest(context)
    }
}
