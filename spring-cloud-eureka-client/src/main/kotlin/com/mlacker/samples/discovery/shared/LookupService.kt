package com.mlacker.samples.discovery.shared

import com.netflix.appinfo.InstanceInfo
import com.netflix.discovery.shared.Application
import com.netflix.discovery.shared.Applications

interface LookupService {

    fun getApplication(appName: String): Application?

    fun getApplications(): Applications

    fun getInstancesById(id: String): List<InstanceInfo>

    fun getNextServerFromEureka(virtualHostname: String): InstanceInfo
}