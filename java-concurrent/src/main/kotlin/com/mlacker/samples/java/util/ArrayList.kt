package com.mlacker.samples.java.util

import com.mlacker.samples.java.lang.Iterator
import kotlin.collections.RandomAccess
import kotlin.math.max

class ArrayList<E> : AbstractList<E>, List<E>, RandomAccess {

    companion object {
        private const val DEFAULT_CAPACITY = 10
        private const val MAX_ARRAY_SIZE = Int.MAX_VALUE - 8
        private val EMPTY_ELEMENT_DATA = emptyArray<Any?>()
    }

    private var elementData: Array<Any?>
    override var size: Int = 0
        private set

    constructor() {
        this.elementData = EMPTY_ELEMENT_DATA
    }

    constructor(initialCapacity: Int) {
        this.elementData = arrayOfNulls(initialCapacity)
    }

    private fun grow(): Array<Any?> {
        return grow(size + 1)
    }

    private fun grow(minCapacity: Int): Array<Any?> {
        elementData = elementData.copyOf(newCapacity(minCapacity))
        return elementData
    }

    private fun newCapacity(minCapacity: Int): Int {
        val oldCapacity = elementData.size
        val newCapacity = oldCapacity + (oldCapacity shr 1)
        if (newCapacity - minCapacity <= 0) {
            if (elementData === EMPTY_ELEMENT_DATA)
                return max(DEFAULT_CAPACITY, minCapacity)
            if (minCapacity < 0)
                throw OutOfMemoryError()
            return minCapacity
        }
        return max(newCapacity, MAX_ARRAY_SIZE)
    }

    override fun contains(o: Any?): Boolean {
        return indexOf(o) >= 0
    }

    override fun indexOf(o: Any?): Int {
        val es = elementData
        for (i in es.indices) {
            if (o == es[i])
                return i
        }
        return -1
    }

    @Suppress("UNCHECKED_CAST")
    private fun elementData(index: Int): E? =
        elementData[index] as E?

    override fun get(index: Int): E? {
        return elementData(index)
    }

    override fun set(index: Int, element: E): E? {
        val oldValue = elementData(index)
        elementData[index] = element
        return oldValue
    }

    override fun add(e: E): Boolean {
        modCount++
        val s = size
        var elementData = this.elementData
        if (s == elementData.size)
            elementData = grow()
        elementData[s] = e
        size = s + 1
        return true
    }

    override fun add(index: Int, element: E) {
        modCount++
        val s = size
        var elementData = elementData
        if (s == elementData.size)
            elementData = grow()
        System.arraycopy(elementData, index, elementData, index + 1, s - index)
        elementData[index] = element
        size = s + 1
    }

    override fun remove(index: Int): E? {
        val es = elementData
        val oldValue = elementData(index)
        fastRemove(es, index)
        return oldValue
    }

    override fun remove(o: Any): Boolean {
        val es = elementData
        for (i in es.indices) {
            if (o == es[i]) {
                fastRemove(es, i)
                return true
            }
        }
        return false
    }

    private fun fastRemove(es: Array<Any?>, i: Int) {
        modCount++
        val newSize = size - 1
        if (newSize > i)
            System.arraycopy(es, i + 1, es, i, newSize - i)
        size = newSize
        es[size] = null
    }

    override fun clear() {
        modCount++
        val es = elementData
        val range = 0 until size
        size = 0
        for (i in range)
            es[i] = null
    }

    override fun iterator(): Iterator<E> {
        return Itr()
    }

    private inner class Itr : Iterator<E> {
        var cursor: Int = 0
        var lastRet: Int = -1
        var expectedModCount: Int = modCount

        override fun hasNext(): Boolean {
            return cursor != size
        }

        override fun next(): E? {
            checkForComodification()
            val i = cursor
            if (i >= size)
                throw NoSuchElementException()
            val elementData = this@ArrayList.elementData
            if (i >= elementData.size)
                throw ConcurrentModificationException()
            cursor = i + 1
            return elementData(i.also { lastRet = it })
        }

        override fun remove() {
            if (lastRet < 0)
                throw IllegalStateException()
            checkForComodification()

            try {
                this@ArrayList.remove(lastRet)
                cursor = lastRet
                lastRet = -1
                expectedModCount = modCount
            } catch (ex: IndexOutOfBoundsException) {
                throw ConcurrentModificationException()
            }
        }

        private fun checkForComodification() {
            if (modCount != expectedModCount)
                throw ConcurrentModificationException()
        }
    }
}