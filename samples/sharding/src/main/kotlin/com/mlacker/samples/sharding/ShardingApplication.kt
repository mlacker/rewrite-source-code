package com.mlacker.samples.sharding

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ShardingApplication

fun main(args: Array<String>) {
    runApplication<ShardingApplication>(*args)
}