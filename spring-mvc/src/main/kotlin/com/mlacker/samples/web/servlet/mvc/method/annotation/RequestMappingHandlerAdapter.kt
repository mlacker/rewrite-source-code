package com.mlacker.samples.web.servlet.mvc.method.annotation

import com.mlacker.samples.web.servlet.mvc.method.AbstractHandlerMethodAdapter
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.core.ParameterNameDiscoverer
import org.springframework.http.converter.*
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter
import org.springframework.http.converter.xml.SourceHttpMessageConverter
import org.springframework.web.accept.ContentNegotiationManager
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.method.HandlerMethod
import org.springframework.web.method.annotation.*
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite
import org.springframework.web.method.support.HandlerMethodReturnValueHandler
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite
import org.springframework.web.servlet.mvc.method.annotation.*
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.xml.transform.Source

class RequestMappingHandlerAdapter : AbstractHandlerMethodAdapter(), ApplicationContextAware, InitializingBean {

    private lateinit var beanFactory: ConfigurableBeanFactory
    private lateinit var applicationContext: ApplicationContext
    private lateinit var argumentResolvers: HandlerMethodArgumentResolverComposite
    private lateinit var returnValueHandlers: HandlerMethodReturnValueHandlerComposite
    private val contentNegotiationManager = ContentNegotiationManager()
    private val messageConverters: MutableList<HttpMessageConverter<*>> = mutableListOf()
    private val requestResponseBodyAdvice = emptyList<Any>()
    private val parameterNameDiscoverer: ParameterNameDiscoverer = DefaultParameterNameDiscoverer()

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
        if (applicationContext is ConfigurableApplicationContext) {
            this.beanFactory = applicationContext.beanFactory
        }
    }

    override fun afterPropertiesSet() {
        addDefaultHttpMessageConverters(this.messageConverters)
        this.argumentResolvers = HandlerMethodArgumentResolverComposite().addResolvers(getDefaultArgumentResolvers())
        this.returnValueHandlers = HandlerMethodReturnValueHandlerComposite().addHandlers(getDefaultReturnValueHandlers())
    }

    protected fun addDefaultHttpMessageConverters(messageConverters: MutableList<HttpMessageConverter<*>>) {
        messageConverters.add(ByteArrayHttpMessageConverter())
        messageConverters.add(StringHttpMessageConverter())
        messageConverters.add(ResourceHttpMessageConverter())
        messageConverters.add(ResourceRegionHttpMessageConverter())
        try {
            messageConverters.add(SourceHttpMessageConverter<Source>())
        } catch (ex: Throwable) {
            // Ignore when no TransformerFactory implementation is available...
        }
        messageConverters.add(AllEncompassingFormHttpMessageConverter())
        messageConverters.add(MappingJackson2HttpMessageConverter(
                Jackson2ObjectMapperBuilder.json().applicationContext(applicationContext).build()))
    }

    private fun getDefaultArgumentResolvers(): List<HandlerMethodArgumentResolver> {
        val resolvers: MutableList<HandlerMethodArgumentResolver> = ArrayList(30)

        // Annotation-based argument resolution
        resolvers.add(RequestParamMethodArgumentResolver(beanFactory, false))
        resolvers.add(RequestParamMapMethodArgumentResolver())
        resolvers.add(PathVariableMethodArgumentResolver())
        resolvers.add(PathVariableMapMethodArgumentResolver())
        resolvers.add(MatrixVariableMethodArgumentResolver())
        resolvers.add(MatrixVariableMapMethodArgumentResolver())
        resolvers.add(ServletModelAttributeMethodProcessor(false))
        resolvers.add(RequestResponseBodyMethodProcessor(messageConverters, this.requestResponseBodyAdvice))
        resolvers.add(RequestPartMethodArgumentResolver(messageConverters, this.requestResponseBodyAdvice))
        resolvers.add(RequestHeaderMethodArgumentResolver(beanFactory))
        resolvers.add(RequestHeaderMapMethodArgumentResolver())
        resolvers.add(ServletCookieValueMethodArgumentResolver(beanFactory))
        resolvers.add(ExpressionValueMethodArgumentResolver(beanFactory))
        resolvers.add(SessionAttributeMethodArgumentResolver())
        resolvers.add(RequestAttributeMethodArgumentResolver())

        // Type-based argument resolution
        resolvers.add(ServletRequestMethodArgumentResolver())
        resolvers.add(ServletResponseMethodArgumentResolver())
        resolvers.add(HttpEntityMethodProcessor(messageConverters, this.requestResponseBodyAdvice))
        resolvers.add(RedirectAttributesMethodArgumentResolver())
        resolvers.add(ModelMethodProcessor())
        resolvers.add(MapMethodProcessor())
        resolvers.add(ErrorsMethodArgumentResolver())
        resolvers.add(SessionStatusMethodArgumentResolver())
        resolvers.add(UriComponentsBuilderMethodArgumentResolver())

        // Catch-all
        resolvers.add(RequestParamMethodArgumentResolver(beanFactory, true))
        resolvers.add(ServletModelAttributeMethodProcessor(true))
        return resolvers
    }

    private fun getDefaultReturnValueHandlers(): List<HandlerMethodReturnValueHandler> {
        val handlers: MutableList<HandlerMethodReturnValueHandler> = ArrayList(20)

        // Single-purpose return value types
        handlers.add(ModelAndViewMethodReturnValueHandler())
        handlers.add(ModelMethodProcessor())
        handlers.add(ViewMethodReturnValueHandler())
        handlers.add(StreamingResponseBodyReturnValueHandler())
        handlers.add(HttpEntityMethodProcessor(messageConverters,
                contentNegotiationManager, requestResponseBodyAdvice))
        handlers.add(HttpHeadersReturnValueHandler())
        handlers.add(CallableMethodReturnValueHandler())
        handlers.add(DeferredResultMethodReturnValueHandler())
        handlers.add(AsyncTaskMethodReturnValueHandler(beanFactory))

        // Annotation-based return value types
        handlers.add(ModelAttributeMethodProcessor(false))
        handlers.add(RequestResponseBodyMethodProcessor(messageConverters,
                contentNegotiationManager, requestResponseBodyAdvice))

        // Multi-purpose return value types
        handlers.add(ViewNameMethodReturnValueHandler())
        handlers.add(MapMethodProcessor())

        return handlers
    }

    override fun supportsInternal(handlerMethod: HandlerMethod): Boolean {
        return true
    }

    override fun handleInternal(req: HttpServletRequest, resp: HttpServletResponse, handlerMethod: HandlerMethod) {
        invokeHandlerMethod(req, resp, handlerMethod)
    }

    private fun invokeHandlerMethod(req: HttpServletRequest, resp: HttpServletResponse, handlerMethod: HandlerMethod) {
        val webRequest = ServletWebRequest(req, resp)
        try {
            val binderFactory = ServletRequestDataBinderFactory(emptyList(), null)
            val invocableMethod = ServletInvocableHandlerMethod(handlerMethod)
            invocableMethod.resolvers = argumentResolvers
            invocableMethod.returnValueHandlers = returnValueHandlers
            invocableMethod.dataBinderFactory = binderFactory
            invocableMethod.parameterNameDiscoverer = this.parameterNameDiscoverer

            invocableMethod.invokeAndHandle(webRequest)
        } finally {
            webRequest.requestCompleted()
        }
    }
}