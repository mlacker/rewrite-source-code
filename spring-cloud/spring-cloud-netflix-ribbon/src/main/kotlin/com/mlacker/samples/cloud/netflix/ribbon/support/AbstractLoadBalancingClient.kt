package com.mlacker.samples.cloud.netflix.ribbon.support

import com.mlacker.samples.cloud.client.loadbalancer.ServiceInstanceChooser
import com.mlacker.samples.netflix.client.AbstractLoadBalancerAwareClient
import com.mlacker.samples.netflix.loadbalancer.LoadBalancerCommand
import com.netflix.client.IResponse
import com.netflix.client.RequestSpecificRetryHandler
import com.netflix.client.RetryHandler
import com.netflix.client.config.IClientConfig
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient
import org.springframework.cloud.netflix.ribbon.RibbonProperties
import org.springframework.cloud.netflix.ribbon.support.ContextAwareRequest
import org.springframework.http.HttpMethod

abstract class AbstractLoadBalancingClient<S : ContextAwareRequest, T : IResponse, D>(
    protected val delegate: D,
    protected val config: IClientConfig,
) : AbstractLoadBalancerAwareClient<S, T>(null), ServiceInstanceChooser {

    protected var connectTimeout: Int = 0
    protected var readTimeout: Int = 0
    protected val okToRetryOnAllOperations: Boolean = false

    init {
        this.retryHandler = RetryHandler.DEFAULT
        this.initWithNiwsConfig(config)
    }

    override fun initWithNiwsConfig(clientConfig: IClientConfig) {
        super.initWithNiwsConfig(clientConfig)
        val ribbon = RibbonProperties.from(clientConfig)
        this.connectTimeout = ribbon.connectTimeout
        this.readTimeout = ribbon.readTimeout
        this.okToRetryOnAllOperations = ribbon.isOkToRetryOnAllOperations
    }

    protected abstract fun createDelegate(configOverride: IClientConfig?): D

    override fun getRequestSpecificRetryHandler(request: S, requestConfig: IClientConfig): RequestSpecificRetryHandler {
        val okToRetryOnAllErrors: Boolean = when {
            this.okToRetryOnAllOperations -> true
            request.method != HttpMethod.GET -> false
            else -> true
        }

        return RequestSpecificRetryHandler(true, okToRetryOnAllErrors, this.retryHandler, requestConfig)
    }

    override fun customizeLoadBalancerCommand(request: S, config: IClientConfig, command: LoadBalancerCommand<T>) {
        if (request.loadBalancerKey != null) {
            command.loadBalancerKey = request.loadBalancerKey
        }
    }

    override fun choose(serviceId: String): ServiceInstance? {
        val server = this.loadBalancer.chooseServer(serviceId)
        if (server != null) {
            return RibbonLoadBalancerClient.RibbonServer(serviceId, server)
        }
        return null
    }
}
