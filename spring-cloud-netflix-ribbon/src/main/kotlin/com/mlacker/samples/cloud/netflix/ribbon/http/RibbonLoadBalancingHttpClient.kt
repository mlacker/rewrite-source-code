package com.mlacker.samples.cloud.netflix.ribbon.http

import com.mlacker.samples.cloud.netflix.ribbon.support.AbstractLoadBalancingClient
import com.netflix.client.config.IClientConfig
import org.slf4j.LoggerFactory
import org.springframework.cloud.netflix.ribbon.RibbonProperties
import java.net.http.HttpClient
import java.net.http.HttpResponse
import java.time.Duration

class RibbonLoadBalancingHttpClient(delegate: HttpClient, config: IClientConfig) :
    AbstractLoadBalancingClient<HttpRibbonRequest, HttpRibbonResponse, HttpClient>(delegate, config) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun createDelegate(configOverride: IClientConfig?): HttpClient {
        val config = configOverride ?: this.config
        val ribbon = RibbonProperties.from(config)

        val connectTimeout = Duration.ofMillis(ribbon.connectTimeout(this.connectTimeout).toLong())

        return HttpClient.newBuilder()
            .connectTimeout(connectTimeout)
            .build()
    }

    override fun execute(ribbonRequest: HttpRibbonRequest, configOverride: IClientConfig?): HttpRibbonResponse {
        val config = configOverride ?: this.config
        val ribbon = RibbonProperties.from(config)

        if (logger.isDebugEnabled) {
            logger.debug("HttpClient sending new Request: ${ribbonRequest.getURI()}")
        }

        val httpRequest = ribbonRequest.toRequest(ribbon, this.readTimeout)
        val httpResponse = delegate.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream())

        return HttpRibbonResponse(httpResponse, ribbonRequest.getURI())
    }
}

