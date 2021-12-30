package com.mlacker.samples.web.servlet

import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.AnnotationAwareOrderComparator
import org.springframework.web.util.NestedServletException
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class DispatcherServlet : FrameworkServlet() {

    private lateinit var handlerMappings: List<HandlerMapping>

    private lateinit var handlerAdapters: List<HandlerAdapter>

    // 494
    override fun onRefresh(context: ApplicationContext) {
        initStrategies(context)
    }

    // 502
    private fun initStrategies(context: ApplicationContext) {
        initHandlerMappings(context)
        initHandlerAdapters(context)
    }

    private fun initHandlerMappings(context: ApplicationContext) {
        this.handlerMappings = context.getBeansOfType(HandlerMapping::class.java).map { it.value }
        if (this.handlerMappings.isNotEmpty())
            AnnotationAwareOrderComparator.sort(this.handlerMappings)
    }

    private fun initHandlerAdapters(context: ApplicationContext) {
        this.handlerAdapters = context.getBeansOfType(HandlerAdapter::class.java).map { it.value }
        if (this.handlerAdapters.isNotEmpty())
            AnnotationAwareOrderComparator.sort(this.handlerAdapters)
    }

    // 909
    override fun doService(req: HttpServletRequest, resp: HttpServletResponse) {
        req.setAttribute("${DispatcherServlet::class.simpleName}.CONTEXT", webApplicationContext)

        doDispatch(req, resp)
    }

    // 1000
    private fun doDispatch(req: HttpServletRequest, resp: HttpServletResponse) {
        var mappedHandler: HandlerExecutionChain? = null

        var dispatchException: Exception? = null

        try {
            mappedHandler = getHandler(req)
            if (mappedHandler == null) {
                noHandlerFound(req, resp)
                return
            }

            val ha = getHandlerAdapter(mappedHandler.handler)

            if (!mappedHandler.applyPreHandle(req, resp)) {
                return
            }

            ha.handle(req, resp, mappedHandler.handler)

            mappedHandler.applyPostHandle(req, resp)
        } catch (ex: Exception) {
            dispatchException = ex
        } catch (err: Throwable) {
            dispatchException = NestedServletException("Handler dispatch failed", err)
        }
        mappedHandler?.triggerAfterCompletion(req, resp, dispatchException)

        if (dispatchException != null)
            throw dispatchException
    }

    // 1231
    private fun getHandler(req: HttpServletRequest): HandlerExecutionChain? {
        for (mapping in this.handlerMappings) {
            val handler = mapping.getHandler(req)
            if (handler != null) {
                return handler
            }
        }
        return null
    }

    // 1249
    private fun noHandlerFound(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND)
    }

    // 1267
    private fun getHandlerAdapter(handler: Any): HandlerAdapter {
        for (adapter in this.handlerAdapters) {
            if (adapter.supports(handler)) {
                return adapter
            }
        }
        throw ServletException()
    }
}