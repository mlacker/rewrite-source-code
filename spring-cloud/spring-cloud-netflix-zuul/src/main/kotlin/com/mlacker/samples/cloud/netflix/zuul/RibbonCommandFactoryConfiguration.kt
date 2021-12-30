package com.mlacker.samples.cloud.netflix.zuul

import com.mlacker.samples.cloud.netflix.zuul.filters.route.http.HttpClientRibbonCommandFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.netflix.ribbon.SpringClientFactory
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties
import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase

class RibbonCommandFactoryConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnRibbonHttpClient
    class HttpClientRibbonConfiguration {

        @Autowired(required = false)
        private val zuulFallbackProviders: Set<FallbackProvider> = emptySet()

        @Bean
        @ConditionalOnMissingBean
        fun ribbonCommandFactory(clientFactory: SpringClientFactory, zuulProperties: ZuulProperties) =
            HttpClientRibbonCommandFactory(clientFactory, zuulProperties, zuulFallbackProviders)
    }

    @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
    @Retention
    @MustBeDocumented
    @Conditional(OnRibbonHttpClientCondition::class)
    private annotation class ConditionalOnRibbonHttpClient

    private class OnRibbonHttpClientCondition :
        AnyNestedCondition(ConfigurationPhase.PARSE_CONFIGURATION) {

        @ConditionalOnProperty(name = arrayOf("ribbon.httpclient.enabled"), matchIfMissing = true)
        class RibbonProperty
    }
}