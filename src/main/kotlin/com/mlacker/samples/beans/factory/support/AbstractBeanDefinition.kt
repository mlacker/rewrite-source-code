package com.mlacker.samples.beans.factory.support

import com.mlacker.samples.beans.factory.config.BeanDefinition
import com.mlacker.samples.beans.factory.config.ConfigurableBeanFactory
import org.springframework.beans.factory.support.AbstractBeanDefinition.AUTOWIRE_NO
import kotlin.reflect.KClass

abstract class AbstractBeanDefinition() : BeanDefinition, Cloneable {

    constructor(original: BeanDefinition) : this() {
    }

    var beanClass: KClass<*>? = null
    override var beanClassName: String? = null

    //    get() = beanClass?.java?.name
//    set(value) {
//        beanClass = ClassUtils.forName(value, classLoader).kotlin
//    }
    override var scope: String = ConfigurableBeanFactory.SCOPE_SINGLETON
    override var isLazyInit: Boolean = false
    override var dependsOn: Array<String> = emptyArray()
    override var isAutowireCandidate: Boolean = true
    override var isPrimary: Boolean = false
    override val isSingleton: Boolean
        get() = ConfigurableBeanFactory.SCOPE_SINGLETON == this.scope
    override val isPrototype: Boolean
        get() = ConfigurableBeanFactory.SCOPE_PROTOTYPE == this.scope

    override fun clone(): Any {
        return cloneBeanDefinition()
    }

    var autowireMode: Int = AUTOWIRE_NO

    abstract fun cloneBeanDefinition(): AbstractBeanDefinition
}