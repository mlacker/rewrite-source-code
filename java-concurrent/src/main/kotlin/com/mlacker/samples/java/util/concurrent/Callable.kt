package com.mlacker.samples.java.util.concurrent

@FunctionalInterface
interface Callable<V> {

    fun call(): V
}