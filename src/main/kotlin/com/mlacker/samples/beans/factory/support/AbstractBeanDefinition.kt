package com.mlacker.samples.beans.factory.support

import com.mlacker.samples.beans.factory.config.BeanDefinition
import com.mlacker.samples.beans.factory.config.ConfigurableBeanFactory
import org.springframework.beans.factory.support.AbstractBeanDefinition.AUTOWIRE_NO
import org.springframework.util.ClassUtils
import org.springframework.util.StringUtils
import kotlin.reflect.KClass

abstract class AbstractBeanDefinition : BeanDefinition, Cloneable {
    constructor()
    constructor(original: BeanDefinition) {
        this.beanClassName = original.beanClassName
        if (StringUtils.hasLength(original.scope)) {
            this.scope = original.scope
        }
        this.isLazyInit = original.isLazyInit
        this.isPrimary = original.isPrimary
        this.isAutowireCandidate = original.isAutowireCandidate
        if (original is AbstractBeanDefinition) {
            beanClass = original.beanClass
            autowireMode = original.autowireMode
        }
    }

    var beanClass: KClass<*>? = null
        get() {
            if (field != null) {
                return field
            }

            if (beanClassName != null) {
                field = ClassUtils.forName(beanClassName!!, null).kotlin
            }

            return field
        }

    override var beanClassName: String? = null
    override var scope: String = ConfigurableBeanFactory.SCOPE_SINGLETON
    override var isLazyInit: Boolean = false
    override var dependsOn: Array<String> = emptyArray()
    override var isAutowireCandidate: Boolean = true
    override var isPrimary: Boolean = false
    override val isSingleton: Boolean
        get() = ConfigurableBeanFactory.SCOPE_SINGLETON == this.scope
    override val isPrototype: Boolean
        get() = ConfigurableBeanFactory.SCOPE_PROTOTYPE == this.scope

    fun validate() {

    }

    override fun clone(): Any {
        return cloneBeanDefinition()
    }

    var autowireMode: Int = AUTOWIRE_NO

    abstract fun cloneBeanDefinition(): AbstractBeanDefinition
}