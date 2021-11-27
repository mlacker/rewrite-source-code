package com.mlacker.samples.cloud.netflix.eureka

import com.mlacker.samples.netflix.appinfo.InstanceInfo
import com.netflix.appinfo.EurekaInstanceConfig
import com.netflix.appinfo.LeaseInfo

class InstanceInfoFactory {

    fun create(config: EurekaInstanceConfig): InstanceInfo {
        val leaseInfoBuilder = LeaseInfo.Builder.newBuilder()
            .setRenewalIntervalInSecs(config.leaseRenewalIntervalInSeconds)
            .setDurationInSecs(config.leaseExpirationDurationInSeconds)

        return InstanceInfo(
            instanceId = config.instanceId,
            appName = config.appname,
            ipAddr = config.ipAddress,
            port = config.nonSecurePort,
            homePageUrl = config.homePageUrl,
            healthCheckUrl = config.healthCheckUrl,
            vipAddress = config.virtualHostName,
            hostName = config.getHostName(false),
            leaseInfo = leaseInfoBuilder.build()
        )
    }
}