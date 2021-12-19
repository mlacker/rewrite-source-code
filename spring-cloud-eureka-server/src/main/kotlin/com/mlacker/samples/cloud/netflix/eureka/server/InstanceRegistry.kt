package com.mlacker.samples.cloud.netflix.eureka.server

import com.mlacker.samples.netflix.appinfo.ApplicationInfoManager
import com.mlacker.samples.netflix.appinfo.InstanceInfo
import com.mlacker.samples.netflix.discovery.EurekaClient
import com.mlacker.samples.netflix.eureka.registry.PeerAwareInstanceRegistryImpl
import com.netflix.discovery.EurekaClientConfig
import com.netflix.eureka.EurekaServerConfig
import com.netflix.eureka.lease.Lease
import com.netflix.eureka.resources.ServerCodecs
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceCanceledEvent
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRenewedEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationEvent

class InstanceRegistry(
    serverConfig: EurekaServerConfig,
    clientConfig: EurekaClientConfig,
    serverCodecs: ServerCodecs,
    eurekaClient: EurekaClient,
    expectedNumberOfClientsSendingRenews: Int,
    private val defaultOpenForTrafficCount: Int,
) :
    PeerAwareInstanceRegistryImpl(serverConfig, clientConfig, serverCodecs, eurekaClient),
    ApplicationContextAware {

    private lateinit var ctxt: ApplicationContext

    init {
        this.expectedNumberOfClientsSendingRenews = expectedNumberOfClientsSendingRenews
    }

    // 64
    override fun setApplicationContext(context: ApplicationContext) {
        this.ctxt = context
    }

    // 78
    override fun openForTraffic(applicationInfoManager: ApplicationInfoManager, count: Int) {
        super.openForTraffic(applicationInfoManager,
            if (count == 0) this.defaultOpenForTrafficCount else count)
    }

    // 84
    override fun register(r: InstanceInfo, leaseDuration: Int, isReplication: Boolean) {
        handleRegistration(r, leaseDuration, isReplication)
        super.register(r, leaseDuration, isReplication)
    }

    // 90
    override fun register(info: InstanceInfo, isReplication: Boolean) {
        handleRegistration(info, resolveInstanceLeaseDuration(info), isReplication)
        super.register(info, isReplication)
    }

    // 96
    override fun cancel(appName: String, id: String, isReplication: Boolean): Boolean {
        handleCancelation(appName, id, isReplication)
        return super.cancel(appName, id, isReplication)
    }

    // 102
    override fun renew(appName: String, id: String, isReplication: Boolean): Boolean {
        val applications = getSortedApplications()
        for (input in applications) {
            if (input.name == appName) {
                val instance = input.instances
                    .firstOrNull { it.id == id }

                publishEvent(EurekaInstanceRenewedEvent(this, appName, id,
                    wrapInstance(instance), isReplication))
                break
            }
        }
        return super.renew(appName, id, isReplication)
    }

    // 125
    override fun internalCancel(appName: String, id: String, isReplication: Boolean): Boolean {
        handleCancelation(appName, id, isReplication)
        return super.internalCancel(appName, id, isReplication)
    }

    // 130
    private fun handleCancelation(appName: String, id: String, isReplication: Boolean) {
        publishEvent(EurekaInstanceCanceledEvent(this, appName, id, isReplication))
    }

    // 136
    private fun handleRegistration(info: InstanceInfo, leaseDuration: Int, isReplication: Boolean) {
        publishEvent(EurekaInstanceRegisteredEvent(this, wrapInstance(info), leaseDuration, isReplication))
    }

    // 151
    private fun publishEvent(event: ApplicationEvent) {
        this.ctxt.publishEvent(event)
    }

    // 155
    private fun resolveInstanceLeaseDuration(info: InstanceInfo): Int {
        return if (info.leaseInfo.durationInSecs > 0) {
            info.leaseInfo.durationInSecs
        } else {
            Lease.DEFAULT_DURATION_IN_SECS
        }
    }

    private fun wrapInstance(info: InstanceInfo?) = info as com.netflix.appinfo.InstanceInfo?
}