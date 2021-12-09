package com.mlacker.samples.netflix.eureka.registry

import com.mlacker.samples.netflix.appinfo.InstanceInfo
import com.netflix.eureka.cluster.PeerEurekaNodes
import com.netflix.eureka.resources.ASGResource

interface PeerAwareInstanceRegistry : InstanceRegistry {

    fun init(peerEurekaNodes: PeerEurekaNodes)

    fun syncUp(): Int

    fun shouldAllowAccess(): Boolean

    fun register(info: InstanceInfo, isReplication: Boolean)

    fun statusUpdate(asgName: String, newStatus: ASGResource.ASGStatus, isReplication: Boolean)
}
