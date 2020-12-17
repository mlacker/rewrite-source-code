package com.mlacker.samples.beans

import org.springframework.beans.BeansException
import org.springframework.core.ResolvableType

class NoSuchBeanException
private constructor(val name: String?, val resolvableType: ResolvableType?) :
        BeansException(
                if (name != null) "No bean named '$name' available"
                else "No qualifying bean of type '$resolvableType' available"
        ) {
    constructor(name: String) : this(name, null)
    constructor(type: ResolvableType) : this(null, type)
}