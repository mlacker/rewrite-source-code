package com.mlacker.samples.beans.factory.support

import com.mlacker.samples.beans.factory.config.BeanDefinition
import kotlin.reflect.KClass

class RootBeanDefinition : AbstractBeanDefinition {
    constructor(original: BeanDefinition) : super(original)
    constructor(type: KClass<*>) {
        beanClass = type
    }

    @Volatile
    internal var beforeInstantiationResolved: Boolean? = null

    @Volatile
    internal var stale: Boolean = false

    override fun cloneBeanDefinition(): RootBeanDefinition {
        TODO("Not yet implemented")
    }
}

class GenericBeanDefinition : AbstractBeanDefinition() {

    override fun cloneBeanDefinition(): AbstractBeanDefinition {
        TODO("Not yet implemented")
    }
}