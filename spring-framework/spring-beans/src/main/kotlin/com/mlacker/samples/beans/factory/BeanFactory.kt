package com.mlacker.samples.beans.factory

import kotlin.reflect.KClass

interface BeanFactory {

    fun <T : Any> getBean(name: String, requiredType: KClass<T>? = null): T

    fun <T : Any> getBean(requiredType: KClass<T>): T
}