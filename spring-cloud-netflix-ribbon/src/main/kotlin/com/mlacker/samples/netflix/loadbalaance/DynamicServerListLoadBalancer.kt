package com.mlacker.samples.netflix.loadbalaance

import com.netflix.client.config.IClientConfig
import com.netflix.loadbalancer.*
import org.slf4j.LoggerFactory

open class DynamicServerListLoadBalancer<T : Server>(
    clientConfig: IClientConfig, rule: IRule, ping: IPing,
    private val serverList: ServerList<T>? = null,
    private val filter: ServerListFilter<T>? = null,
    private val serverListUpdater: ServerListUpdater = PollingServerListUpdater()
) : BaseLoadBalancer(clientConfig = clientConfig, rule = rule, ping = ping) {

    companion object {
        private val logger = LoggerFactory.getLogger(DynamicServerListLoadBalancer::class.java)
    }

    init {
        if (filter is AbstractServerListFilter) {
            filter.loadBalancerStats = loadBalancerStats
        }
        restOfInit()
    }

    private fun restOfInit() {
        enableAndInitLearnNewServersFeature()

        updateListOfServers()
    }

    override fun setServersList(serverList: List<Server>) {
        super.setServersList(serverList)
        val serversInZones = HashMap<String, MutableList<Server>>()
        for (server in serverList) {
            loadBalancerStats.getSingleServerStat(server)
            var zone = server.zone
            if (zone != null) {
                zone = zone.toLowerCase()
                var servers = serversInZones[zone]
                if (servers == null) {
                    servers = mutableListOf()
                    serversInZones[zone] = servers
                }
                servers.add(server)
            }
        }
        setServerListForZones(serversInZones)
    }

    protected open fun setServerListForZones(zoneServersMap: HashMap<String, MutableList<Server>>) {
        logger.debug("Setting server list for zones: $zoneServersMap")
        loadBalancerStats.updateZoneServerMapping(zoneServersMap)
    }

    private fun enableAndInitLearnNewServersFeature() {
        serverListUpdater.start { updateListOfServers() }
    }

    private fun stopServerListRefreshing() {
        serverListUpdater.stop()
    }

    private val identifier = this.clientConfig?.clientName

    private fun updateListOfServers() {
        var servers = emptyList<T>()

        if (serverList != null) {
            servers = serverList.updatedListOfServers
            logger.debug("List of Servers for $identifier obtained from Discovery client: $servers")

            if (filter != null) {
                servers = filter.getFilteredListOfServers(servers)
                logger.debug("Filtered List of Servers for $identifier obtained from Discovery client: $servers")
            }
        }
        updateAllServerList(servers)
    }

    private fun updateAllServerList(servers: List<T>) {
        for (server in servers) {
            server.isAlive = true
        }
        setServersList(servers)
    }

    override fun shutdown() {
        super.shutdown()
        stopServerListRefreshing()
    }
}