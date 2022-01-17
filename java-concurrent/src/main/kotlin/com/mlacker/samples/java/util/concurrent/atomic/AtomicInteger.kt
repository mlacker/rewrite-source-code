@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package com.mlacker.samples.java.util.concurrent.atomic

import jdk.internal.misc.Unsafe
import java.util.function.IntBinaryOperator
import java.util.function.IntUnaryOperator

class AtomicInteger(initialValue: Int = 0) {

    @Volatile
    private var value: Int = initialValue

    /**
     * Returns the current value,
     * with memory effects as specified by VarHandle#getVolatile.
     */
    fun get(): Int = value

    /**
     * Returns the current value, with memory semantics of reading as
     * if the variable was declared non-volatile.
     */
    fun getPlain(): Int =
        U.getInt(this, VALUE)

    /**
     * Returns the current value,
     * with memory effects as specified by VarHandle#getAcquire.
     */
    fun getAcquire(): Int =
        U.getIntAcquire(this, VALUE)

    /**
     * Sets the value to newValue,
     * with memory effects as specified by VarHandle#setVolatile
     */
    fun set(newValue: Int) {
        value = newValue
    }

    /**
     * Sets the value to newValue,
     * with memory effects as specified by VarHandle#setRelease.
     */
    fun lazySet(newValue: Int) =
        U.putIntRelease(this, VALUE, newValue)

    fun getAndSet(newValue: Int) =
        U.getAndSetInt(this, VALUE, newValue)

    fun compareAndSet(expectedValue: Int, newValue: Int): Boolean =
        U.compareAndSetInt(this, VALUE, expectedValue, newValue)

    fun compareAndExchange(expectedValue: Int, newValue: Int): Int =
        U.compareAndExchangeInt(this, VALUE, expectedValue, newValue)

    fun getAndIncrement(): Int =
        U.getAndAddInt(this, VALUE, 1)

    fun getAndDecrement(): Int =
        U.getAndAddInt(this, VALUE, -1)

    fun getAndAdd(delta: Int): Int =
        U.getAndAddInt(this, VALUE, delta)

    fun incrementAndGet(): Int =
        U.getAndAddInt(this, VALUE, 1) + 1

    fun getAndUpdate(updateFunction: IntUnaryOperator): Int {
        var prev = get()
        var next = 0
        var haveNext = false
        while (true) {
            if (!haveNext)
                next = updateFunction.applyAsInt(prev)
            if (weakCompareAndSetVolatile(prev, next))
                return prev
            /* haveNext = (prev == (prev = get()));
             * mov eax, get()
             * push prev
             * cmp eax
             * mov prev, eax
             */
            haveNext = prev == get().also { prev = it }
        }
    }

    fun updateAndGet(updateFunction: IntUnaryOperator): Int {
        var prev = get()
        var next = 0
        var haveNext = false
        while (true) {
            if (!haveNext)
                next = updateFunction.applyAsInt(prev)
            if (weakCompareAndSetVolatile(prev, next))
                return next
            haveNext = prev == get().also { prev = it }
        }
    }

    fun accumulateAndGet(x: Int, accumulatorFunction: IntBinaryOperator): Int {
        var prev = get()
        var next = 0
        var haveNext = false
        while (true) {
            if (!haveNext)
                next = accumulatorFunction.applyAsInt(prev, x)
            if (weakCompareAndSetVolatile(prev, next))
                return next
            haveNext = prev == get().also { prev = it }
        }
    }

    private fun weakCompareAndSetVolatile(expectedValue: Int, newValue: Int): Boolean =
        U.weakCompareAndSetInt(this, VALUE, expectedValue, newValue)

    companion object {
        private val U: Unsafe = Unsafe.getUnsafe()
        private val VALUE = U.objectFieldOffset(AtomicInteger::class.java, "value")
    }
}