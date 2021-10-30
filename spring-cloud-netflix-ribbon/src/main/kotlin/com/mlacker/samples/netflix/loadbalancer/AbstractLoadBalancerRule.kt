package com.mlacker.samples.netflix.loadbalancer

abstract class AbstractLoadBalancerRule : IRule {

    override lateinit var loadBalancer: ILoadBalancer
}