package com.mlacker.samples.beans.factory.support

import com.mlacker.samples.beans.NoSuchBeanException
import com.mlacker.samples.beans.factory.ObjectFactory
import com.mlacker.samples.beans.factory.config.SingletonBeanRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.BeanCreationNotAllowedException
import org.springframework.beans.factory.BeanCurrentlyInCreationException
import org.springframework.beans.factory.DisposableBean
import org.springframework.util.StringUtils
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet

open class DefaultSingletonBeanRegistry : SingletonBeanRegistry {

    companion object {
        private const val SUPPRESSED_EXCEPTIONS_LIMIT: Int = 100
    }

    private val singletonObjects: MutableMap<String, Any> = ConcurrentHashMap(256)

    private val singletonFactories: MutableMap<String, ObjectFactory<*>> = HashMap(16)

    private val earlySingletonObjects: MutableMap<String, Any> = HashMap(16)

    private val registeredSingletons: MutableSet<String> = LinkedHashSet(256)

    private val singletonsCurrentlyInCreation: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap(16))

    private val inCreationCheckExclusions: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap(16))

    private var suppressedExceptions: MutableSet<Exception>? = null

    private var singletonsCurrentlyInDestruction = false

    private val disposableBeans: MutableMap<String, DisposableBean> = LinkedHashMap()

    private val dependentBeanMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap(64)

    private val dependenciesForBeanMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap(64)

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun registerSingleton(beanName: String, singletonObject: Any) {
        synchronized(this.singletonObjects) {
            val oldObject = this.singletonObjects[beanName]
            if (oldObject != null)
                throw IllegalStateException("Cloud not register object [$singletonObject] " +
                        "under bean name '$beanName': there is already object [$oldObject] bound")
            addSingleton(beanName, singletonObject)
        }
    }

    protected fun addSingleton(beanName: String, singletonObject: Any) {
        synchronized(this.singletonObjects) {
            this.singletonObjects[beanName] = singletonObject
            this.singletonFactories.remove(beanName)
            this.earlySingletonObjects.remove(beanName)
            this.registeredSingletons.add(beanName)
        }
    }

    protected fun addSingletonFactory(beanName: String, singletonFactory: ObjectFactory<*>) {
        synchronized(this.singletonObjects) {
            if (!this.singletonObjects.containsKey(beanName)) {
                this.singletonFactories[beanName] = singletonFactory
                this.earlySingletonObjects.remove(beanName)
                this.registeredSingletons.add(beanName)
            }
        }
    }

    override fun getSingleton(beanName: String) = getSingleton(beanName, true)

    protected fun getSingleton(beanName: String, allowEarlyReference: Boolean): Any {
        var singletonObject = this.singletonObjects[beanName]
        if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
            synchronized(this.singletonObjects) {
                singletonObject = this.earlySingletonObjects[beanName]
                if (singletonObject == null && allowEarlyReference) {
                    val singletonFactory = this.singletonFactories[beanName]
                    if (singletonFactory != null) {
                        singletonObject = singletonFactory.getObject()
                        this.earlySingletonObjects[beanName] = singletonObject!!
                        this.singletonFactories.remove(beanName)
                    }
                }
            }
        }

        if (singletonObject == null) throw NoSuchBeanException(beanName)

        return singletonObject!!
    }

    fun getSingleton(beanName: String, singletonFactory: ObjectFactory<*>): Any {
        synchronized(this.singletonObjects) {
            var singletonObject = this.singletonObjects[beanName]
            if (singletonObject == null) {
                if (this.singletonsCurrentlyInDestruction) {
                    throw BeanCreationNotAllowedException(beanName,
                            "Singleton bean creation not allowed while singletons of this factory are in destruction " +
                                    "(Do not request a bean from a BeanFactory in a destroy method implementation!)")
                }
                logger.debug("Creating shared instance of singleton bean '$beanName'")
                beforeSingletonCreation(beanName)
                var newSingleton = false
                val recordSuppressedExceptions = (this.suppressedExceptions == null)
                if (recordSuppressedExceptions)
                    this.suppressedExceptions = LinkedHashSet()
                try {
                    singletonObject = singletonFactory.getObject()
                    newSingleton = true
                } catch (ex: IllegalStateException) {
                    singletonObject = this.singletonObjects[beanName]
                    if (singletonObject == null)
                        throw ex
                } catch (ex: BeanCreationException) {
                    if (recordSuppressedExceptions) {
                        this.suppressedExceptions!!.forEach { ex.addRelatedCause(it) }
                    }
                    throw ex
                } finally {
                    if (recordSuppressedExceptions) {
                        this.suppressedExceptions = null
                    }
                    afterSingletonCreation(beanName)
                }
                if (newSingleton)
                    addSingleton(beanName, singletonObject!!)
            }
            return singletonObject!!
        }
    }

    protected fun onSuppressedException(ex: Exception) {
        synchronized(this.singletonObjects) {
            if (this.suppressedExceptions != null && this.suppressedExceptions!!.size < SUPPRESSED_EXCEPTIONS_LIMIT) {
                this.suppressedExceptions!!.add(ex)
            }
        }
    }

    protected fun removeSingleton(beanName: String) {
        synchronized(this.singletonObjects) {
            this.singletonObjects.remove(beanName)
            this.singletonFactories.remove(beanName)
            this.earlySingletonObjects.remove(beanName)
            this.registeredSingletons.remove(beanName)
        }
    }

    override fun containsSingleton(beanName: String) = this.singletonObjects.containsKey(beanName)

    override fun getSingletonNames(): Array<String> {
        synchronized(this.singletonObjects) {
            return StringUtils.toStringArray(this.registeredSingletons)
        }
    }

    override fun getSingletonCount(): Int {
        synchronized(this.singletonObjects) {
            return this.registeredSingletons.size
        }
    }

    fun setCurrentlyInCreation(beanName: String, inCreation: Boolean) {
        if (!inCreation) {
            this.inCreationCheckExclusions.add(beanName)
        } else {
            this.inCreationCheckExclusions.remove(beanName)
        }
    }

    fun isCurrentlyInCreation(beanName: String) =
            (!this.inCreationCheckExclusions.contains(beanName) && isActuallyInCreation(beanName))

    protected open fun isActuallyInCreation(beanName: String) = isSingletonCurrentlyInCreation(beanName)

    fun isSingletonCurrentlyInCreation(beanName: String) = this.singletonsCurrentlyInCreation.contains(beanName)

    protected fun beforeSingletonCreation(beanName: String) {
        if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.add(beanName))
            throw BeanCurrentlyInCreationException(beanName)
    }

    protected fun afterSingletonCreation(beanName: String) {
        if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.remove(beanName))
            throw IllegalStateException("Singleton '$beanName' isn't currently in creation")
    }

    fun registerDisposableBean(beanName: String, bean: DisposableBean) {
        synchronized(this.disposableBeans) {
            this.disposableBeans[beanName] = bean
        }
    }

    fun registerDependentBean(beanName: String, dependentBeanName: String) {
        synchronized(this.dependentBeanMap) {
            val dependentBeans = this.dependentBeanMap.computeIfAbsent(beanName) { LinkedHashSet(8) }
            if (!dependentBeans.add(dependentBeanName)) {
                return
            }
        }

        synchronized(this.dependenciesForBeanMap) {
            this.dependenciesForBeanMap
                    .computeIfAbsent(dependentBeanName) { LinkedHashSet(8) }
                    .add(beanName)
        }
    }

    protected fun hasDependentBean(beanName: String) = this.dependentBeanMap.containsKey(beanName)

    fun getDependentBeans(beanName: String) =
            this.dependentBeanMap[beanName]?.toTypedArray() ?: emptyArray()

    fun getDependenciesForBean(beanName: String): Array<String> =
            this.dependenciesForBeanMap[beanName]?.toTypedArray() ?: emptyArray()

    open fun destroySingletons() {
        logger.trace("Destroying singletons in $this")

        synchronized(this.singletonObjects) {
            this.singletonsCurrentlyInDestruction = true
        }

        val disposableBeanNames: Array<String>
        synchronized(this.disposableBeans) {
            disposableBeanNames = this.disposableBeans.keys.toTypedArray()
        }
        for (i in disposableBeanNames.size - 1 downTo 0) {
            destroySingleton(disposableBeanNames[i])
        }

        clearSingletonCache()
    }

    protected fun clearSingletonCache() {
        synchronized(this.singletonObjects) {
            this.singletonObjects.clear()
            this.singletonFactories.clear()
            this.earlySingletonObjects.clear()
            this.registeredSingletons.clear()
            this.singletonsCurrentlyInDestruction = false
        }
    }

    open fun destroySingleton(beanName: String) {
        removeSingleton(beanName)

        val disposableBean: DisposableBean?
        synchronized(this.disposableBeans) {
            disposableBean = this.disposableBeans.remove(beanName)
        }
        destroyBean(beanName, disposableBean)
    }

    protected fun destroyBean(beanName: String, bean: DisposableBean?) {
        // TODO("Not yet implemented")
    }

    override fun getSingletonMutex() = this.singletonObjects
}