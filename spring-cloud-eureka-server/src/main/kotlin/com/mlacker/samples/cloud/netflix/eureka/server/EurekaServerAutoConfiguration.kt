package com.mlacker.samples.cloud.netflix.eureka.server

import com.netflix.appinfo.ApplicationInfoManager
import com.netflix.discovery.EurekaClient
import com.netflix.discovery.EurekaClientConfig
import com.netflix.eureka.DefaultEurekaServerContext
import com.netflix.eureka.EurekaServerConfig
import com.netflix.eureka.EurekaServerContext
import com.netflix.eureka.cluster.PeerEurekaNodes
import com.netflix.eureka.registry.PeerAwareInstanceRegistry
import com.netflix.eureka.resources.ServerCodecs
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.netflix.eureka.server.EurekaDashboardProperties
import org.springframework.cloud.netflix.eureka.server.EurekaServerBootstrap
import org.springframework.cloud.netflix.eureka.server.EurekaServerConfigBean
import org.springframework.cloud.netflix.eureka.server.InstanceRegistry
import org.springframework.cloud.netflix.eureka.server.InstanceRegistryProperties
import org.springframework.cloud.netflix.eureka.server.ReplicationClientAdditionalFilters
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@ConditionalOnBean(EurekaServerMarkerConfiguration.Marker::class)
@EnableConfigurationProperties(EurekaDashboardProperties::class, InstanceRegistryProperties::class)
@PropertySource("classpath:/eureka/server.properties")
class EurekaServerAutoConfiguration(
    private val applicationInfoManager: ApplicationInfoManager,
    private val eurekaServerConfig: EurekaServerConfig,
    private val eurekaClientConfig: EurekaClientConfig,
    private val eurekaClient: EurekaClient,
    private val instanceRegistryProperties: InstanceRegistryProperties,
) : WebMvcConfigurer {

    @Bean
    fun peerAwareInstanceRegistry(serverCodecs: ServerCodecs): PeerAwareInstanceRegistry {
        // force initialization
        this.eurekaClient.applications
        return InstanceRegistry(this.eurekaServerConfig, this.eurekaClientConfig,
            serverCodecs, this.eurekaClient,
            this.instanceRegistryProperties.expectedNumberOfClientsSendingRenews,
            this.instanceRegistryProperties.defaultOpenForTrafficCount)
    }

    @Bean
    @ConditionalOnMissingBean
    fun peerEurekaNodes(
        registry: PeerAwareInstanceRegistry, serverCodes: ServerCodecs,
        replicationClientAdditionalFilters: ReplicationClientAdditionalFilters,
    ): PeerEurekaNodes {
        return PeerEurekaNodes(registry, this.eurekaServerConfig,
            this.eurekaClientConfig, serverCodes, this.applicationInfoManager)
    }

    @Bean
    @ConditionalOnMissingBean
    fun eurekaServerContext(
        serverCodecs: ServerCodecs,
        registry: PeerAwareInstanceRegistry, peerEurekaNodes: PeerEurekaNodes,
    ): EurekaServerContext {
        return DefaultEurekaServerContext(this.eurekaServerConfig, serverCodecs,
            registry, peerEurekaNodes, this.applicationInfoManager)
    }

    @Bean
    fun eurekaServerBootstrap(registry: PeerAwareInstanceRegistry, serverContext: EurekaServerContext)
            : EurekaServerBootstrap {
        return EurekaServerBootstrap(this.applicationInfoManager,
            this.eurekaClientConfig, this.eurekaServerConfig, registry, serverContext)
    }

    @Configuration
    protected class EurekaServerConfigBeanConfiguration {

        @Bean
        @ConditionalOnMissingBean
        fun eurekaServerConfig(clientConfig: EurekaClientConfig): EurekaServerConfig {
            val server = EurekaServerConfigBean()
            if (clientConfig.shouldRegisterWithEureka()) {
                // Set a sensible default if we are supposed to replicate
                server.registrySyncRetries = 5
            }
            return server
        }
    }
}