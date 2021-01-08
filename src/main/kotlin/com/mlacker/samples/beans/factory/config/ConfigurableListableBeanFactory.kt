package com.mlacker.samples.beans.factory.config

import com.mlacker.samples.beans.factory.ListableBeanFactory

interface ConfigurableListableBeanFactory: ListableBeanFactory, AutowireCapableBeanFactory, ConfigurableBeanFactory {

    fun getBeanDefinition(beanName: String): BeanDefinition

    fun preInstantiateSingletons()
}