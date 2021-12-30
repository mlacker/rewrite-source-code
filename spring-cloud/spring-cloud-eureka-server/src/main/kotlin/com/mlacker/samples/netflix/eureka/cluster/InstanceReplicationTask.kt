package com.mlacker.samples.netflix.eureka.cluster

import com.mlacker.samples.netflix.appinfo.InstanceInfo
import com.mlacker.samples.netflix.appinfo.InstanceStatus
import com.mlacker.samples.netflix.eureka.registry.PeerAwareInstanceRegistryImpl

abstract class InstanceReplicationTask(
    peerNodeName: String,
    action: PeerAwareInstanceRegistryImpl.Action,
    val appName: String,
    val id: String
) : ReplicationTask(peerNodeName, action) {

    var instanceInfo: InstanceInfo? = null
    var overriddenStatus: InstanceStatus? = null
    var replicateInstanceInfo: Boolean = false

    constructor(
        peerNodeName: String,
        action: PeerAwareInstanceRegistryImpl.Action,
        instanceInfo: InstanceInfo,
        overriddenStatus: InstanceStatus?,
        replicateInstanceInfo: Boolean
    ): this(peerNodeName, action, instanceInfo.appName, instanceInfo.id) {
        this.instanceInfo = instanceInfo
        this.overriddenStatus = overriddenStatus
        this.replicateInstanceInfo = replicateInstanceInfo
    }

    override val taskName: String
        get() = "$appName/$id:$action@$peerNodeName"
}