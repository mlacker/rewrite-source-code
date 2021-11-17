package com.mlacker.samples.cloud.client.discovery

import org.springframework.cloud.commons.util.SpringFactoryImportSelector
import org.springframework.core.Ordered
import org.springframework.core.annotation.AnnotationAttributes
import org.springframework.core.annotation.Order
import org.springframework.core.type.AnnotationMetadata

@Order(Ordered.LOWEST_PRECEDENCE - 100)
class EnableDiscoveryClientImportSelector :
    SpringFactoryImportSelector<EnableDiscoveryClient>() {

    override fun selectImports(metadata: AnnotationMetadata): Array<String> {
        val imports = super.selectImports(metadata)

        val attributes = AnnotationAttributes.fromMap(
            metadata.getAnnotationAttributes(annotationClass.name, true))

        val autoRegister = attributes!!.getBoolean("autoRegister")

        if (autoRegister) {
            imports.plus(
                "com.mlacker.samples.cloud.client.serviceregistry.AutoServiceRegistrationConfiguration")
        }

        return imports
    }

    override fun isEnabled(): Boolean {
        return environment.getProperty("spring.cloud.discovery.enabled",
            Boolean::class.java, true)
    }

    override fun hasDefaultFactory(): Boolean {
        return true
    }
}
