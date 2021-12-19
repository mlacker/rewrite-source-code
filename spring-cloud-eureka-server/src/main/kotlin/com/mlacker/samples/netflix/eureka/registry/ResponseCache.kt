package com.mlacker.samples.netflix.eureka.registry

import com.netflix.eureka.registry.Key

interface ResponseCache {

    fun invalidate(appName: String, vipAddress: String?)

    fun get(key: Key): String?

    fun stop()
}