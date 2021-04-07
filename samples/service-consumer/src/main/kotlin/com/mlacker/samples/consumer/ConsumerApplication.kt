package com.mlacker.samples.consumer

import org.springframework.boot.runApplication
import org.springframework.cloud.client.SpringCloudApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringCloudApplication
@EnableFeignClients("com.mlacker.samples.provider.api.client")
class ConsumerApplication

fun main(args: Array<String>) {
    runApplication<ConsumerApplication>(*args)
}