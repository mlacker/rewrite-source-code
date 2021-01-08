package com.mlacker.samples.beans.factory.support

import com.mlacker.samples.beans.factory.config.BeanDefinition
import com.mlacker.samples.beans.factory.config.BeanScope
import kotlin.reflect.KClass

abstract class AbstractBeanDefinition: BeanDefinition {

    override lateinit var beanClass: KClass<*>
    
    override var scope: BeanScope = BeanScope.Singleton
}
