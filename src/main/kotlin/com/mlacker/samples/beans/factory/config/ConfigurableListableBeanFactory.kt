package com.mlacker.samples.beans.factory.config

import org.springframework.beans.factory.config.DependencyDescriptor
import kotlin.reflect.KClass

interface ConfigurableListableBeanFactory:
        ListableBeanFactory, AutowireCapableBeanFactory, ConfigurableBeanFactory {

    fun registerResolvableDependency(dependencyType: KClass<*>, autowiredValue: Any)

    fun isAutowireCandidate(beanName: String, descriptor: DependencyDescriptor): Boolean

    fun getBeanDefinition(beanName: String): BeanDefinition

    fun freezeConfiguration()

    val isConfigurationFrozen: Boolean

    fun preInstantiateSingletons()
}