package com.mlacker.samples.netflix.loadbalaance

import com.netflix.loadbalancer.LoadBalancerStats
import com.netflix.loadbalancer.Server

abstract class AbstractLoadBalancer : ILoadBalancer {

    enum class ServerGroup {
        ALL,
        STATUS_UP,
        STATUS_NOT_UP
    }

    fun chooseServer(): Server? = chooseServer(null)

    abstract val loadBalancerStats: LoadBalancerStats
}