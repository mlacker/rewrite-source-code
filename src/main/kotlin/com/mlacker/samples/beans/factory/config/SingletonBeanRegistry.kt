package com.mlacker.samples.beans.factory.config

interface SingletonBeanRegistry {

    fun registerSingleton(beanName: String, singletonObject: Any)

    fun getSingleton(beanName: String): Any

    fun containsSingleton(beanName: String): Boolean

    fun getSingletonNames(): Array<String>

    fun getSingletonCount(): Int

    fun getSingletonMutex(): Any
}