package com.mlacker.samples.java.util

abstract class AbstractMap<K, V>: Map<K, V> {

    override val isEmpty: Boolean
        get() = size == 0


}