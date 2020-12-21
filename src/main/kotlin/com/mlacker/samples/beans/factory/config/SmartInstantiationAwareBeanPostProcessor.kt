package com.mlacker.samples.beans.factory.config

import kotlin.reflect.KClass
import kotlin.reflect.KFunction

interface SmartInstantiationAwareBeanPostProcessor: InstantiationAwareBeanPostProcessor {

    fun determineCandidateConstructors(beanClass: KClass<*>?, beanName: String): Collection<KFunction<*>>? = null
}