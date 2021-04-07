package com.mlacker.samples.netflix.loadbalaance

abstract class AbstractLoadBalancerRule : IRule {

    override lateinit var loadBalancer: ILoadBalancer
}