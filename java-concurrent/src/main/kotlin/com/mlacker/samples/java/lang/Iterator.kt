package com.mlacker.samples.java.lang

interface Iterator<E> {

    operator fun hasNext(): Boolean

    operator fun next(): E?

    fun remove()
}