package com.mlacker.samples.cloud.client.discovery

import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Import(EnableDiscoveryClientImportSelector::class)
annotation class EnableDiscoveryClient(
    val autoRegister: Boolean = true
)
