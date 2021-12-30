package com.mlacker.samples

import com.mlacker.samples.web.servlet.DispatcherServlet
import com.mlacker.samples.web.servlet.HandlerAdapter
import com.mlacker.samples.web.servlet.HandlerMapping
import com.mlacker.samples.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import com.mlacker.samples.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionOutcome
import org.springframework.boot.autoconfigure.condition.SpringBootCondition
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPath
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.format.support.FormattingConversionService
import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor
import org.springframework.web.servlet.resource.ResourceUrlProvider
import org.springframework.web.servlet.resource.ResourceUrlProviderExposingInterceptor

@SpringBootApplication
class MvcApplication {

    @Bean(DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
    @Conditional(DispatcherServletCondition::class)
    fun dispatcherServlet(): DispatcherServlet {
        return DispatcherServlet()
    }

    @Bean
    @Conditional(DispatcherServletCondition::class)
    fun getDispatcherServletPath(): DispatcherServletPath {
        return DispatcherServletPath { "/" }
    }

    @Bean
    @Conditional(DispatcherServletCondition::class)
    fun getContextRefreshListener(dispatcherServlet: DispatcherServlet) =
            dispatcherServlet.ContextRefreshListener()

    @Bean
    fun handlerMapping(
            @Qualifier("mvcConversionService") conversionService: FormattingConversionService,
            @Qualifier("mvcResourceUrlProvider") resourceUrlProvider: ResourceUrlProvider
    ): HandlerMapping {
        return RequestMappingHandlerMapping().apply {
            this.interceptors.add(ConversionServiceExposingInterceptor(conversionService))
            this.interceptors.add(ResourceUrlProviderExposingInterceptor(resourceUrlProvider))
        }
    }

    @Bean
    fun handlerAdapter(): HandlerAdapter {
        return RequestMappingHandlerAdapter()
    }

    class DispatcherServletCondition : SpringBootCondition() {
        private val enabled = true

        override fun getMatchOutcome(context: ConditionContext, metadata: AnnotatedTypeMetadata): ConditionOutcome {
            return if (enabled)
                ConditionOutcome.match()
            else
                ConditionOutcome.noMatch("Disabled")
        }
    }
}

fun main(vararg args: String) {
    runApplication<MvcApplication>(*args)
}