package com.mlacker.samples.java.util

interface Map<K, V> {

    val size: Int

    val isEmpty: Boolean

    fun containsKey(key: K): Boolean

    fun get(key: K): V?

    fun put(key: K, value: V): V?

    fun remove(key: K): V?

    fun clear()

    interface Entry<K, V> {
        val key: K
        var value: V
    }
}