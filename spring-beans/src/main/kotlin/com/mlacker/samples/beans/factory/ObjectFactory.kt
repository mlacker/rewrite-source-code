package com.mlacker.samples.beans.factory

@FunctionalInterface
interface ObjectFactory<T : Any> {

    fun getObject(): T
}