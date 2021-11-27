package com.mlacker.samples.cloud.netflix.eureka.http

import com.mlacker.samples.netflix.appinfo.InstanceInfo
import com.mlacker.samples.netflix.discovery.shared.Application
import com.mlacker.samples.netflix.discovery.shared.Applications
import com.mlacker.samples.netflix.discovery.shared.transport.EurekaHttpClient
import com.netflix.discovery.shared.transport.EurekaHttpResponse
import com.netflix.discovery.shared.transport.EurekaHttpResponse.anEurekaHttpResponse
import org.springframework.http.HttpEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

class RestTemplateEurekaHttpClient(
    serviceUrl: String,
) : EurekaHttpClient {

    private val restTemplate: RestTemplate = RestTemplate()
    private val serviceUrl: String = serviceUrl + if (serviceUrl.endsWith("/")) "" else "/"

    override fun register(info: InstanceInfo): EurekaHttpResponse<Void> {
        val response = restTemplate.postForEntity("${serviceUrl}apps/${info.appName}",
            HttpEntity(info), Void::class.java)

        return anEurekaHttpResponse(response.statusCodeValue)
            .headers(headersOf(response)).build()
    }

    override fun cancel(appName: String, id: String): EurekaHttpResponse<Void> {
        TODO("Not yet implemented")
    }

    override fun sendHeartBeat(
        appName: String,
        id: String,
        info: InstanceInfo,
        overriddenStatus: InstanceInfo.InstanceStatus?,
    ): EurekaHttpResponse<InstanceInfo> {
        TODO("Not yet implemented")
    }

    override fun statusUpdate(appName: String, id: String, newStatus: InstanceInfo.InstanceStatus, info: InstanceInfo): EurekaHttpResponse<Void> {
        TODO("Not yet implemented")
    }

    override fun deleteStatusOverride(appName: String, id: String, info: InstanceInfo): EurekaHttpResponse<Void> {
        TODO("Not yet implemented")
    }

    override fun getApplications(): EurekaHttpResponse<Applications> {
        TODO("Not yet implemented")
    }

    override fun getDelta(): EurekaHttpResponse<Applications> {
        TODO("Not yet implemented")
    }

    override fun getVip(vipAddress: String): EurekaHttpResponse<Applications> {
        TODO("Not yet implemented")
    }

    override fun getApplication(appName: String): EurekaHttpResponse<Application> {
        TODO("Not yet implemented")
    }

    override fun getInstance(appName: String, id: String): EurekaHttpResponse<InstanceInfo> {
        TODO("Not yet implemented")
    }

    override fun getInstance(id: String): EurekaHttpResponse<InstanceInfo> {
        TODO("Not yet implemented")
    }

    override fun shutdown() {
        // Nothing to do
    }

    private fun headersOf(response: ResponseEntity<*>): Map<String, String> {
        if (response.headers.isNullOrEmpty()) {
            return emptyMap()
        }

        return response.headers
            .filter { it.value.isNotEmpty() }
            .mapValues { it.value[0] }
    }
}