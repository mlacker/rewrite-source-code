package com.mlacker.samples.cloud.netflix.eureka.config

import com.mlacker.samples.cloud.netflix.eureka.http.RestTemplateEurekaHttpClient
import com.mlacker.samples.netflix.discovery.endpoint.EndpointUtils
import com.mlacker.samples.netflix.discovery.shared.transport.EurekaHttpClient
import com.netflix.discovery.EurekaClientConfig
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@ConditionalOnProperty("spring.cloud.config.discovery.enabled", matchIfMissing = false)
@Configuration
@EnableConfigurationProperties
class EurekaConfigServerBootstrapConfiguration {

    @Bean
    @ConditionalOnMissingBean(EurekaClientConfig::class)
    fun eurekaClientConfigBean() = EurekaClientConfigBean()

    @Bean
    @ConditionalOnMissingBean(EurekaHttpClient::class)
    fun configDiscoveryRestTemplateEurekaHttpClient(config: EurekaClientConfigBean, env: Environment): RestTemplateEurekaHttpClient {
        val serviceUrls = EndpointUtils.getServiceUrlsFromConfig(
            config, EurekaClientConfigBean.DEFAULT_ZONE, config.shouldPreferSameZoneEureka())
        return RestTemplateEurekaHttpClient(serviceUrls[0])
    }
}