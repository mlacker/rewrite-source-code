package com.mlacker.samples.beans.factory

import kotlin.reflect.KClass

interface BeanFactory {

    fun getBean(name: String): Any

    fun <T: Any> getBean(name: String, requiredType: KClass<T>): T

    fun <T: Any> getBean(requiredType: KClass<T>): T

    fun containsBean(name: String): Boolean

    fun isSingleton(name: String): Boolean

    fun isPrototype(name: String): Boolean

    fun getType(name: String): KClass<*>
}