 package com.mlacker.samples.beans.factory

import org.springframework.beans.factory.Aware

interface BeanFactoryAware : Aware {

    fun setBeanFactory(beanFactory: BeanFactory)
}