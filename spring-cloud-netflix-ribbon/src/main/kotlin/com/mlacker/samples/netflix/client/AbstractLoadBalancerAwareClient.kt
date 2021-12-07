package com.mlacker.samples.netflix.client

import com.mlacker.samples.netflix.loadbalancer.ServerOperation
import com.mlacker.samples.netflix.loadbalancer.LoadBalancerCommand
import com.netflix.client.ClientException
import com.netflix.client.ClientRequest
import com.netflix.client.IClient
import com.netflix.client.IClientConfigAware
import com.netflix.client.IResponse
import com.netflix.client.RequestSpecificRetryHandler
import com.netflix.client.config.IClientConfig
import com.netflix.loadbalancer.ILoadBalancer
import com.netflix.loadbalancer.LoadBalancerContext
import com.netflix.loadbalancer.Server

abstract class AbstractLoadBalancerAwareClient<S :  ClientRequest, T : IResponse>(lb: ILoadBalancer?) :
    LoadBalancerContext(lb), IClient<S, T>, IClientConfigAware {

    fun executeWithLoadBalancer(request: S, requestConfig: IClientConfig): T {
        val command = buildLoadBalancerCommand(request, requestConfig)

        try {
            return command.submit(object : ServerOperation<T> {
                @Suppress("UNCHECKED_CAST")
                override fun invoke(server: Server): T {
                    val finalUri = reconstructURIWithServer(server, request.uri)
                    val requestForServer = request.replaceUri(finalUri) as S
                    return this@AbstractLoadBalancerAwareClient
                        .execute(requestForServer, requestConfig)
                }
            })
        } catch (ex: Exception) {
            throw if (ex.cause is ClientException) {
                ex.cause as ClientException
            } else {
                ClientException(ex)
            }
        }
    }

    abstract fun getRequestSpecificRetryHandler(request: S, requestConfig: IClientConfig): RequestSpecificRetryHandler

    private fun buildLoadBalancerCommand(request: S, config: IClientConfig): LoadBalancerCommand<T> {
        val handler = getRequestSpecificRetryHandler(request, config)
        val command = LoadBalancerCommand<T>(this, request.uri, handler)
        customizeLoadBalancerCommand(request, config, command)
        return command
    }

    protected open fun customizeLoadBalancerCommand(request: S, config: IClientConfig, command: LoadBalancerCommand<T>) {
        // do nothing by default, give a chance to its derived class to customize the builder
    }
}