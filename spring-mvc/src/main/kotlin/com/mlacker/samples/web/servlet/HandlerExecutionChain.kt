package com.mlacker.samples.web.servlet

import org.slf4j.LoggerFactory
import org.springframework.web.servlet.HandlerInterceptor
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class HandlerExecutionChain(val handler: Any) {

    private val interceptors: MutableList<HandlerInterceptor> = mutableListOf()

    private val logger = LoggerFactory.getLogger(javaClass)
    private var interceptorIndex = -1

    fun addInterceptor(interceptor: HandlerInterceptor) = interceptors.add(interceptor)

    fun applyPreHandle(request: HttpServletRequest, response: HttpServletResponse): Boolean {
        interceptors.forEachIndexed { i, interceptor ->
            if (!interceptor.preHandle(request, response, this.handler)) {
                triggerAfterCompletion(request, response, null)
                return false
            }
            this.interceptorIndex = i
        }
        return true
    }

    fun applyPostHandle(request: HttpServletRequest, response: HttpServletResponse) {
        for (i in interceptors.size until 0) {
            interceptors[i].postHandle(request, response, this.handler, null)
        }
    }

    fun triggerAfterCompletion(request: HttpServletRequest, response: HttpServletResponse, ex: Exception?) {
        for (i in interceptors.size until 0) {
            try {
                interceptors[i].afterCompletion(request, response, this.handler, ex)
            } catch (ex2: Throwable) {
                logger.error("HandlerInterceptor.afterCompletion throw exception", ex2)
            }
        }
    }
}