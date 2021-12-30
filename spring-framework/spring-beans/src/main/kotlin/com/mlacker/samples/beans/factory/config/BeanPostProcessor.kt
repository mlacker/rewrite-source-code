package com.mlacker.samples.beans.factory.config

interface BeanPostProcessor {

    fun postProcessBeforeInitialization(bean: Any, beanName: String): Any? = bean

    fun postProcessAfterInitialzation(bean: Any, beanName: String): Any? = bean
}
