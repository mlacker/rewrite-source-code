package com.mlacker.samples.netflix.loadbalaance

import com.netflix.loadbalancer.Server

interface ILoadBalancer {

    fun addServers(newServers: List<Server>)

    fun chooseServer(key: Any?): Server?

    fun markServerDown(server: Server)

    fun getReachableServers(): List<Server>

    fun getAllServers(): List<Server>
}