package com.mlacker.samples.beans.factory.support

import com.mlacker.samples.beans.factory.config.AutowireCapableBeanFactory
import com.mlacker.samples.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.InitializingBean
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor

abstract class AbstractAutowireCapableBeanFactory : AbstractBeanFactory(), AutowireCapableBeanFactory {

    override fun initializeBean(existingBean: Any, beanName: String): Any {
        var bean = existingBean
        bean = applyBeanPostProcessorsBeforeInitialization(bean, beanName)
        if (bean is InitializingBean) {
            bean.afterPropertiesSet()
        }
        bean = applyBeanPostProcessorsAfterInitialization(bean, beanName)
        return bean
    }

    override fun applyBeanPostProcessorsBeforeInitialization(existingBean: Any, beanName: String): Any {
        var result: Any = existingBean
        for (processor in beanPostProcessors) {
            result = processor.postProcessBeforeInitialization(result, beanName) ?: return result
        }
        return result
    }

    override fun applyBeanPostProcessorsAfterInitialization(existingBean: Any, beanName: String): Any {
        var result: Any = existingBean
        for (processor in beanPostProcessors) {
            result = processor.postProcessAfterInitialzation(result, beanName) ?: return result
        }
        return result
    }

    override fun createBean(beanName: String, bd: BeanDefinition): Any {
        val bean = createBeanInstance(beanName, bd)

        val exposedObject = initializeBean(bean, beanName)

        return exposedObject
    }

    private fun createBeanInstance(beanName: String, bd: BeanDefinition): Any {
        val bean: Any
        val beanClass = bd.beanClass
        var constructor = beanClass.primaryConstructor

        if (constructor == null) {
            constructor = beanClass.constructors.firstOrNull() ?: throw BeanCreationException(beanName, "Cannot find any constructor")
        }

        val size = constructor.parameters.size
        if (size == 0) {
            bean = beanClass.createInstance()
        } else {
            val paramNames = constructor.parameters
                    .map { getBean<Any>(it.name!!) }
                    .toTypedArray()
            bean = constructor.call(*paramNames)
        }

        return bean
    }
}