package com.mlacker.samples.cloud.client.serviceregistry

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(AutoServiceRegistrationProperties::class)
@ConditionalOnProperty("spring.cloud.service-registry.auto-registration.enabled", matchIfMissing = true)
class AutoServiceRegistrationConfiguration