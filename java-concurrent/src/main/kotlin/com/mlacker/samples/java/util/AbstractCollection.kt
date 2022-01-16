package com.mlacker.samples.java.util

import com.mlacker.samples.java.lang.Iterator

abstract class AbstractCollection<E>: Collection<E> {

    abstract override fun iterator(): Iterator<E>

    abstract override val size: Int

    override fun isEmpty(): Boolean = size == 0

    override fun contains(o: Any?): Boolean {
        val it = iterator()
        while (it.hasNext())
            if (o == it.next())
                return true
        return false
    }

    override fun add(e: E): Boolean {
        throw UnsupportedOperationException()
    }

    override fun remove(o: Any): Boolean {
        val it = iterator()
        while (it.hasNext()) {
            if (o == it.next()) {
                it.remove()
                return true
            }
        }
        return false
    }

    override fun clear() {
        val it = iterator()
        while (it.hasNext()) {
            it.next()
            it.remove()
        }
    }
}