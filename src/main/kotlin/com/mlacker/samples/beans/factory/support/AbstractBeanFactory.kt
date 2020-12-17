package com.mlacker.samples.beans.factory.support

import com.mlacker.samples.beans.factory.ObjectFactory
import com.mlacker.samples.beans.factory.config.BeanDefinition
import com.mlacker.samples.beans.factory.config.BeanPostProcessor
import com.mlacker.samples.beans.factory.config.ConfigurableBeanFactory
import org.springframework.beans.BeansException
import org.springframework.beans.factory.BeanCurrentlyInCreationException
import org.springframework.beans.factory.BeanNotOfRequiredTypeException
import org.springframework.core.NamedThreadLocal
import org.springframework.util.ClassUtils
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

abstract class AbstractBeanFactory : DefaultSingletonBeanRegistry(), ConfigurableBeanFactory {

    override var beanClassLoader: ClassLoader? = ClassUtils.getDefaultClassLoader()

    val beanPostProcessors: MutableList<BeanPostProcessor> = CopyOnWriteArrayList()

    private val mergedBeanDefinitions: MutableMap<String, RootBeanDefinition> = ConcurrentHashMap(256)

    private val alreadyCreated: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap(256))

    private val prototypesCurrentlyInCreation: ThreadLocal<Any> =
            NamedThreadLocal("Prototype beans currently in creation")

    override fun getBean(name: String): Any = doGetBean(name, null, false)

    override fun <T : Any> getBean(name: String, requiredType: KClass<T>) = doGetBean(name, requiredType, false)

    protected fun <T : Any> doGetBean(name: String, requiredType: KClass<T>?, typeCheckOnly: Boolean): T {
        val bean: Any

        var sharedInstance = getSingleton(name)
        if (sharedInstance != null) {
            if (logger.isTraceEnabled) {
                if (isSingletonCurrentlyInCreation(name))
                    logger.trace("Returning eagerly cached instance of singleton bean '$name' " +
                            "that is not fully initialized yet - a consequence of a circular reference")
                else
                    logger.trace("Returning cached instance of singleton bean '$name'")
            }
            bean = sharedInstance
        } else {
            if (isPrototypeCurrentlyInCreation(name)) {
                throw BeanCurrentlyInCreationException(name)
            }

            if (!typeCheckOnly) {
                markBeanAsCreated(name)
            }

            try {
                val mbd = getMergedLocalBeanDefinition(name)

                when {
                    mbd.isSingleton -> {
                        sharedInstance = getSingleton(name, object : ObjectFactory<Any> {
                            override fun getObject(): Any {
                                try {
                                    return createBean(name, mbd)
                                } catch (ex: BeansException) {
                                    destroySingle(name)
                                    throw ex
                                }
                            }
                        })
                        bean = sharedInstance
                    }
                    mbd.isPrototype -> {
                        bean = createBean(name, mbd)
                    }
                    else -> {
                        throw IllegalStateException("No Scope registered for scope name '${mbd.scope}'")
                    }
                }
            } catch (ex: BeansException) {
                this.cleanupAfterBeanCreationFailure(name)
                throw ex
            }
        }

        if (requiredType != null && !requiredType.isInstance(bean)) {
            throw BeanNotOfRequiredTypeException(name, requiredType.java, bean.javaClass)
        }

        @Suppress("UNCHECKED_CAST")
        return bean as T
    }

    override fun containsBean(name: String) = containsSingleton(name) || containsBeanDefinition(name)

    override fun isSingleton(name: String): Boolean {
        return getSingleton(name) != null || getMergedLocalBeanDefinition(name).isSingleton
    }

    override fun isPrototype(name: String) = getMergedLocalBeanDefinition(name).isPrototype

    //---------------------------------------------------------------------
    // Implementation of ConfigurableBeanFactory interface
    //---------------------------------------------------------------------


    override fun addBeanPostProcessor(beanPostProcessor: BeanPostProcessor) {
        this.beanPostProcessors.remove(beanPostProcessor)
        this.beanPostProcessors.add(beanPostProcessor)
    }

    override fun getBeanPostProcessorCount() = this.beanPostProcessors.size

    // 1103
    protected fun isPrototypeCurrentlyInCreation(beanName: String): Boolean {
        val curVal = this.prototypesCurrentlyInCreation.get()
        return curVal != null &&
                (curVal == beanName || (curVal is Set<*> && curVal.contains(beanName)))
    }

    // 1282
    protected fun getMergedLocalBeanDefinition(beanName: String): RootBeanDefinition {
        val bd = getBeanDefinition(beanName)

        // Use copy of given root bean definition.
        return if (bd is RootBeanDefinition) {
            bd.cloneBeanDefinition()
        } else {
            RootBeanDefinition(bd)
        }
    }

    // 1431
    protected fun clearMergedBeanDefinition(beanName: String) {
        this.mergedBeanDefinitions[beanName]?.stale = true
    }

    // 1719
    protected fun markBeanAsCreated(beanName: String) {
        if (!this.alreadyCreated.contains(beanName)) {
            synchronized(this.mergedBeanDefinitions) {
                if (!this.alreadyCreated.contains(beanName)) {
                    clearMergedBeanDefinition(beanName)
                    this.alreadyCreated.add(beanName)
                }
            }
        }
    }

    // 1736
    protected fun cleanupAfterBeanCreationFailure(beanName: String) {
        synchronized(this.mergedBeanDefinitions) {
            this.alreadyCreated.remove(beanName)
        }
    }

    //---------------------------------------------------------------------
    // Abstract methods to be implemented by subclasses
    //---------------------------------------------------------------------

    // 1912
    protected abstract fun containsBeanDefinition(beanName: String): Boolean

    // 1933
    protected abstract fun getBeanDefinition(beanName: String): BeanDefinition

    // 1946
    protected abstract fun createBean(beanName: String, mbd: RootBeanDefinition): Any
}