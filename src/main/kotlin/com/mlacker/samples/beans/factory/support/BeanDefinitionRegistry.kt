package com.mlacker.samples.beans.factory.support

import com.mlacker.samples.beans.factory.config.BeanDefinition

interface BeanDefinitionRegistry {

    fun registerBeanDefinition(beanName: String, beanDefinition: BeanDefinition)

    fun removeBeanDefinition(beanName: String)

    fun getBeanDefinition(beanName: String): BeanDefinition

    fun containsBeanDefinition(beanName: String): Boolean

    fun getBeanDefinitionNames(): Array<String>

    fun getBeanDefinitionCount(): Int

    fun isBeanNameInUse(beanName: String): Boolean
}
