package com.mlacker.samples.beans.factory.config

interface BeanDefinition {

    var beanClassName: String?
    var scope: String
    var isLazyInit: Boolean
    var dependsOn: Array<String>
    var isAutowireCandidate: Boolean
    var isPrimary: Boolean
    val isSingleton: Boolean
    val isPrototype: Boolean
}