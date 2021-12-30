package com.mlacker.samples.netflix.eureka.cluster

import com.mlacker.samples.netflix.eureka.registry.PeerAwareInstanceRegistryImpl
import com.netflix.discovery.shared.transport.EurekaHttpResponse

abstract class ReplicationTask(
    protected val peerNodeName: String,
    val action: PeerAwareInstanceRegistryImpl.Action,
) {
    abstract val taskName: String

    abstract fun execute(): EurekaHttpResponse<*>

    open fun handleSuccess() {
    }

    open fun handleFailure(statusCode: Int, responseEntity: Any) {
    }
}