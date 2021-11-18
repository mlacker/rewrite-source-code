package com.mlacker.samples.cloud.netflix.eureka

import com.mlacker.samples.cloud.client.ConditionalOnDiscoveryEnabled
import com.mlacker.samples.discovery.EurekaClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnDiscoveryEnabled
class EurekaDiscoveryClientConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun discoveryClient(client: EurekaClient): EurekaDiscoveryClient {
        return EurekaDiscoveryClient(client)
    }
}