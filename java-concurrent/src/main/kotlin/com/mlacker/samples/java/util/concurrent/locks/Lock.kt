package com.mlacker.samples.java.util.concurrent.locks

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import kotlin.jvm.Throws

interface Lock {

    fun lock()

    @Throws(InterruptedException::class)
    fun lockInterruptibly()

    fun tryLock(): Boolean

    @Throws(InterruptedException::class)
    fun tryLock(time: Long, unit: TimeUnit): Boolean

    fun unlock()

    fun newCondition(): Condition
}