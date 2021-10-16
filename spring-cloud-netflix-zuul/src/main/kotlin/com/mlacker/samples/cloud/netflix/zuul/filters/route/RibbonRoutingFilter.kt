package com.mlacker.samples.cloud.netflix.zuul.filters.route

import com.mlacker.samples.netflix.zuul.FilterType
import com.mlacker.samples.netflix.zuul.ZuulFilter
import com.mlacker.samples.netflix.zuul.context.RequestContext
import com.netflix.client.ClientException
import com.netflix.hystrix.exception.HystrixRuntimeException
import com.netflix.zuul.exception.ZuulException
import org.springframework.cloud.netflix.ribbon.support.RibbonCommandContext
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpResponse
import java.io.InputStream

class RibbonRoutingFilter(
        private val helper: ProxyRequestHelper,
        private val ribbonCommandFactory: RibbonCommandFactory<*>
) : ZuulFilter() {

    override val filterType: FilterType
        get() = FilterType.Route

    override val filterOrder: Int
        get() = RIBBON_ROUTING_FILTER_ORDER

    override val shouldFilter: Boolean
        get() = RequestContext.currentContext.let { ctx ->
            ctx.routeHost != null &&
                    ctx[SERVICE_ID_KEY] != null &&
                    ctx.sendZuulResponse()
        }

    override fun run(): Any {
        val context = RequestContext.currentContext
        try {
            val commandContext = buildCommandContext(context)
            val response = forward(commandContext)
            setResponse(response)
            return response
        } catch (ex: ZuulException) {
            throw ZuulRuntimeException(ex)
        }
    }

    private fun buildCommandContext(context: RequestContext): RibbonCommandContext {
        val headers = this.helper
                .buildZuulRequestHeaders(context.request)
        val params = this.helper
                .buildZuulRequestQueryParams(context.request)
        val verb = context.request.method ?: "GET"

        val requestEntity = RequestContext
                .currentContext[REQUEST_ENTITY_KEY] as InputStream?
                ?: context.request.inputStream

        val serviceId = context[SERVICE_ID_KEY] as String
        val retryable = context[RETRYABLE_KEY] as Boolean
        val loadBalanceKey = context[LOAD_BALANCER_KEY]
        val contentLength = context.request.contentLengthLong

        val uri = this.helper.buildZuulRequestURI(context.request)
                .replace("//", "/")

        return RibbonCommandContext(serviceId, verb, uri, retryable, headers, params,
                requestEntity, null, contentLength, loadBalanceKey)
    }

    private fun forward(context: RibbonCommandContext): ClientHttpResponse {
        val info = this.helper.debug(context.method,
                context.uri, context.headers, context.params,
                context.requestEntity)

        val command = this.ribbonCommandFactory.create(context)

        return try {
            val response = command.execute()
            this.helper.appendDebug(info, response.rawStatusCode,
                    response.headers)
            response
        } catch (ex: HystrixRuntimeException) {
            handleException(info, ex)
        }
    }

    private fun handleException(
            info: MutableMap<String, Any>, ex: HystrixRuntimeException
    ): ClientHttpResponse {
        var statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value()
        var message = ex.failureType.toString()
        var cause: Throwable = ex

        val clientException = findClientException(ex)
                ?: findClientException(ex.fallbackException)

        if (clientException != null) {
            if (clientException.errorType ==
                    ClientException.ErrorType.SERVER_THROTTLED) {
                statusCode = HttpStatus.SERVICE_UNAVAILABLE.value()
            }
            cause = clientException
            message = clientException.errorType.toString()
        }
        info["status"] = statusCode
        throw ZuulException(cause, "Forwarding error", statusCode, message)
    }

    private fun findClientException(t: Throwable?): ClientException? {
        if (t == null) {
            return null
        }
        if (t is ClientException) {
            return t
        }
        return findClientException(t.cause)
    }

    private fun setResponse(resp: ClientHttpResponse) {
        RequestContext.currentContext["zuulResponse"] = resp
        this.helper.setResponse(resp.rawStatusCode, resp.body,
                resp.headers)
    }
}