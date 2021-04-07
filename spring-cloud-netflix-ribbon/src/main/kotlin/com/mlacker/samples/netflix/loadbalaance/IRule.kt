package com.mlacker.samples.netflix.loadbalaance

import com.netflix.loadbalancer.Server

interface IRule {

    var loadBalancer: ILoadBalancer

    fun choose(key: Any?): Server?
}