package com.mlacker.samples.beans.factory.config

import com.mlacker.samples.beans.factory.BeanFactory
import org.springframework.core.ResolvableType
import kotlin.reflect.KClass

interface ListableBeanFactory : BeanFactory {

    fun containsBeanDefinition(beanName: String): Boolean

    fun getBeanDefinitionCount(): Int

    fun getBeanDefinitionNames(): Array<String>

    fun getBeanNamesForType(
            type: ResolvableType, includeNonSingletons: Boolean = true, allowEagerInit: Boolean = true): Array<String>

    fun getBeanNamesForType(
            type: KClass<*>, includeNonSingletons: Boolean = true, allowEagerInit: Boolean = true): Array<String>
}