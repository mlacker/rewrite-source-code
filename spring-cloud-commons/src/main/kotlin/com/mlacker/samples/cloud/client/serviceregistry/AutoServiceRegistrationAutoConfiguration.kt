package com.mlacker.samples.cloud.client.serviceregistry

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.lang.IllegalStateException
import javax.annotation.PostConstruct

@Configuration
@Import(AutoServiceRegistrationConfiguration::class)
@ConditionalOnProperty("spring.cloud.service-registry.auto-registration.enable", matchIfMissing = true)
class AutoServiceRegistrationAutoConfiguration(
    @Autowired(required = false)
    private val autoServiceRegistration: AutoServiceRegistration?,
    private val properties: AutoServiceRegistrationProperties,
) {

    @PostConstruct
    private fun init() {
        if (this.autoServiceRegistration == null && this.properties.isFailFast) {
            throw IllegalStateException("Auto Service Registration has " +
                    "been requested, but there is no AutoServiceRegistration bean")
        }
    }
}