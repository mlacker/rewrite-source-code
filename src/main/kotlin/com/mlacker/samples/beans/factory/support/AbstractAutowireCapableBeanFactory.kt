package com.mlacker.samples.beans.factory.support

import com.mlacker.samples.beans.NoSuchBeanException
import com.mlacker.samples.beans.factory.BeanFactoryAware
import com.mlacker.samples.beans.factory.ObjectFactory
import com.mlacker.samples.beans.factory.config.AutowireCapableBeanFactory
import com.mlacker.samples.beans.factory.config.InstantiationAwareBeanPostProcessor
import com.mlacker.samples.beans.factory.config.SmartInstantiationAwareBeanPostProcessor
import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.factory.*
import org.springframework.beans.factory.support.AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.core.ParameterNameDiscoverer
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.primaryConstructor

abstract class AbstractAutowireCapableBeanFactory : AbstractBeanFactory(), AutowireCapableBeanFactory {

    val parameterNameDiscoverer: ParameterNameDiscoverer = DefaultParameterNameDiscoverer()

    val allowCircularReferences: Boolean = true

    override fun initializeBean(existingBean: Any, beanName: String) = initializeBean(beanName, existingBean, null)

    // 410
    override fun applyBeanPostProcessorsBeforeInitialization(existingBean: Any, beanName: String): Any {
        var result = existingBean
        for (processor in beanPostProcessors) {
            result = processor.postProcessBeforeInitialization(result, beanName) ?: return result
        }
        return result
    }

    // 425
    override fun applyBeanPostProcessorsAfterInitialization(existingBean: Any, beanName: String): Any {
        var result = existingBean
        for (processor in beanPostProcessors) {
            result = processor.postProcessAfterInitialization(result, beanName) ?: return result
        }
        return result
    }

    override fun destroyBean(existingBean: Any) {
        TODO("Not yet implemented")
    }

    override fun createBean(beanName: String, mbd: RootBeanDefinition): Any {
        logger.trace("Creating instance of bean '$beanName'")

        try {
            val bean = resolveBeforeInstantiation(beanName, mbd)
            if (bean != null) {
                return bean
            }
        } catch (ex: Throwable) {
            throw BeanCreationException(beanName, "BeanPostProcessor before instantiation of bean failed", ex)
        }

        try {
            val beanInstance = doCreateBean(beanName, mbd)
            logger.trace("Finished creating instance of bean '$beanName'")
            return beanInstance
        } catch (ex: BeanCreationException) {
            throw ex
        } catch (ex: Throwable) {
            throw BeanCreationException(beanName, "Unexpected exception during bean creation", ex)
        }
    }

    // 547
    protected fun doCreateBean(beanName: String, mbd: RootBeanDefinition): Any {
        val instanceWrapper: BeanWrapper = createBeanInstance(beanName, mbd)
        val bean = instanceWrapper.wrappedInstance

        val earlySingletonExposure = (mbd.isSingleton && this.allowCircularReferences && isSingletonCurrentlyInCreation(beanName))
        if (earlySingletonExposure) {
            logger.trace("Eagerly caching bean '$beanName' " +
                    "to allow for resolving potential circular references")
            addSingletonFactory(beanName, object : ObjectFactory<Any> {
                override fun getObject(): Any = bean
            })
        }

        var exposedObject = bean
        try {
            populateBean(beanName, mbd, instanceWrapper)
            exposedObject = initializeBean(beanName, exposedObject, mbd)
        } catch (ex: Throwable) {
            if (ex is BeanCreationException && beanName == ex.beanName) {
                throw ex
            } else {
                throw BeanCreationException(beanName, "Initialization of bean failed", ex)
            }
        }

        if (earlySingletonExposure) {
            try {
                val earlySingletonReference = getSingleton(beanName, false)
                if (exposedObject == bean) {
                    exposedObject = earlySingletonReference
                }
            } catch (ex: NoSuchBeanException) {
            }
        }

        return exposedObject
    }

    // 1106
    protected fun resolveBeforeInstantiation(beanName: String, mbd: RootBeanDefinition): Any? {
        var bean: Any? = null
        if (mbd.beforeInstantiationResolved != false) {
            if (hasInstantiationAwareBeanPostProcessors) {
                bean = applyBeanPostProcessorsBeforeInstantiation(mbd.beanClass!!, beanName)
                if (bean != null) {
                    bean = applyBeanPostProcessorsAfterInitialization(bean, beanName)
                }
            }
            mbd.beforeInstantiationResolved = bean != null
        }
        return bean
    }

    // 1136
    private fun applyBeanPostProcessorsBeforeInstantiation(beanClass: KClass<*>, beanName: String): Any? {
        for (bp in beanPostProcessors) {
            if (bp is InstantiationAwareBeanPostProcessor) {
                val result = bp.postProcessBeforeInstantiation(beanClass, beanName)
                if (result != null) {
                    return result
                }
            }
        }
        return null
    }

    // 1161
    protected fun createBeanInstance(beanName: String, mbd: RootBeanDefinition): BeanWrapper {
        val beanClass = mbd.beanClass

        if (beanClass != null && beanClass.visibility != KVisibility.PUBLIC)
            throw BeanCreationException(beanName,
                    "Bean class isn't public, and non-public access not allowed: ${beanClass.simpleName}")

        val ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName)
        if (ctors != null || mbd.autowireMode == AUTOWIRE_CONSTRUCTOR) {
            return autowireConstructor(beanName, mbd, ctors)
        }

        return instantiateBean(beanName, mbd)
    }

    // 1278
    protected fun determineConstructorsFromBeanPostProcessors(beanClass: KClass<*>?, beanName: String)
            : Collection<KFunction<*>>? {
        if (hasInstantiationAwareBeanPostProcessors) {
            for (bp in beanPostProcessors) {
                if (bp is SmartInstantiationAwareBeanPostProcessor) {
                    val ctors = bp.determineCandidateConstructors(beanClass, beanName)
                    if (ctors != null) {
                        return ctors
                    }
                }
            }
        }
        // TODO: return null
        return beanClass?.primaryConstructor?.let { listOf(it) }
    }

    // 1301
    protected fun instantiateBean(beanName: String, mbd: RootBeanDefinition): BeanWrapper {
        try {
            val primaryConstructor = mbd.beanClass!!.constructors.first()
            val beanInstance: Any = primaryConstructor.call()
            return BeanWrapperImpl(beanInstance)
        } catch (ex: Throwable) {
            throw BeanCreationException(beanName, "Instantiation of bean failed", ex)
        }
    }

    // 1353
    protected fun autowireConstructor(beanName: String, mbd: RootBeanDefinition, ctors: Collection<KFunction<*>>?): BeanWrapper {
        return ConstructorResolver(this).autowireConstructor(beanName, mbd)
    }

    // 1367
    protected fun populateBean(beanName: String, mbd: RootBeanDefinition, bw: BeanWrapper) {
        if (hasInstantiationAwareBeanPostProcessors) {
            for (bp in beanPostProcessors) {
                if (bp is InstantiationAwareBeanPostProcessor) {
                    if (!bp.postProcessAfterInstantiation(bw.wrappedInstance, beanName)) {
                        return
                    }
                }
            }
        }

        // TODO: applyPropertyValues
    }

    // 1773
    protected fun initializeBean(beanName: String, bean: Any, mbd: RootBeanDefinition?): Any {
        invokeAwareMethods(beanName, bean)

        var wrappedBean = bean

        wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName)

        try {
            invokeInitMethods(beanName, wrappedBean, mbd)
        } catch (ex: Throwable) {
            throw BeanCreationException(beanName, "Invocation of init method failed", ex)
        }

        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName)

        return wrappedBean
    }

    // 1804
    private fun invokeAwareMethods(beanName: String, bean: Any) {
        if (bean is Aware) {
            if (bean is BeanNameAware) {
                bean.setBeanName(beanName)
            }
            if (bean is BeanClassLoaderAware && beanClassLoader != null) {
                bean.setBeanClassLoader(beanClassLoader!!)
            }
            if (bean is BeanFactoryAware) {
                bean.setBeanFactory(this)
            }
        }
    }

    // 1836
    private fun invokeInitMethods(beanName: String, bean: Any, mbd: RootBeanDefinition?) {
        if (bean is InitializingBean) {
            logger.trace("Invoking afterPropertiesSet() on bean with name '$beanName'")
            bean.afterPropertiesSet()
        }
    }
}