package com.mlacker.samples.java.util

import com.mlacker.samples.java.lang.Iterator

interface List<E>: Collection<E> {

    override val size: Int

    override fun isEmpty(): Boolean

    override fun contains(o: Any?): Boolean

    override fun iterator(): Iterator<E>

    override fun add(e: E): Boolean

    override fun remove(o: Any): Boolean

    override fun clear()

    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int

    fun get(index: Int): E?

    fun set(index: Int, element: E): E?

    fun add(index: Int, element: E)

    fun remove(index: Int): E?

    fun indexOf(o: Any?): Int
}