package com.mlacker.samples.cloud.netflix.zuul

import com.mlacker.samples.cloud.netflix.zuul.filters.route.RibbonCommandFactory
import com.mlacker.samples.cloud.netflix.zuul.filters.route.RibbonRoutingFilter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration(proxyBeanMethods = false)
@Import(RibbonCommandFactoryConfiguration.HttpClientRibbonConfiguration::class)
@ConditionalOnBean(ZuulProxyMarkerConfiguration.Marker::class)
class ZuulProxyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RibbonRoutingFilter::class)
    fun ribbonRoutingFilter(
        helper: ProxyRequestHelper,
        ribbonCommandFactory: RibbonCommandFactory<*>,
    ): RibbonRoutingFilter {
        return RibbonRoutingFilter(helper, ribbonCommandFactory)
    }
}