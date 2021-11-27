package com.mlacker.samples.netflix.discovery.shared

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.mlacker.samples.netflix.appinfo.InstanceInfo
import java.lang.StringBuilder
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.ArrayList

class Applications(
    @JsonIgnore var appsHashCode: String? = null,
    registeredApplications: List<Application> = emptyList(),
) {
    private val applications: AbstractQueue<Application> = ConcurrentLinkedQueue()
    private val appNameApplicationMap: MutableMap<String, Application> = ConcurrentHashMap()
    private val virtualHostNameAppMap: MutableMap<String, VipIndexSupport> = ConcurrentHashMap()

    init {
        for (app in registeredApplications) {
            addApplication(app)
        }
    }

    fun addApplication(app: Application) {
        appNameApplicationMap[app.name.toUpperCase()] = app
        addInstancesToVIPMaps(app, this.virtualHostNameAppMap)
        applications.add(app)
    }

    val registeredApplications: List<Application>
        @JsonProperty("application")
        get() = ArrayList(this.applications)

    fun getRegisteredApplications(appName: String): Application? =
        appNameApplicationMap[appName.toUpperCase()]

    fun getInstancesByVirtualHostName(virtualHostName: String): List<InstanceInfo> =
        this.virtualHostNameAppMap[virtualHostName.toUpperCase()]?.vipList?.get() ?: emptyList()

    fun size(): Int = applications.map { it.size() }.sum()

    fun getReconcileHashCode(): String {
        val instanceCountMap: TreeMap<String, AtomicInteger> = TreeMap()
        populateInstanceCountMap(instanceCountMap)
        return getReconcileHashCode(instanceCountMap)
    }

    private fun getReconcileHashCode(instanceCountMap: Map<String, AtomicInteger>): String {
        val reconcileHashCode = StringBuilder(75)
        instanceCountMap.forEach { (key, value) ->
            reconcileHashCode.append(key)
                .append("_")
                .append(value.get())
                .append("_")
        }
        return reconcileHashCode.toString()
    }

    fun populateInstanceCountMap(instanceCountMap: MutableMap<String, AtomicInteger>) {
        for (app in this.registeredApplications) {
            for (info in app.getInstancesAsIsFromEureka()) {
                val instanceCount = instanceCountMap
                    .computeIfAbsent(info.status.name) { AtomicInteger() }
                instanceCount.incrementAndGet()
            }
        }
    }

    fun shuffleInstances(filterUpInstances: Boolean) {
        val virtualHostNameAppMap: MutableMap<String, VipIndexSupport> = mutableMapOf()
        for (application in appNameApplicationMap.values) {
            application.shuffleAndStoreInstances(filterUpInstances)
            this.addInstancesToVIPMaps(application, virtualHostNameAppMap)
        }
        shuffleAndFilterInstances(virtualHostNameAppMap, filterUpInstances)

        this.virtualHostNameAppMap.putAll(virtualHostNameAppMap)
        this.virtualHostNameAppMap.keys.retainAll(virtualHostNameAppMap.keys)
    }

    fun getNextIndex(virtualHostname: String): AtomicLong =
        this.virtualHostNameAppMap[virtualHostname.toUpperCase()]?.roundRobinIndex ?: AtomicLong()

    private fun shuffleAndFilterInstances(srcMap: MutableMap<String, VipIndexSupport>, filterUpInstances: Boolean) {
        for ((_, vipIndexSupport) in srcMap) {
            val filteredInstances: List<InstanceInfo> = vipIndexSupport.instances
                .filter { !filterUpInstances || it.status == InstanceInfo.InstanceStatus.UP }
                .toMutableList()
                .shuffled()

            vipIndexSupport.vipList.set(filteredInstances)
            vipIndexSupport.roundRobinIndex.set(0)
        }
    }

    private fun addInstanceToMap(info: InstanceInfo, vipAddresses: String?, vipMap: MutableMap<String, VipIndexSupport>) {
        if (vipAddresses != null) {
            for (vipAddress in vipAddresses.toUpperCase().split(",")) {
                vipMap.computeIfAbsent(vipAddress) { VipIndexSupport() }.instances.add(info)
            }
        }
    }

    private fun addInstancesToVIPMaps(app: Application, virtualHostNameAppMap: MutableMap<String, VipIndexSupport>) {
        for (info in app.getInstances()) {
            if (info.vipAddress != null) {
                addInstanceToMap(info, info.vipAddress, virtualHostNameAppMap)
            }
        }
    }

    fun removeApplication(app: Application) {
        this.appNameApplicationMap.remove(app.name.toUpperCase())
        this.applications.remove(app)
    }

    private class VipIndexSupport {
        val instances: AbstractQueue<InstanceInfo> = ConcurrentLinkedQueue()
        val roundRobinIndex: AtomicLong = AtomicLong(0)
        val vipList: AtomicReference<List<InstanceInfo>> = AtomicReference(emptyList())
    }
}