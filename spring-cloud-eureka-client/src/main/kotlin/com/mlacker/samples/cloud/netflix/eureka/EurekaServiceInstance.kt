package com.mlacker.samples.cloud.netflix.eureka

import com.mlacker.samples.cloud.client.ServiceInstance
import com.mlacker.samples.netflix.appinfo.InstanceInfo
import java.net.URI

class EurekaServiceInstance(val instance: InstanceInfo) : ServiceInstance {

    override val instanceId: String
        get() = instance.id
    override val serviceId: String
        get() = instance.appName
    override val host: String
        get() = instance.hostName
    override val port: Int
        get() = instance.port
    override val uri: URI
        get() = URI.create("http://$host:$port")
    override val metadata: Map<String, String>
        get() = instance.metadata
}