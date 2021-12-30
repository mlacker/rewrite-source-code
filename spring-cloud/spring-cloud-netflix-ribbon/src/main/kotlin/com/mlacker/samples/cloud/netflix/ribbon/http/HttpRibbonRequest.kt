package com.mlacker.samples.cloud.netflix.ribbon.http

import org.springframework.cloud.netflix.ribbon.RibbonProperties
import org.springframework.cloud.netflix.ribbon.support.ContextAwareRequest
import org.springframework.cloud.netflix.ribbon.support.RibbonCommandContext
import org.springframework.web.util.UriUtils
import java.net.URI
import java.net.http.HttpRequest
import java.time.Duration

class HttpRibbonRequest(context: RibbonCommandContext) : ContextAwareRequest(context) {

    fun toRequest(ribbon: RibbonProperties, readTimeout: Int): HttpRequest {
        val uri = URI.create(context.uri + UriUtils.encodeQueryParams(context.params))
        val method = (context.method ?: "GET").toUpperCase()
        val bodyPublisher = HttpRequest.BodyPublishers.ofInputStream { context.requestEntity }
        val timeout = Duration.ofMillis(ribbon.readTimeout(readTimeout).toLong())

        val builder = HttpRequest.newBuilder(uri)
            .method(method, bodyPublisher)
            .timeout(timeout)


        context.headers.forEach { (name, values) ->
            values.forEach { builder.header(name, it) }
        }

        return builder.build()
    }
}