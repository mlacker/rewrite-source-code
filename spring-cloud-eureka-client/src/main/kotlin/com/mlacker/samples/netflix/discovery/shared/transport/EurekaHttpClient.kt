package com.mlacker.samples.netflix.discovery.shared.transport

import com.mlacker.samples.netflix.discovery.shared.Application
import com.mlacker.samples.netflix.discovery.shared.Applications
import com.mlacker.samples.netflix.appinfo.InstanceInfo
import com.netflix.discovery.shared.transport.EurekaHttpResponse

interface RegistrationClient {

    fun register(info: InstanceInfo): EurekaHttpResponse<Void>

    fun cancel(appName: String, id: String): EurekaHttpResponse<Void>

    fun sendHeartBeat(appName: String, id: String, info: InstanceInfo, overriddenStatus: InstanceInfo.InstanceStatus?): EurekaHttpResponse<InstanceInfo>

    fun statusUpdate(appName: String, id: String, newStatus: InstanceInfo.InstanceStatus, info: InstanceInfo): EurekaHttpResponse<Void>

    fun deleteStatusOverride(appName: String, id: String, info: InstanceInfo): EurekaHttpResponse<Void>
}

interface QueryClient {

    fun getApplications(): EurekaHttpResponse<Applications>

    fun getDelta(): EurekaHttpResponse<Applications>

    fun getVip(vipAddress: String): EurekaHttpResponse<Applications>

    fun getApplication(appName: String): EurekaHttpResponse<Application>

    fun getInstance(appName: String, id: String): EurekaHttpResponse<InstanceInfo>

    fun getInstance(id: String): EurekaHttpResponse<InstanceInfo>
}

interface EurekaHttpClient : RegistrationClient, QueryClient {

    fun shutdown()
}