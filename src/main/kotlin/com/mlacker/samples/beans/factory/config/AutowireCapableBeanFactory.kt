package com.mlacker.samples.beans.factory.config

import com.mlacker.samples.beans.factory.BeanFactory

interface AutowireCapableBeanFactory : BeanFactory {

    fun initializeBean(existingBean: Any, beanName: String): Any

    fun applyBeanPostProcessorsBeforeInitialization(existingBean: Any, beanName: String): Any

    fun applyBeanPostProcessorsAfterInitialization(existingBean: Any, beanName: String): Any

    fun destroyBean(existingBean: Any)
}