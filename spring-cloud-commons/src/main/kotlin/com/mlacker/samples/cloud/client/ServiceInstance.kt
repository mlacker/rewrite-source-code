package com.mlacker.samples.cloud.client

import java.net.URI

interface ServiceInstance {

    val instanceId: String
    val serviceId: String
    val host: String
    val port: Int
    val uri: URI
    val metadata: Map<String, String>
}
