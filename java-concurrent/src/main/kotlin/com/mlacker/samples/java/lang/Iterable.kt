package com.mlacker.samples.java.lang

interface Iterable<T> {

    operator fun iterator(): Iterator<T>
}