package com.mlacker.samples.cloud.netflix.eureka.server

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EurekaServerMarkerConfiguration {

    @Bean
    fun eurekaServerMarkerBean(): Marker = Marker()

    class Marker
}