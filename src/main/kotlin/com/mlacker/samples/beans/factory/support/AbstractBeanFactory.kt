package com.mlacker.samples.beans.factory.support

import com.mlacker.samples.beans.NoSuchBeanException
import com.mlacker.samples.beans.factory.ObjectFactory
import com.mlacker.samples.beans.factory.config.BeanDefinition
import com.mlacker.samples.beans.factory.config.BeanPostProcessor
import com.mlacker.samples.beans.factory.config.ConfigurableBeanFactory
import com.mlacker.samples.beans.factory.config.InstantiationAwareBeanPostProcessor
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

    @Volatile
    protected var hasInstantiationAwareBeanPostProcessors = false

    private val mergedBeanDefinitions: MutableMap<String, RootBeanDefinition> = ConcurrentHashMap(256)

    private val alreadyCreated: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap(256))

    private val prototypesCurrentlyInCreation: ThreadLocal<Any> =
            NamedThreadLocal("Prototype beans currently in creation")

    //---------------------------------------------------------------------
    // Implementation of BeanFactory interface
    //---------------------------------------------------------------------

    // 201
    override fun getBean(name: String): Any = doGetBean(name, null)

    // 206
    override fun <T : Any> getBean(name: String, requiredType: KClass<T>) = doGetBean(name, requiredType)

    // 242
    protected fun <T : Any> doGetBean(name: String, requiredType: KClass<T>?): T {
        var bean: Any

        var sharedInstance: Any
        try {
            sharedInstance = getSingleton(name)
            if (logger.isTraceEnabled) {
                if (isSingletonCurrentlyInCreation(name)) {
                    logger.trace("Returning eagerly cached instance of singleton bean '$name' " +
                            "that is not fully initialized yet - a consequence of a circular reference")
                } else {
                    logger.trace("Returning cached instance of singleton bean '$name'")
                }
            }
            bean = sharedInstance
        } catch (ex: NoSuchBeanException) {

            if (isPrototypeCurrentlyInCreation(name)) {
                throw BeanCurrentlyInCreationException(name)
            }

            markBeanAsCreated(name)

            try {
                val mbd = getMergedLocalBeanDefinition(name)

                when {
                    mbd.isSingleton -> {
                        sharedInstance = getSingleton(name, object : ObjectFactory<Any> {
                            override fun getObject(): Any {
                                try {
                                    return createBean(name, mbd)
                                } catch (ex: BeansException) {
                                    destroySingleton(name)
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

    // 406
    override fun containsBean(name: String) = containsSingleton(name) || containsBeanDefinition(name)

    // 417
    override fun isSingleton(name: String): Boolean {
        return try {
            getSingleton(name)
            true
        } catch (ex: NoSuchBeanException) {
            getMergedLocalBeanDefinition(name).isSingleton
        }
    }

    // 458
    override fun isPrototype(name: String) = getMergedLocalBeanDefinition(name).isPrototype

    // 664
    override fun getType(name: String): KClass<*> {
        TODO("Not yet implemented")
    }

    //---------------------------------------------------------------------
    // Implementation of HierarchicalBeanFactory interface
    //---------------------------------------------------------------------

    // 762
    fun containsLocalBean(name: String) = containsSingleton(name) || containsBeanDefinition(name)

    //---------------------------------------------------------------------
    // Implementation of ConfigurableBeanFactory interface
    //---------------------------------------------------------------------


    // 927
    override fun addBeanPostProcessor(beanPostProcessor: BeanPostProcessor) {
        this.beanPostProcessors.remove(beanPostProcessor)
        if (beanPostProcessor is InstantiationAwareBeanPostProcessor) {
            this.hasInstantiationAwareBeanPostProcessors = true
        }
        this.beanPostProcessors.add(beanPostProcessor)
    }

    // 943
    override fun getBeanPostProcessorCount() = this.beanPostProcessors.size

    // 1068
    override fun getMergedBeanDefinition(beanName: String): BeanDefinition =
            getMergedLocalBeanDefinition(beanName)

    // 1094
    override fun isActuallyInCreation(beanName: String): Boolean =
            isSingletonCurrentlyInCreation(beanName) || isPrototypeCurrentlyInCreation(beanName)

    // 1103
    protected fun isPrototypeCurrentlyInCreation(beanName: String): Boolean {
        val curVal = this.prototypesCurrentlyInCreation.get()
        return curVal != null &&
                (curVal == beanName || (curVal is Set<*> && curVal.contains(beanName)))
    }

    // 1155
    override fun destroyBean(beanName: String, beanInstance: Any) {
        destroyBean(beanName, beanInstance, getMergedLocalBeanDefinition(beanName))
    }

    // 1166
    protected fun destroyBean(beanName: String, bean: Any, mbd: RootBeanDefinition) {
        TODO("Not yet implemented")
    }

    //---------------------------------------------------------------------
    // Implementation methods
    //---------------------------------------------------------------------

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

    // 1775
    protected val hasBeanCreationStarted get() = this.alreadyCreated.isNotEmpty()

    // 1838
    fun isBeanNameInUse(beanName: String): Boolean = containsLocalBean(beanName) || hasDependentBean(beanName)

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