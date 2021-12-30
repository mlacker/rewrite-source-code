package com.mlacker.samples.beans.factory.config

import kotlin.reflect.KClass

interface BeanDefinition {

    var beanClass: KClass<*>

    var scope: BeanScope
}

enum class BeanScope {
    Singleton, Prototype
}