package com.mlacker.samples.cloud.loadbalancer.config

import com.mlacker.samples.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.client.loadbalancer.AsyncLoadBalancerAutoConfiguration
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients
import org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ConfigurationCondition
import org.springframework.context.annotation.Primary

@Configuration(proxyBeanMethods = false)
@LoadBalancerClients
@AutoConfigureAfter(LoadBalancerAutoConfiguration::class)
@AutoConfigureBefore(
    org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration::class,
    AsyncLoadBalancerAutoConfiguration::class
)
class BlockingLoadBalancerClientAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = ["org.springframework.web.client.RestTemplate"])
    @Conditional(OnNoRibbonDefaultCondition::class)
    protected class BlockingLoadbalancerClientConfig {

        @Bean
        @ConditionalOnBean(LoadBalancerClientFactory::class)
        @Primary
        fun blockingLoadBalancerClient(loadBalancerClientFactory: LoadBalancerClientFactory)
                : BlockingLoadBalancerClient {
            return BlockingLoadBalancerClient(loadBalancerClientFactory)
        }
    }

    class OnNoRibbonDefaultCondition
        : AnyNestedCondition(ConfigurationCondition.ConfigurationPhase.REGISTER_BEAN) {

        @ConditionalOnProperty("spring.cloud.loadbalancer.ribbon.enabled", havingValue = "false")
        class RibbonNotEnabled

        @ConditionalOnMissingClass("org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient")
        class RibbonLoadBalancerNotPresent
    }
}