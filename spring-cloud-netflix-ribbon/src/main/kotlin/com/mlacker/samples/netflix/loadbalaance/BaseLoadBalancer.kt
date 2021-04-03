package com.mlacker.samples.netflix.loadbalaance

import com.netflix.client.config.IClientConfig
import com.netflix.loadbalancer.IPing
import com.netflix.loadbalancer.IPingStrategy
import com.netflix.loadbalancer.IRule
import com.netflix.loadbalancer.LoadBalancerStats
import com.netflix.loadbalancer.RoundRobinRule
import com.netflix.loadbalancer.Server
import org.slf4j.LoggerFactory
import java.util.*

open class BaseLoadBalancer(
    protected var name: String = "default",
    protected val clientConfig: IClientConfig? = null,
    protected val rule: IRule = RoundRobinRule(),
    final override val loadBalancerStats: LoadBalancerStats = LoadBalancerStats(name),
    protected val ping: IPing? = null,
    private val pingStrategy: IPingStrategy = SerialPingStrategy()
) : AbstractLoadBalancer() {

    companion object {
        private val logger = LoggerFactory.getLogger(BaseLoadBalancer::class.java)
    }

    private var allServerList: MutableList<Server> = mutableListOf()
    private var upServerList: List<Server> = emptyList()

    private lateinit var lbTimer: Timer
    private val pingIntervalSeconds: Long = 10

    init {
        setupPingTask()
    }

    private fun setupPingTask() {
        lbTimer = Timer("NFLoadBalancer-PingTimer-$name", true)
        lbTimer.schedule(object : TimerTask() {
            override fun run() {
                runPinger()
            }
        }, 0, pingIntervalSeconds * 1000)
    }

    override fun addServers(newServers: List<Server>) {
        val newList = mutableListOf<Server>()
        newList.addAll(allServerList)
        newList.addAll(newServers)
        setServersList(newList)
    }

    open fun setServersList(serverList: List<Server>) {
        val allServers = mutableListOf<Server>()
        for (server in serverList) {
            allServers.add(server)
        }
        allServerList = allServers
    }

    override fun getReachableServers(): List<Server> = Collections.unmodifiableList(upServerList)

    override fun getAllServers(): List<Server> = Collections.unmodifiableList(allServerList)

    override fun chooseServer(key: Any?): Server? {
        return try {
            rule.choose(key)
        } catch (ex: Exception) {
            logger.warn("LoadBalancer [$name]:  Error choosing server for key $key", ex)
            null
        }
    }

    override fun markServerDown(server: Server) {
        server.isAlive = false
    }

    fun runPinger() {
        val results = pingStrategy.pingServers(ping, allServerList.toTypedArray())

        upServerList = allServerList.filterIndexed { i, _ -> results[i] }
    }

    open fun shutdown() {
        lbTimer.cancel()
    }

    private class SerialPingStrategy : IPingStrategy {
        override fun pingServers(ping: IPing?, servers: Array<out Server>): BooleanArray {
            val results = BooleanArray(servers.size) { false }

            for (i in 0..servers.size) {
                try {
                    ping?.run { results[i] = isAlive(servers[i]) }
                } catch (ex: Exception) {
                    logger.error("Exception while pinging Server: '${servers[i]}'", ex)
                }
            }

            return results
        }
    }
}