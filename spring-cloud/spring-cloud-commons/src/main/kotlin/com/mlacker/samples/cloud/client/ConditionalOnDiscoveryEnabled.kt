package com.mlacker.samples.cloud.client

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@Target(AnnotationTarget.CLASS)
@Retention
@ConditionalOnProperty("spring.cloud.discovery.enabled", matchIfMissing = true)
annotation class ConditionalOnDiscoveryEnabled