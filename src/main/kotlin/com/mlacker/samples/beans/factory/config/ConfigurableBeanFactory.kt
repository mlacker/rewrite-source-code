package com.mlacker.samples.beans.factory.config

import com.mlacker.samples.beans.factory.BeanFactory

interface ConfigurableBeanFactory : BeanFactory, SingletonBeanRegistry {

    fun addBeanPostProcessor(beanPostProcessor: BeanPostProcessor)
}