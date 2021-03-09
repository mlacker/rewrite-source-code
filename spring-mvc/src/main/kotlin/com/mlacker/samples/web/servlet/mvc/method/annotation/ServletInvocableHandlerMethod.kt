package com.mlacker.samples.web.servlet.mvc.method.annotation

import com.mlacker.samples.web.method.support.InvocableHandlerMethod
import org.springframework.util.Assert
import org.springframework.util.StringUtils
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.method.HandlerMethod
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite
import org.springframework.web.method.support.ModelAndViewContainer

class ServletInvocableHandlerMethod(handlerMethod: HandlerMethod) : InvocableHandlerMethod(handlerMethod) {

    private val mavContainer = ModelAndViewContainer()
    var returnValueHandlers: HandlerMethodReturnValueHandlerComposite? = null

    fun invokeAndHandle(webRequest: ServletWebRequest) {
        val returnValue = invokeForRequest(webRequest)
        setResponseStatus(webRequest)

        Assert.state(this.returnValueHandlers != null, "No return value handlers")

        this.returnValueHandlers!!.handleReturnValue(
                returnValue, getReturnValueType(returnValue), mavContainer, webRequest)
    }

    private fun setResponseStatus(webRequest: ServletWebRequest) {
        if (responseStatus == null) {
            return
        }

        val response = webRequest.response
        if (response != null) {
            if (StringUtils.hasText(responseStatusReason)) {
                response.sendError(responseStatus!!.value(), responseStatusReason)
            } else {
                response.status = responseStatus!!.value()
            }
        }
    }
}