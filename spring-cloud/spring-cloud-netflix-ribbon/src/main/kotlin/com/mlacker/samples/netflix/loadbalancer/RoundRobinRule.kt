package com.mlacker.samples.netflix.loadbalancer

import com.netflix.loadbalancer.Server
import java.util.concurrent.atomic.AtomicInteger

open class RoundRobinRule : AbstractLoadBalancerRule(), IRule {

    private val nextServerCyclicCounter: AtomicInteger = AtomicInteger(0)

    override fun choose(key: Any?): Server? {
        return choose(loadBalancer, key)
    }

    protected open fun choose(lb: ILoadBalancer, key: Any?): Server? {
        var count = 0
        var server: Server? = null
        while (server == null && count++ < 10) {
            val reachableServers = lb.getReachableServers()
            val allServers = lb.getAllServers()

            if (reachableServers.isEmpty() || allServers.isEmpty()) {
                return null
            }

            val nextServerIndex = incrementAndGetModulo(allServers.size)
            server = allServers[nextServerIndex]

            if (server.isAlive && server.isReadyToServe) {
                return server
            }

            server = null
        }

        return server
    }

    private fun incrementAndGetModulo(modulo: Int): Int {
        while (true) {
            val current = nextServerCyclicCounter.get()
            val next = (current + 1) % modulo
            if (nextServerCyclicCounter.compareAndSet(current, next))
                return next
        }
    }
}
