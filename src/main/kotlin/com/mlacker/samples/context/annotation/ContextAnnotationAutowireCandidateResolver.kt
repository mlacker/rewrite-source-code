package com.mlacker.samples.context.annotation

import com.mlacker.samples.beans.factory.BeanFactory
import com.mlacker.samples.beans.factory.BeanFactoryAware
import com.mlacker.samples.beans.factory.support.DefaultListableBeanFactory
import org.springframework.aop.TargetSource
import org.springframework.aop.framework.ProxyFactory
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.config.DependencyDescriptor
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.AnnotationUtils

class ContextAnnotationAutowireCandidateResolver : BeanFactoryAware {

    protected lateinit var beanFactory: BeanFactory
        private set

    override fun setBeanFactory(beanFactory: BeanFactory) {
        this.beanFactory = beanFactory
    }

    fun getLazyResolutionProxyIfNecessary(descriptor: DependencyDescriptor, beanName: String): Any? {
        return if (isLazy(descriptor)) buildLazyResolutionProxy(descriptor, beanName) else null
    }

    private fun isLazy(descriptor: DependencyDescriptor): Boolean {
        for (ann in descriptor.annotations) {
            val lazy = AnnotationUtils.getAnnotation(ann, Lazy::class.java)
            if (lazy != null && lazy.value) {
                return true
            }
        }
        val methodParameter = descriptor.methodParameter
        if (methodParameter != null) {
            val method = methodParameter.method
            if (method == null || Nothing::class.java == method.returnType) {
                val lazy = AnnotationUtils.getAnnotation(methodParameter.annotatedElement, Lazy::class.java)
                if (lazy != null && lazy.value) {
                    return true
                }
            }
        }
        return false
    }

    protected fun buildLazyResolutionProxy(descriptor: DependencyDescriptor, beanName: String): Any {
        val beanFactory = this.beanFactory as DefaultListableBeanFactory
        val ts = object : TargetSource {
            override fun getTargetClass(): Class<*>? = descriptor.dependencyType

            override fun isStatic(): Boolean = false

            override fun getTarget(): Any? {
                return beanFactory.doResolveDependency(descriptor, beanName, null)
                        ?: return when (targetClass) {
                            Map::class.java -> emptyMap<Any, Any>()
                            List::class.java -> emptyList<Any>()
                            Set::class.java, Collection::class.java -> emptySet<Any>()
                            else -> throw NoSuchBeanDefinitionException(descriptor.resolvableType,
                                    "Optional dependency not present for lazy injection point")
                        }
            }

            override fun releaseTarget(target: Any) {
            }
        }
        val pf = ProxyFactory()
        pf.targetSource = ts
        if (descriptor.dependencyType.isInterface) {
            pf.addInterface(descriptor.dependencyType)
        }
        return pf.getProxy(beanFactory.beanClassLoader)
    }
}