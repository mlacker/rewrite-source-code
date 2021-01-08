package com.mlacker.samples.beans.factory.support

import com.mlacker.samples.beans.factory.ObjectFactory
import com.mlacker.samples.beans.factory.config.SingletonBeanRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

open class DefaultSingletonBeanRegistry : SingletonBeanRegistry {

    private val singletonObjects: MutableMap<String, Any> = ConcurrentHashMap(256)

    private val singletonFactories: MutableMap<String, ObjectFactory<*>> = mutableMapOf()

    private val earlySingletonObjects: MutableMap<String, Any> = mutableMapOf()

    protected val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun registerSingleton(beanName: String, singletonObject: Any) =
            addSingleton(beanName, singletonObject)

    override fun getSingleton(beanName: String): Any? =
            getSingleton(beanName, true)

    fun getSingleton(beanName: String, singletonFactory: ObjectFactory<*>): Any {
        var singletonObject = this.singletonObjects[beanName]

        if (singletonObject == null) {
            logger.debug("Creating shared instance of singleton bean '$beanName'")

            singletonObject = singletonFactory.getObject()

            addSingleton(beanName, singletonObject)
        }

        return singletonObject
    }

    protected fun addSingleton(beanName: String, singletonObject: Any) {
        this.singletonObjects[beanName] = singletonObject
        this.singletonFactories.remove(beanName)
        this.earlySingletonObjects.remove(beanName)
    }

    protected fun getSingleton(beanName: String, allowEarlyReference: Boolean): Any? {
        var singletonObject = this.singletonObjects[beanName]

        if (singletonObject == null && allowEarlyReference) {
            this.singletonFactories[beanName]?.also {
                singletonObject = it.getObject()
                this.earlySingletonObjects[beanName] = singletonObject!!
                this.singletonFactories.remove(beanName)
            }
        }

        return singletonObject
    }
}