package com.mlacker.samples.netflix.loadbalaance

import com.netflix.client.ClientFactory
import com.netflix.client.config.IClientConfig
import com.netflix.loadbalancer.IPing
import com.netflix.loadbalancer.IRule
import com.netflix.loadbalancer.Server
import com.netflix.loadbalancer.ServerList
import com.netflix.loadbalancer.ServerListFilter
import com.netflix.loadbalancer.ServerListUpdater
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class ZoneAwareLoadBalancer<T : Server>(
    clientConfig: IClientConfig,
    rule: IRule,
    ping: IPing,
    serverList: ServerList<T>?,
    filter: ServerListFilter<T>?,
    serverListUpdater: ServerListUpdater
) : DynamicServerListLoadBalancer<T>(clientConfig, rule, ping, serverList, filter, serverListUpdater) {

    private val balancers = ConcurrentHashMap<String, BaseLoadBalancer>()

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun setServerListForZones(zoneServersMap: HashMap<String, MutableList<Server>>) {
        super.setServerListForZones(zoneServersMap)

        for (entry in zoneServersMap) {
            val zone = entry.key.toLowerCase()
            getLoadBalancer(zone).setServersList(entry.value)
        }

        for (existingLBEntry in balancers) {
            if (!zoneServersMap.containsKey(existingLBEntry.key)) {
                existingLBEntry.value.setServersList(emptyList<T>())
            }
        }
    }

    override fun chooseServer(key: Any?): Server? {
        if (loadBalancerStats.availableZones.size <= 1) {
            logger.debug("Zone aware logic disabled or there is only one zone")
            return super.chooseServer(key)
        }

        return super.chooseServer(key)
    }

    private fun getLoadBalancer(text: String): BaseLoadBalancer {
        val zone = text.toLowerCase()
        var loadBalancer = balancers[zone]

        if (loadBalancer == null) {
            val rule = cloneRule(this.rule)
            loadBalancer = BaseLoadBalancer(name = "${this.name}_$zone", rule = rule, loadBalancerStats = this.loadBalancerStats)
            val prev = balancers.putIfAbsent(zone, loadBalancer)
            if (prev != null) {
                loadBalancer = prev
            }
        }

        return loadBalancer
    }

    private fun cloneRule(toClone: IRule): IRule {
        return ClientFactory.instantiateInstanceWithClientConfig(toClone.javaClass.name, this.clientConfig) as IRule
    }
}