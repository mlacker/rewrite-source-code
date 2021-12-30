package com.mlacker.samples.web.servlet.handler

import com.mlacker.samples.web.servlet.HandlerExecutionChain
import com.mlacker.samples.web.servlet.HandlerMapping
import org.springframework.context.ApplicationContext
import org.springframework.core.Ordered
import org.springframework.util.AntPathMatcher
import org.springframework.web.context.request.WebRequestInterceptor
import org.springframework.web.context.support.WebApplicationObjectSupport
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.handler.MappedInterceptor
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter
import org.springframework.web.util.UrlPathHelper
import javax.servlet.http.HttpServletRequest

abstract class AbstractHandlerMapping : WebApplicationObjectSupport(),
        HandlerMapping, Ordered {

    private var defaultHandler: Any? = null

    protected val urlPathHelper = UrlPathHelper()

    protected val pathMatcher = AntPathMatcher()

    val interceptors = mutableListOf<Any?>()

    private val adaptedInterceptors = mutableListOf<HandlerInterceptor>()

    override fun getOrder(): Int {
        return Ordered.LOWEST_PRECEDENCE
    }

    override fun initApplicationContext(context: ApplicationContext) {
        initInterceptors()
    }

    private fun initInterceptors() {
        this.interceptors.forEachIndexed { i, interceptor ->
            if (interceptor == null) {
                throw IllegalArgumentException("Entry number $i in interceptors array is null")
            }
            this.adaptedInterceptors.add(adaptInterceptor(interceptor))
        }
    }

    private fun adaptInterceptor(interceptor: Any): HandlerInterceptor {
        return when (interceptor) {
            is HandlerInterceptor -> {
                interceptor
            }
            is WebRequestInterceptor -> {
                WebRequestHandlerInterceptorAdapter(interceptor)
            }
            else -> {
                throw IllegalArgumentException("Interceptor type not supported: ${interceptor.javaClass.name}")
            }
        }
    }

    override fun getHandler(req: HttpServletRequest): HandlerExecutionChain? {
        var handler = getHandlerInternal(req) ?: defaultHandler ?: return null

        if (handler is String) {
            handler = obtainApplicationContext().getBean(handler)
        }

        return getHandlerExecutionChain(handler, req)
    }

    protected abstract fun getHandlerInternal(req: HttpServletRequest): Any?

    private fun getHandlerExecutionChain(handler: Any, req: HttpServletRequest): HandlerExecutionChain? {
        val chain = if (handler is HandlerExecutionChain) handler else HandlerExecutionChain(handler)

        val lookupPath = this.urlPathHelper.getLookupPathForRequest(req, HandlerMapping.LOOKUP_PATH)
        for (interceptor in this.adaptedInterceptors) {
            if (interceptor is MappedInterceptor) {
                if (interceptor.matches(lookupPath, this.pathMatcher)) {
                    chain.addInterceptor(interceptor.interceptor)
                } else {
                    chain.addInterceptor(interceptor)
                }
            }
        }
        return chain
    }
}