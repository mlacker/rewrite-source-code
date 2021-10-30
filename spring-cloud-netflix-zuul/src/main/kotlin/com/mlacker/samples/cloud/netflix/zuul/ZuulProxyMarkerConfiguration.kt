package com.mlacker.samples.cloud.netflix.zuul

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class ZuulProxyMarkerConfiguration {

    @Bean
    fun zuulProxyMarkerBean(): Marker = Marker()

    class Marker
}