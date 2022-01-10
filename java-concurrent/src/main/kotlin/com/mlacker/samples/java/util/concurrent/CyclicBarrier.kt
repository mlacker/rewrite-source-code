package com.mlacker.samples.java.util.concurrent

import com.mlacker.samples.java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.BrokenBarrierException

class CyclicBarrier(
    private val parties: Int,
    private val barrierCommand: Runnable? = null
) {

    private val lock = ReentrantLock()
    private val trip = lock.newCondition()
    private var generation: Generation = Generation()
    private var count: Int = parties

    @Throws(InterruptedException::class, BrokenBarrierException::class)
    fun await(): Int {
        return doWait()
    }

    fun isBroken(): Boolean {
        lock.lock()
        try {
            return generation.broken
        } finally {
            lock.unlock()
        }
    }

    fun reset() {
        lock.lock()
        try {
            breakBarrier()
            nextGeneration()
        } finally {
            lock.unlock()
        }
    }

    fun getNumberWaiting(): Int {
        lock.lock()
        try {
            return parties - count
        } finally {
            lock.unlock()
        }
    }

    private fun nextGeneration() {
        trip.signalAll()
        count = parties
        generation = Generation()
    }

    private fun breakBarrier() {
        generation.broken = true
        count = parties
        trip.signalAll()
    }

    @Throws(InterruptedException::class, BrokenBarrierException::class)
    private fun doWait(): Int {
        lock.lock()
        try {
            val g = generation

            if (g.broken) {
                throw BrokenBarrierException()
            }

            if (Thread.interrupted()) {
                breakBarrier()
                throw InterruptedException()
            }

            val index = --count
            if (index == 0) {
                var ranAction = false
                try {
                    barrierCommand?.run()
                    ranAction = true
                    nextGeneration()
                    return 0
                } finally {
                    if (!ranAction)
                        breakBarrier()
                }
            }

            while (true) {
                try {
                    trip.await()
                } catch (ie: InterruptedException) {
                    if (g == generation && !g.broken) {
                        breakBarrier()
                        throw ie
                    } else {
                        Thread.currentThread().interrupt()
                    }
                }

                if (g.broken)
                    throw BrokenBarrierException()

                if (g != generation)
                    return index
            }
        } finally {
            lock.unlock()
        }
    }

    private class Generation {
        var broken: Boolean = false
    }
}