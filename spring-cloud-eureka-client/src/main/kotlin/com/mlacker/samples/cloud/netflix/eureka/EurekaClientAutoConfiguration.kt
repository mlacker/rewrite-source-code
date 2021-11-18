package com.mlacker.samples.cloud.netflix.eureka

import com.mlacker.samples.cloud.client.ConditionalOnDiscoveryEnabled
import com.mlacker.samples.discovery.DiscoveryClient
import com.mlacker.samples.discovery.EurekaClient
import com.netflix.appinfo.ApplicationInfoManager
import com.netflix.appinfo.EurekaInstanceConfig
import com.netflix.discovery.EurekaClientConfig
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.cloud.commons.util.IdUtils
import org.springframework.cloud.commons.util.InetUtils
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.ConfigurableEnvironment

@Configuration
@ConditionalOnDiscoveryEnabled
@AutoConfigureAfter(
    name = [
        "org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration"
    ]
)
class EurekaClientAutoConfiguration(private val env: ConfigurableEnvironment) {

    @Bean
    @ConditionalOnMissingBean(EurekaInstanceConfig::class)
    fun eurekaInstanceConfigBean(inetUtils: InetUtils): EurekaInstanceConfigBean {
        val instance = EurekaInstanceConfigBean(inetUtils)

        instance.instanceId = IdUtils.getDefaultInstanceId(env)
        instance.nonSecurePort = env.getProperty("server.port", Int::class.java, 8080)
        instance.securePort = instance.nonSecurePort
        instance.isPreferIpAddress = true

        env.getProperty("eureka.instance.ip-address")?.let { instance.ipAddress = it }
        env.getProperty("eureka.instance.hostname")?.let { instance.hostname = it }

        return instance
    }

    @Configuration
    protected class EurekaClientConfiguration {

        @Bean(destroyMethod = "shutdown")
        @ConditionalOnMissingBean(EurekaClient::class)
        fun eurekaClient(manager: ApplicationInfoManager, config: EurekaClientConfig): EurekaClient {
            return DiscoveryClient(manager, config)
        }
    }
}