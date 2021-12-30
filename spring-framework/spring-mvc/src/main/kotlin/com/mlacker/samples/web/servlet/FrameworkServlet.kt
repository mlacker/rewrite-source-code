package com.mlacker.samples.web.servlet

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

abstract class FrameworkServlet : HttpServlet(), ApplicationContextAware {

    protected lateinit var webApplicationContext: WebApplicationContext

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        if (applicationContext is WebApplicationContext) {
            this.webApplicationContext = applicationContext
        }
    }

    override fun service(req: HttpServletRequest, resp: HttpServletResponse) =
            processRequest(req, resp)

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) =
            processRequest(req, resp)

    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) =
            processRequest(req, resp)

    override fun doPut(req: HttpServletRequest, resp: HttpServletResponse) =
            processRequest(req, resp)

    override fun doDelete(req: HttpServletRequest, resp: HttpServletResponse) =
            processRequest(req, resp)

    private fun processRequest(req: HttpServletRequest, resp: HttpServletResponse) {
        val previousAttributes = RequestContextHolder.getRequestAttributes()
        val requestAttributes = buildRequestAttributes(req, resp)

        initContextHolders(requestAttributes)

        try {
            doService(req, resp)
        } finally {
            resetContextHolders(previousAttributes)
            requestAttributes.requestCompleted()
        }
    }

    private fun buildRequestAttributes(req: HttpServletRequest, resp: HttpServletResponse) =
            ServletRequestAttributes(req, resp)

    private fun initContextHolders(requestAttributes: RequestAttributes) {
        RequestContextHolder.setRequestAttributes(requestAttributes)
    }

    private fun resetContextHolders(previousAttributes: RequestAttributes?) {
        RequestContextHolder.setRequestAttributes(previousAttributes)
    }

    protected abstract fun doService(req: HttpServletRequest, resp: HttpServletResponse)

    protected open fun onRefresh(context: ApplicationContext) {}

    inner class ContextRefreshListener : ApplicationListener<ContextRefreshedEvent> {

        override fun onApplicationEvent(event: ContextRefreshedEvent) {
            this@FrameworkServlet.onRefresh(event.applicationContext)
        }
    }
}