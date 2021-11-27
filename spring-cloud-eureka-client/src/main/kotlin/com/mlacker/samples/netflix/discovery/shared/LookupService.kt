package com.mlacker.samples.netflix.discovery.shared

import com.mlacker.samples.netflix.appinfo.InstanceInfo

interface LookupService {

    fun getApplication(appName: String): Application?

    fun getApplications(): Applications

    fun getInstancesById(id: String): List<InstanceInfo>

    fun getNextServerFromEureka(virtualHostname: String): InstanceInfo
}