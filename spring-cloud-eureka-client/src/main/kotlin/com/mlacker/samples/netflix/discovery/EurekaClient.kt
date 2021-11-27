package com.mlacker.samples.netflix.discovery

import com.mlacker.samples.netflix.appinfo.ApplicationInfoManager
import com.mlacker.samples.netflix.discovery.shared.Applications
import com.mlacker.samples.netflix.appinfo.InstanceInfo
import com.mlacker.samples.netflix.discovery.shared.LookupService
import com.netflix.discovery.EurekaClientConfig

interface EurekaClient: LookupService {

    val applicationInfoManager: ApplicationInfoManager

    val clientConfig: EurekaClientConfig

    fun getApplications(serviceUrl: String): Applications?

    fun getInstancesByVipAddress(vipAddress: String?): List<InstanceInfo>

    fun shutdown()
}