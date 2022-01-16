package com.mlacker.samples.java.util

import com.mlacker.samples.java.lang.Iterable
import com.mlacker.samples.java.lang.Iterator

interface Collection<E>: Iterable<E> {

    val size: Int

    fun isEmpty(): Boolean

    fun contains(o: Any?): Boolean

    override fun iterator(): Iterator<E>

    fun add(e: E): Boolean

    fun remove(o: Any): Boolean

    fun clear()

    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int
}