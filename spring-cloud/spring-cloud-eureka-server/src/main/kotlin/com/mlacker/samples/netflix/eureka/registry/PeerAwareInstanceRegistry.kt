package com.mlacker.samples.netflix.eureka.registry

import com.mlacker.samples.netflix.appinfo.InstanceInfo
import com.mlacker.samples.netflix.eureka.cluster.PeerEurekaNodes

interface PeerAwareInstanceRegistry : InstanceRegistry {

    fun init(peerEurekaNodes: PeerEurekaNodes)

    fun syncUp(): Int

    fun shouldAllowAccess(): Boolean

    fun register(info: InstanceInfo, isReplication: Boolean)
}
