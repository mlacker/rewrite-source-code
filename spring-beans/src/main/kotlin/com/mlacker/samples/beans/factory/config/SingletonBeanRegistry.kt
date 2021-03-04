package com.mlacker.samples.beans.factory.config

interface SingletonBeanRegistry {

    fun registerSingleton(beanName: String, singletonObject: Any)

    fun getSingleton(beanName: String): Any?
}