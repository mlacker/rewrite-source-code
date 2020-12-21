package com.mlacker.samples.beans.factory.support

import org.springframework.beans.BeanUtils
import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.BeansException
import org.springframework.beans.factory.*
import org.springframework.beans.factory.config.DependencyDescriptor
import org.springframework.core.CollectionFactory
import org.springframework.core.MethodParameter
import org.springframework.util.Assert
import org.springframework.util.ClassUtils
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.kotlinFunction

class ConstructorResolver(val beanFactory: AbstractAutowireCapableBeanFactory) {

    private val logger = beanFactory.logger

    fun autowireConstructor(beanName: String, mbd: RootBeanDefinition): BeanWrapper {
        val bw = BeanWrapperImpl()

        val constructorToUse: KFunction<*> = mbd.beanClass!!.primaryConstructor!!
        val argsToUse: Array<Any>

        if (constructorToUse.parameters.isEmpty()) {
            bw.setBeanInstance(instantiate(beanName, mbd, constructorToUse, EMPTY_ARGS))
            return bw
        }

        val autowiring = true

        val paramTypes = constructorToUse.parameters.map { it.type }
        try {
            val paramNames = this.beanFactory.parameterNameDiscoverer
                    .getParameterNames(constructorToUse.javaConstructor as Constructor<*>)
            val argsHolder = createArgumentArray(beanName, mbd, null, bw, paramTypes, paramNames,
                    getUserDeclaredConstructor(constructorToUse), autowiring, true)

            argsToUse = argsHolder.arguments as Array<Any>
        } catch (ex: UnsatisfiedDependencyException) {
            throw BeanCreationException(beanName, "Cloud not resolve matching constructor", ex)
        }

        Assert.state(argsToUse != null, "Unresolved constructor arguments")
        bw.setBeanInstance(instantiate(beanName, mbd, constructorToUse, argsToUse))
        return bw
    }

    private fun createArgumentArray(
            beanName: String, mbd: RootBeanDefinition, resolvedValues: Any?,
            bw: BeanWrapperImpl, paramTypes: List<KType>, paramNames: Array<String>?, executable: KFunction<*>,
            autowiring: Boolean, fallback: Boolean): ArgumentsHolder {
        val args = ArgumentsHolder(paramTypes.size)
        val autowiredBeanNames = LinkedHashSet<String>(4)

        for (paramIndex in 0..paramTypes.size) {
            val paramType = paramTypes[paramIndex]
            val paramName = paramNames?.get(paramIndex)

            val methodParam = MethodParameter.forExecutable(executable as Executable, paramIndex)

            try {
                val autowiredArgument = resolveAutowiredArgument(
                        methodParam, beanName, autowiredBeanNames, fallback)
                args.arguments[paramIndex] = autowiredArgument
                args.rawArguments[paramIndex] = autowiredArgument
                args.preparedArguments[paramIndex] = autowiredArgument
                args.resolveNecessary = true
            } catch (ex: BeansException) {
                throw UnsatisfiedDependencyException(
                        null, beanName, InjectionPoint(methodParam), ex)
            }
        }

        for (autowiredBeanName in autowiredBeanNames) {
            this.beanFactory.registerDependentBean(autowiredBeanName, beanName)
            logger.debug("Autowiring by type from bean name '$beanName' " +
                    "via constructor to bean named '$autowiredBeanName'")
        }

        return args
    }

    private fun resolveAutowiredArgument(
            param: MethodParameter, beanName: String,
            autowiredBeanNames: MutableSet<String>, fallback: Boolean): Any? {
        val paramType = param.parameterType
        try {
            return this.beanFactory.resolveDependency(
                    DependencyDescriptor(param, true), beanName, autowiredBeanNames)
        } catch (ex: NoSuchBeanDefinitionException) {
            if (fallback && ex !is NoUniqueBeanDefinitionException) {
                if (paramType.isArray) {
                    return java.lang.reflect.Array.newInstance(paramType.componentType, 0)
                } else if (CollectionFactory.isApproximableCollectionType(paramType)) {
                    return CollectionFactory.createCollection<Any>(paramType, 0)
                } else if (CollectionFactory.isApproximableMapType(paramType)) {
                    return CollectionFactory.createMap<Any, Any>(paramType, 0)
                }
            }
            throw ex
        }
    }

    private fun instantiate(beanName: String, mbd: RootBeanDefinition, constructorToUse: KFunction<*>, argsToUse: Array<Any>): Any {
        try {
            return BeanUtils.instantiateClass(constructorToUse.javaConstructor!!, argsToUse)
        } catch (ex: Throwable) {
            throw BeanCreationException(beanName, "Bean instantiation via constructor failed", ex)
        }
    }

    protected fun getUserDeclaredConstructor(constructor: KFunction<*>): KFunction<*> {
        val javaConstructor = constructor.javaConstructor!!
        val declaringClass = javaConstructor.declaringClass
        val userClass = ClassUtils.getUserClass(declaringClass)

        if (userClass != declaringClass) {
            return userClass.getDeclaredConstructor(*javaConstructor.parameterTypes)
                    .kotlinFunction as KFunction<*>
        }
        return constructor
    }

    companion object {
        private val EMPTY_ARGS: Array<Any> = emptyArray()

        private class ArgumentsHolder(size: Int) {

            val arguments = arrayOfNulls<Any>(size)
            val rawArguments = arrayOfNulls<Any>(size)
            val preparedArguments = arrayOfNulls<Any>(size)
            var resolveNecessary: Boolean = false
        }
    }
}