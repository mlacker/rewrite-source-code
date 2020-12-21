package com.mlacker.samples.beans.factory.support

import com.mlacker.samples.beans.factory.config.BeanDefinition

class RootBeanDefinition constructor() : AbstractBeanDefinition() {

    constructor(original: BeanDefinition) : this()

    constructor(original: RootBeanDefinition) : this()

    @Volatile
    internal var beforeInstantiationResolved: Boolean? = null

    @Volatile
    internal var stale: Boolean = false

    override fun cloneBeanDefinition(): RootBeanDefinition = RootBeanDefinition(this)
}