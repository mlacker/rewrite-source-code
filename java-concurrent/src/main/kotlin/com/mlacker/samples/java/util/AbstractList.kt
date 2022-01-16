package com.mlacker.samples.java.util

abstract class AbstractList<E>: AbstractCollection<E>(), List<E> {

    protected var modCount = 0

    override fun add(e: E): Boolean {
        add(size, e)
        return true
    }

    abstract override fun get(index: Int): E?

    override fun set(index: Int, element: E): E? {
        throw UnsupportedOperationException()
    }
    override fun add(index: Int, element: E) {
        throw UnsupportedOperationException()
    }

    override fun remove(index: Int): E? {
        throw UnsupportedOperationException()
    }

    override fun indexOf(o: Any?): Int {
        throw UnsupportedOperationException()
    }

    override fun equals(other: Any?): Boolean {
        if (other == this)
            return true
        if (other !is List<*>)
            return false

        val e1 = iterator()
        val e2 = other.iterator()
        while (e1.hasNext() && e2.hasNext()) {
            val o1 = e1.next()
            val o2 = e2.next()
            if (o1 != o2)
                return false
        }
        return !(e1.hasNext() || e2.hasNext())
    }

    override fun hashCode(): Int {
        var hashCode = 1
        for (e in this) {
            hashCode = 31 * hashCode + e.hashCode()
        }
        return hashCode
    }
}