package com.mlacker.samples.beans.factory.config

import kotlin.reflect.KClass

interface InstantiationAwareBeanPostProcessor: BeanPostProcessor {

    fun postProcessBeforeInstantiation(beanClass: KClass<*>, beanName: String): Any? = null

    fun postProcessAfterInstantiation(bean: Any, beanName: String): Boolean = true
}