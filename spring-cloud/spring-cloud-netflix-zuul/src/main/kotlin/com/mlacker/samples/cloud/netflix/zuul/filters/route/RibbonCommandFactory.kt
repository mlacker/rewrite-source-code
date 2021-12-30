package com.mlacker.samples.cloud.netflix.zuul.filters.route

import org.springframework.cloud.netflix.ribbon.support.RibbonCommandContext

interface RibbonCommandFactory<T: RibbonCommand> {

    fun create(context: RibbonCommandContext): T
}