package com.mlacker.samples.beans.factory.config

import com.mlacker.samples.beans.factory.BeanFactory

interface ConfigurableBeanFactory : BeanFactory, SingletonBeanRegistry {

    companion object {
        const val SCOPE_SINGLETON: String = "singleton"
        const val SCOPE_PROTOTYPE: String = "prototype"
    }

    var beanClassLoader: ClassLoader?

    fun addBeanPostProcessor(beanPostProcessor: BeanPostProcessor)

    fun getBeanPostProcessorCount(): Int

    fun getMergedBeanDefinition(beanName: String): BeanDefinition

    fun setCurrentlyInCreation(beanName: String, inCreation: Boolean)

    fun isCurrentlyInCreation(beanName: String): Boolean

    fun getDependentBeans(beanName: String): Array<String>

    fun getDependenciesForBean(beanName: String): Array<String>

    fun destroyBean(beanName: String, beanInstance: Any)

    fun destroySingletons()
}