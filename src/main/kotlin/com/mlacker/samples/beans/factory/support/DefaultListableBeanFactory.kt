package com.mlacker.samples.beans.factory.support

import com.mlacker.samples.beans.NoSuchBeanException
import com.mlacker.samples.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.BeanNotOfRequiredTypeException
import org.springframework.beans.factory.config.DependencyDescriptor
import org.springframework.beans.factory.support.AutowireCandidateResolver
import org.springframework.beans.factory.support.SimpleAutowireCandidateResolver
import org.springframework.core.ResolvableType
import org.springframework.util.ClassUtils
import kotlin.reflect.KClass

class DefaultListableBeanFactory : AbstractAutowireCapableBeanFactory() {

    val autowireCandidateResolver: AutowireCandidateResolver = SimpleAutowireCandidateResolver.INSTANCE

    override fun <T : Any> getBean(requiredType: KClass<T>): T =
            resolveBean(ResolvableType.forRawClass(requiredType.java))

    override fun containsBeanDefinition(beanName: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun getBeanDefinition(beanName: String): BeanDefinition {
        TODO("Not yet implemented")
    }

    override fun createBean(beanName: String, mbd: RootBeanDefinition): Any {
        TODO("Not yet implemented")
    }

    override fun getMergedBeanDefinition(beanName: String): BeanDefinition {
        TODO("Not yet implemented")
    }

    override fun isFactoryBean(name: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun getDependentBeans(beanName: String): Array<String> {
        TODO("Not yet implemented")
    }

    override fun getDependenciesForBean(beanName: String): Array<String> {
        TODO("Not yet implemented")
    }

    override fun destroyBean(beanName: String, beanInstance: Any) {
        TODO("Not yet implemented")
    }

    override fun destroySingletons() {
        TODO("Not yet implemented")
    }

    // 1209
    override fun resolveDependency(descriptor: DependencyDescriptor, requestingBeanName: String,
            autowiredBeanNames: MutableSet<String>): Any? {
        // TODO: Optional, ObjectProvider
        return this.autowireCandidateResolver.getLazyResolutionProxyIfNecessary(descriptor, requestingBeanName)
                ?: doResolveDependency(descriptor, requestingBeanName, autowiredBeanNames)
    }

    // 1234
    fun doResolveDependency(descriptor: DependencyDescriptor, beanName: String,
            autowiredBeanNames: MutableSet<String>?): Any? {
        val type = descriptor.dependencyType
        val matchingBeans = findAutowireCandidates(beanName, type, descriptor)
        
        val autowiredBeanName: String
        var instanceCandidate: Any?
        
        if (matchingBeans.size > 1) {
            autowiredBeanName = determineAutowireCandidate(matchingBeans, descriptor)
            instanceCandidate = matchingBeans[autowiredBeanName]
        } else {
            val entry = matchingBeans.entries.first()
            autowiredBeanName = entry.key
            instanceCandidate = entry.value
        }

        autowiredBeanNames?.add(autowiredBeanName)

        if (instanceCandidate is KClass<*>) {
            instanceCandidate = this.getBean(autowiredBeanName)
        }

        var result = instanceCandidate
        if (result is NullBean) {
            result = null
        }
        if (!ClassUtils.isAssignableValue(type, result)) {
            throw BeanNotOfRequiredTypeException(autowiredBeanName, type, instanceCandidate?.javaClass!!)
        }
        return result
    }

    protected fun determineAutowireCandidate(matchingBeans: Map<String, Any?>, descriptor: DependencyDescriptor): String {
        TODO()
    }

    // 1470
    protected fun findAutowireCandidates(beanName: String, type: Class<*>, descriptor: DependencyDescriptor)
            : Map<String, Any?> {
        val candidates = LinkedHashMap<String, Any?>()

        if (containsSingleton(beanName)) {
            val beanInstance = this.getBean(beanName)
            candidates[beanName] = if (beanInstance is NullBean) null else beanInstance
        } else {
            candidates[beanName] = getType(beanName)
        }

        return candidates
    }

    private fun <T : Any> resolveBean(requiredType: ResolvableType): T {
        throw NoSuchBeanException(requiredType)
    }
}