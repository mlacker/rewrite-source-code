package com.mlacker.samples.beans.factory.support

import com.mlacker.samples.beans.factory.ObjectFactory
import com.mlacker.samples.beans.factory.config.BeanDefinition
import com.mlacker.samples.beans.factory.config.BeanPostProcessor
import com.mlacker.samples.beans.factory.config.BeanScope
import com.mlacker.samples.beans.factory.config.ConfigurableBeanFactory
import org.springframework.beans.factory.BeanNotOfRequiredTypeException
import org.springframework.util.ClassUtils
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

abstract class AbstractBeanFactory : DefaultSingletonBeanRegistry(), ConfigurableBeanFactory {

    private val beanClassLoader: ClassLoader = ClassUtils.getDefaultClassLoader()!!

    protected val beanPostProcessors: MutableList<BeanPostProcessor> = CopyOnWriteArrayList()

    override fun <T : Any> getBean(name: String, requiredType: KClass<T>?): T {
        return doGetBean(name, requiredType)
    }

    private fun <T : Any> doGetBean(name: String, requiredType: KClass<T>?): T {
        val beanName = name
        val bean: Any

        val sharedInstance = getSingleton(beanName)
        if (sharedInstance != null) {
            logger.trace("Returning cached instance of singleton bean '$beanName'")
            bean = sharedInstance
        } else {
            val bd = getBeanDefinition(beanName)

            bean = when (bd.scope) {
                BeanScope.Singleton ->
                    getSingleton(beanName, object : ObjectFactory<Any> {
                        override fun getObject(): Any {
                            return createBean(beanName, bd)
                        }
                    })
                BeanScope.Prototype -> createBean(beanName, bd)
            }
        }

        if (requiredType != null && !requiredType.isInstance(bean)) {
            throw BeanNotOfRequiredTypeException(name, requiredType.java, bean.javaClass)
        }

        @Suppress("UNCHECKED_CAST")
        return bean as T
    }

    override fun addBeanPostProcessor(beanPostProcessor: BeanPostProcessor) {
        this.beanPostProcessors.remove(beanPostProcessor)
        // Add to end of list
        this.beanPostProcessors.add(beanPostProcessor)
    }

    protected abstract fun getBeanDefinition(beanName: String): BeanDefinition

    protected abstract fun createBean(beanName: String, bd: BeanDefinition): Any
}