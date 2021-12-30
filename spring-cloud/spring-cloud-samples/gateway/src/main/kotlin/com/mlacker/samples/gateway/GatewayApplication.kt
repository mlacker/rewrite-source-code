package com.mlacker.samples.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.netflix.zuul.EnableZuulProxy
import org.springframework.retry.annotation.EnableRetry

@SpringBootApplication
@EnableDiscoveryClient
@EnableZuulProxy
@EnableRetry
class GatewayApplication

fun main(args: Array<String>) {
    runApplication<GatewayApplication>(*args)
}