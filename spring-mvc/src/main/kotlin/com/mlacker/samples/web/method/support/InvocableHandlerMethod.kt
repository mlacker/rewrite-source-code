package com.mlacker.samples.web.method.support

import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.core.ParameterNameDiscoverer
import org.springframework.util.ObjectUtils
import org.springframework.util.ReflectionUtils
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.HandlerMethod
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite
import org.springframework.web.servlet.mvc.method.annotation.ServletRequestDataBinderFactory
import java.lang.Error
import java.lang.Exception
import java.lang.RuntimeException
import java.lang.reflect.InvocationTargetException

open class InvocableHandlerMethod(handlerMethod: HandlerMethod) : HandlerMethod(handlerMethod) {

    companion object {
        private val EMPTY_ARGS = emptyArray<Any>()
    }

    var dataBinderFactory: ServletRequestDataBinderFactory? = null
    var resolvers = HandlerMethodArgumentResolverComposite()
    var parameterNameDiscoverer: ParameterNameDiscoverer = DefaultParameterNameDiscoverer()

    fun invokeForRequest(request: NativeWebRequest): Any? {
        val args = getMethodArgumentValues(request)

        return doInvoke(args)
    }

    private fun getMethodArgumentValues(request: NativeWebRequest): Array<Any> {
        if (ObjectUtils.isEmpty(methodParameters)) {
            return EMPTY_ARGS
        }
        val args = arrayOfNulls<Any>(methodParameters.size)
        methodParameters.forEachIndexed { i, parameter ->
            parameter.initParameterNameDiscovery(this.parameterNameDiscoverer)
            if (!this.resolvers.supportsParameter(parameter)) {
                throw java.lang.IllegalStateException(formatArgumentError(parameter, "No suitable resolver"))
            }
            args[i] = this.resolvers.resolveArgument(parameter, null, request, this.dataBinderFactory)
        }
        return args.requireNoNulls()
    }

    private fun doInvoke(args: Array<Any>): Any? {
        ReflectionUtils.makeAccessible(bridgedMethod)
        try {
            return bridgedMethod.invoke(bean, *args)
        } catch (ex: IllegalArgumentException) {
            assertTargetBean(bridgedMethod, bean, args)
            throw IllegalStateException(formatInvokeError(ex.message ?: "Illegal argument", args), ex)
        } catch (ex: InvocationTargetException) {
            when (ex.targetException) {
                is RuntimeException, is Error, is Exception -> throw ex.targetException
                else -> throw java.lang.IllegalStateException(formatInvokeError("Invocation failure", args), ex.targetException)
            }
        }
    }
}