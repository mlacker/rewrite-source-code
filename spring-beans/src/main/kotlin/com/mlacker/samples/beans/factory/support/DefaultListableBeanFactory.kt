package com.mlacker.samples.beans.factory.support

import com.mlacker.samples.beans.factory.config.BeanDefinition
import com.mlacker.samples.beans.factory.config.BeanScope
import com.mlacker.samples.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

class DefaultListableBeanFactory : AbstractAutowireCapableBeanFactory(),
        ConfigurableListableBeanFactory, BeanDefinitionRegistry {

    private val beanDefinitionMap: MutableMap<String, BeanDefinition> = ConcurrentHashMap(256)

    override fun registerBeanDefinition(beanName: String, beanDefinition: BeanDefinition) {
        beanDefinitionMap[beanName] = beanDefinition
    }

    override fun removeBeanDefinition(beanName: String) {
        beanDefinitionMap.remove(beanName)
    }

    override fun getBeanDefinition(beanName: String): BeanDefinition {
        return beanDefinitionMap[beanName] ?: throw NoSuchBeanDefinitionException(beanName)
    }

    override fun preInstantiateSingletons() {
        for (entry in beanDefinitionMap) {
            if (entry.value.scope == BeanScope.Singleton) {
                getBean<Any>(entry.key)
            }
        }
    }

    override fun <T : Any> getBean(requiredType: KClass<T>): T {
        for (entry in beanDefinitionMap) {
            if (requiredType.isSuperclassOf(entry.value.beanClass)) {
                return getBean(entry.key)
            }
        }

        throw NoSuchBeanDefinitionException(requiredType.java)
    }
}