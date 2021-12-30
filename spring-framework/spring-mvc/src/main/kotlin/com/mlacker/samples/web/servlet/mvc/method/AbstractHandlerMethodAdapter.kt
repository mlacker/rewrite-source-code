package com.mlacker.samples.web.servlet.mvc.method

import com.mlacker.samples.web.servlet.HandlerAdapter
import org.springframework.core.Ordered
import org.springframework.web.method.HandlerMethod
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

abstract class AbstractHandlerMethodAdapter : HandlerAdapter, Ordered {

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE

    override fun supports(handler: Any): Boolean {
        return handler is HandlerMethod && supportsInternal(handler)
    }

    abstract fun supportsInternal(handlerMethod: HandlerMethod): Boolean

    override fun handle(req: HttpServletRequest, resp: HttpServletResponse, handler: Any) {
        handleInternal(req, resp, handler as HandlerMethod)
    }

    abstract fun handleInternal(req: HttpServletRequest, resp: HttpServletResponse, handlerMethod: HandlerMethod)
}