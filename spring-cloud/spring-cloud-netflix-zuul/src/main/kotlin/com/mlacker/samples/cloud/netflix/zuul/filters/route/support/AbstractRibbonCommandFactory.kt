package com.mlacker.samples.cloud.netflix.zuul.filters.route.support

import com.mlacker.samples.cloud.netflix.zuul.filters.route.RibbonCommand
import com.mlacker.samples.cloud.netflix.zuul.filters.route.RibbonCommandFactory
import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider

abstract class AbstractRibbonCommandFactory<T : RibbonCommand>(
        fallbackProviders: Set<FallbackProvider>
) : RibbonCommandFactory<T> {

    private val fallbackProviderCache: Map<String, FallbackProvider>
    private var defaultFallbackProvider: FallbackProvider? = null

    init {
        fallbackProviderCache = mutableMapOf()
        for (provider in fallbackProviders) {
            val route = provider.route
            if ("*".equals(route) || route == null) {
                defaultFallbackProvider = provider
            } else {
                fallbackProviderCache[route] = provider
            }
        }
    }

    protected fun getFallbackProvider(route: String): FallbackProvider? =
            fallbackProviderCache[route] ?: defaultFallbackProvider
}