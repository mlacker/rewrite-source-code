package com.mlacker.samples.netflix.zuul.context

import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class RequestContext : ConcurrentHashMap<String, Any>() {

    companion object {
        private val threadLocal: ThreadLocal<out RequestContext> =
                object : ThreadLocal<RequestContext>() {
                    override fun initialValue() = RequestContext()
                }

        val currentContext: RequestContext = threadLocal.get()
    }

    var request: HttpServletRequest
        get() = get("request") as HttpServletRequest
        set(value) {
            put("request", value)
        }

    var response: HttpServletResponse
        get() = get("response") as HttpServletResponse
        set(value) {
            put("response", value)
        }

    var routeHost: URL?
        get() = get("routeHost") as URL?
        set(value) {
            put("routeHost", value!!)
        }

    fun sendZuulResponse(): Boolean =
            (get("sendZuulResponse") ?: true) as Boolean
}