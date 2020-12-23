package com.mlacker.samples.beans.factory.support

import com.mlacker.samples.beans.factory.config.BeanDefinition
import com.mlacker.samples.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.BeanNotOfRequiredTypeException
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.config.DependencyDescriptor
import org.springframework.beans.factory.config.NamedBeanHolder
import org.springframework.beans.factory.support.AutowireCandidateResolver
import org.springframework.beans.factory.support.SimpleAutowireCandidateResolver
import org.springframework.core.ResolvableType
import org.springframework.util.ClassUtils
import org.springframework.util.StringUtils
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class DefaultListableBeanFactory : AbstractAutowireCapableBeanFactory(),
        ConfigurableListableBeanFactory, BeanDefinitionRegistry, Serializable {

    val autowireCandidateResolver: AutowireCandidateResolver = SimpleAutowireCandidateResolver.INSTANCE

    private val resolvableDependencies: MutableMap<KClass<*>, Any> = ConcurrentHashMap(16)

    private val beanDefinitionMap: MutableMap<String, BeanDefinition> = ConcurrentHashMap(16)

    private val beanDefinitionNames: MutableList<String> = ArrayList(256)

    private val manualSingletonNames: MutableSet<String> = LinkedHashSet(16)

    @Volatile
    private var frozenBeanDefinitionNames: Array<String>? = null

    @Volatile
    private var configurationFrozen = false

    //---------------------------------------------------------------------
    // Implementation of remaining BeanFactory methods
    //---------------------------------------------------------------------

    // 342
    override fun <T : Any> getBean(requiredType: KClass<T>): T =
            resolveBean(ResolvableType.forRawClass(requiredType.java))

    // 419
    private fun <T : Any> resolveBean(requiredType: ResolvableType): T =
            resolveNamedBean<T>(requiredType).beanInstance

    // 1155
    private fun <T> resolveNamedBean(requiredType: ResolvableType): NamedBeanHolder<T> {
        TODO("Not yet implemented")
    }

    //---------------------------------------------------------------------
    // Implementation of ListableBeanFactory interface
    //---------------------------------------------------------------------

    // 450
    override fun containsBeanDefinition(beanName: String) = this.beanDefinitionMap.containsKey(beanName)

    // 456
    override fun getBeanDefinitionCount() = this.beanDefinitionMap.size

    // 461
    override fun getBeanDefinitionNames(): Array<String> =
            if (this.frozenBeanDefinitionNames != null) this.frozenBeanDefinitionNames!!.clone()
            else this.beanDefinitionNames.toTypedArray()

    override fun getBeanNamesForType(type: ResolvableType, includeNonSingletons: Boolean, allowEagerInit: Boolean): Array<String> {
        TODO("Not yet implemented")
    }

    override fun getBeanNamesForType(type: KClass<*>, includeNonSingletons: Boolean, allowEagerInit: Boolean): Array<String> {
        TODO("Not yet implemented")
    }

    //---------------------------------------------------------------------
    // Implementation of ConfigurableListableBeanFactory interface
    //---------------------------------------------------------------------

    // 733
    override fun registerResolvableDependency(dependencyType: KClass<*>, autowiredValue: Any) {
        if (!dependencyType.isInstance(autowiredValue)) {
            throw IllegalArgumentException("Value [$autowiredValue] " +
                    "does not implement specified dependency type [${dependencyType.simpleName}]")
        }
        this.resolvableDependencies[dependencyType] = autowiredValue
    }

    // 745
    override fun isAutowireCandidate(beanName: String, descriptor: DependencyDescriptor): Boolean =
            isAutowireCandidate(beanName, descriptor, this.autowireCandidateResolver)

    // 759
    protected fun isAutowireCandidate(beanName: String,
            descriptor: DependencyDescriptor, resolver: AutowireCandidateResolver): Boolean {
        TODO("Not yet implemented")
    }

    // 810
    override fun getBeanDefinition(beanName: String): BeanDefinition {
        val bd = this.beanDefinitionMap[beanName]
        if (bd == null) {
            logger.trace("No bean named '$beanName' found in $this")
            throw NoSuchBeanDefinitionException(beanName)
        }
        return bd
    }

    // 843
    override fun freezeConfiguration() {
        this.configurationFrozen = true
        this.frozenBeanDefinitionNames = StringUtils.toStringArray(this.beanDefinitionNames)
    }

    // 849
    override val isConfigurationFrozen: Boolean
        get() = this.configurationFrozen

    // 864
    override fun preInstantiateSingletons() {
        logger.trace("Pre-instantiation singletons in $this")

        val beanNames = ArrayList(this.beanDefinitionNames)

        for (beanName in beanNames) {
            val bd = getMergedLocalBeanDefinition(beanName)
            if (bd.isSingleton && !bd.isLazyInit) {
                getBean(beanName)
            }
        }
    }

    //---------------------------------------------------------------------
    // Implementation of BeanDefinitionRegistry interface
    //---------------------------------------------------------------------

    // 926
    override fun registerBeanDefinition(beanName: String, beanDefinition: BeanDefinition) {
        if (beanDefinition is AbstractBeanDefinition) {
            beanDefinition.validate()
        }

        val existingDefinition = this.beanDefinitionMap[beanName]
        if (existingDefinition != null) {
            logger.debug("Overriding bean definition for bean '$beanName'")
            this.beanDefinitionMap[beanName] = beanDefinition
        } else {
            this.beanDefinitionMap[beanName] = beanDefinition
            this.beanDefinitionNames.add(beanName)
            removeManualSingletonName(beanName)

            this.frozenBeanDefinitionNames = null
        }

        if (existingDefinition != null || containsSingleton(beanName)) {
            resetBeanDefinition(beanName)
        }
    }

    // 1001
    override fun removeBeanDefinition(beanName: String) {
        val bd = this.beanDefinitionMap.remove(beanName) ?: throw NoSuchBeanDefinitionException(beanName)

        this.beanDefinitionNames.remove(beanName)
        this.frozenBeanDefinitionNames = null

        resetBeanDefinition(beanName)
    }

    // 1040
    protected fun resetBeanDefinition(beanName: String) {
        clearMergedBeanDefinition(beanName)

        destroySingleton(beanName)
    }

    // 1077
    override fun registerSingleton(beanName: String, singletonObject: Any) {
        super.registerSingleton(beanName, singletonObject)
        if (!this.beanDefinitionMap.contains(beanName)) {
            this.manualSingletonNames.add(beanName)
        }
    }

    // 1084
    override fun destroySingletons() {
        super.destroySingletons()
        this.manualSingletonNames.clear()
    }

    // 1091
    override fun destroySingleton(beanName: String) {
        super.destroySingleton(beanName)
        this.manualSingletonNames.clear()
    }

    // 1097
    private fun removeManualSingletonName(beanName: String) {
        this.manualSingletonNames.remove(beanName)
    }

    //---------------------------------------------------------------------
    // Dependency resolution functionality
    //---------------------------------------------------------------------

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

    // 1549
    protected fun determineAutowireCandidate(candidates: Map<String, Any?>, descriptor: DependencyDescriptor): String {
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
}