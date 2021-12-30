package com.mlacker.samples.netflix.loadbalancer

import com.netflix.loadbalancer.Server

interface IRule {

    var loadBalancer: ILoadBalancer

    fun choose(key: Any?): Server?
}