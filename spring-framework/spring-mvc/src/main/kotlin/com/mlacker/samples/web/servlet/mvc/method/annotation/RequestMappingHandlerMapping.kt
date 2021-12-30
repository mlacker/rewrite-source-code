package com.mlacker.samples.web.servlet.mvc.method.annotation

import com.mlacker.samples.web.servlet.mvc.method.RequestMappingInfoHandlerMapping
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.stereotype.Controller
import org.springframework.web.accept.ContentNegotiationManager
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method

class RequestMappingHandlerMapping : RequestMappingInfoHandlerMapping() {

    private val config = RequestMappingInfo.BuilderConfiguration()
    private val contentNegotiationManager = ContentNegotiationManager()

    // 180
    override fun afterPropertiesSet() {
        this.config.also {
            it.urlPathHelper = urlPathHelper
            it.pathMatcher = pathMatcher
            it.setTrailingSlashMatch(true)
            it.setContentNegotiationManager(contentNegotiationManager)
        }

        super.afterPropertiesSet()
    }

    // 239
    override fun isHandler(beanType: Class<*>): Boolean =
            (AnnotatedElementUtils.hasAnnotation(beanType, Controller::class.java) ||
                    AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping::class.java))

    //254
    override fun getMappingForMethod(method: Method, handlerType: Class<*>): RequestMappingInfo? {
        val info = createRequestMappingInfo(method) ?: return null

        return createRequestMappingInfo(handlerType)?.combine(info)!!
    }

    // 336
    private fun createRequestMappingInfo(element: AnnotatedElement): RequestMappingInfo? {
        val requestMapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping::class.java)
                ?: return null
        return RequestMappingInfo
                .paths(*requestMapping.path)
                .methods(*requestMapping.method)
                .params(*requestMapping.params)
                .headers(*requestMapping.headers)
                .consumes(*requestMapping.consumes)
                .produces(*requestMapping.produces)
                .mappingName(requestMapping.name)
                .options(this.config)
                .build()
    }
}