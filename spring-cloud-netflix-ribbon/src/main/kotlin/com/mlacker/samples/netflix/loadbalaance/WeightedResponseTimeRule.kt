package com.mlacker.samples.netflix.loadbalaance

import com.netflix.loadbalancer.Server
import java.util.*

class WeightedResponseTimeRule(override var loadBalancer: ILoadBalancer) : RoundRobinRule() {

    private var accumulatedWeights: List<Double> = listOf()
    private val serverWeightTaskTimerInterval: Long = 30 * 1000
    private lateinit var serverWightTimer: Timer
    private val random = Random()

    private fun initialize() {
        serverWightTimer = Timer("NFLoadBalancer-serverWeightTimer", true)
        serverWightTimer.schedule(object : TimerTask() {
            override fun run() {
                ServerWeight().maintainWeights()
            }
        }, 0, serverWeightTaskTimerInterval)

        ServerWeight().maintainWeights()
    }

    fun shutdown() {
        serverWightTimer.cancel()
    }

    override fun choose(lb: ILoadBalancer, key: Any?): Server? {
        var server: Server? = null

        while (server == null) {
            val currentWeights = accumulatedWeights
            if (Thread.interrupted()) {
                return null
            }

            val allServers = lb.getAllServers()
            val serverCount = allServers.size

            if (serverCount == 0) {
                return null
            }

            var serverIndex = 0
            val maxTotalWeight = if (currentWeights.isEmpty()) 0.0 else currentWeights.last()

            if (maxTotalWeight < 0.001 || serverCount != currentWeights.size) {
                server = super.choose(lb, key)
                if (server == null) {
                    return server
                }
            } else {
                val randomWeight = random.nextDouble() * maxTotalWeight
                var n = 0
                for (d in currentWeights) {
                    if (d >= randomWeight) {
                        serverIndex = n
                        break
                    } else {
                        n++
                    }
                }

                server = allServers[serverIndex]
            }

            if (server.isAlive) {
                return server
            }

            server = null
        }

        return server
    }

    inner class ServerWeight {
        fun maintainWeights() {
            val stats = (loadBalancer as AbstractLoadBalancer).loadBalancerStats
            var totalResponseTime = 0.0
            for (server in loadBalancer.getAllServers()) {
                val ss = stats.getSingleServerStat(server)
                totalResponseTime += ss.responseTimeAvg
            }

            var weightSoFar = 0.0
            val finalWeights = mutableListOf<Double>()
            for (server in loadBalancer.getAllServers()) {
                val ss = stats.getSingleServerStat(server)
                val weight = totalResponseTime - ss.responseTimeAvg
                weightSoFar += weight
                finalWeights.add(weightSoFar)
            }

            accumulatedWeights = finalWeights
        }
    }
}