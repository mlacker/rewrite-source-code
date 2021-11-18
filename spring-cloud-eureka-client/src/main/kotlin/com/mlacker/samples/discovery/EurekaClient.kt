package com.mlacker.samples.discovery

import com.mlacker.samples.discovery.shared.LookupService
import com.netflix.appinfo.ApplicationInfoManager
import com.netflix.appinfo.InstanceInfo
import com.netflix.discovery.EurekaClientConfig
import com.netflix.discovery.shared.Applications

interface EurekaClient: LookupService {

    val applicationInfoManager: ApplicationInfoManager

    val clientConfig: EurekaClientConfig

    fun getApplications(serviceUrl: String): Applications?

    fun getInstancesByVipAddress(vipAddress: String?): List<InstanceInfo>

    fun shutdown()
}