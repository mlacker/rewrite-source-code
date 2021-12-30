package com.mlacker.samples.netflix.discovery.shared

import com.mlacker.samples.netflix.appinfo.InstanceInfo
import com.mlacker.samples.netflix.appinfo.InstanceStatus
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class Application(
    var name: String,
    instances: List<InstanceInfo> = emptyList(),
) {
    val instances: MutableSet<InstanceInfo> = LinkedHashSet()
    private val instancesMap: MutableMap<String, InstanceInfo> = ConcurrentHashMap()
    private val shuffledInstances: AtomicReference<List<InstanceInfo>> = AtomicReference()
    private var isDirty: Boolean = false

    init {
        for (instance in instances) {
            addInstance(instance)
        }
    }

    fun addInstance(instance: InstanceInfo) {
        instancesMap[instance.id] = instance
        synchronized(instances) {
            instances.remove(instance)
            instances.add(instance)
            isDirty = true
        }
    }

    fun removeInstance(instance: InstanceInfo) {
        instancesMap.remove(instance.id)
        synchronized(instances) {
            instances.remove(instance)
            isDirty = true
        }
    }

    fun getInstances(): List<InstanceInfo> =
        shuffledInstances.get() ?: getInstancesAsIsFromEureka()

    fun getInstancesAsIsFromEureka(): List<InstanceInfo> =
        ArrayList(this.instances)

    fun getInstanceById(id: String): InstanceInfo? = instancesMap[id]

    fun size(): Int = instances.size

    fun shuffleAndStoreInstances(filterUpInstances: Boolean) {
        val instanceInfoList: MutableList<InstanceInfo>
        synchronized(instances) {
            instanceInfoList = ArrayList(instances)
        }

        if (filterUpInstances) {
            val it = instanceInfoList.iterator()
            while (it.hasNext()) {
                val instanceInfo = it.next()
                if (instanceInfo.status != InstanceStatus.UP) {
                    it.remove()
                }
            }
        }

        instanceInfoList.shuffle()
        this.shuffledInstances.set(instanceInfoList)
    }
}