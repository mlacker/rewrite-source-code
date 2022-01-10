package com.mlacker.samples.java.util.concurrent

import com.mlacker.samples.java.util.concurrent.locks.AbstractQueuedSynchronizer

class Semaphore(permits: Int) {

    private val sync: Sync = NonfairSync(permits)

    fun acquire(permits: Int = 1) {
        if (permits < 0) throw IllegalArgumentException()
        sync.acquireShared(permits)
    }

    fun tryAcquire(permits: Int = 1): Boolean {
        if (permits < 0) throw IllegalArgumentException()
        return sync.nonfairTryAcquireShared(permits) >= 0
    }

    fun release(permits: Int = 1) {
        if (permits < 0) throw IllegalArgumentException()
        sync.releaseShared(permits)
    }

    fun availablePermits(): Int =
        sync.permits

    fun drainPermits(): Int =
        sync.drainPermits()

    fun reducePermits(reduction: Int) {
        sync.reducePermits(reduction)
    }

    private abstract class Sync(permits: Int) : AbstractQueuedSynchronizer() {

        init {
            state = permits
        }

        val permits: Int
            get() = state


        fun nonfairTryAcquireShared(acquires: Int): Int {
            while (true) {
                val available = state
                val remaining = available - acquires
                if (remaining < 0 ||
                    compareAndSetState(available, remaining)
                ) return remaining
            }
        }

        override fun tryReleaseShared(releases: Int): Boolean {
            while (true) {
                val current = state
                val next = current + releases
                if (next < current)
                    throw Error("Maximum permit count exceeded")
                if (compareAndSetState(current, next))
                    return true
            }
        }

        fun reducePermits(reductions: Int) {
            while (true) {
                val current = state
                val next = current - reductions
                if (next > current)
                    throw Error("Permit count underflow")
                if (compareAndSetState(current, next))
                    return
            }
        }

        fun drainPermits(): Int {
            while (true) {
                val current = state
                if (current == 0 || compareAndSetState(current, 0))
                    return current
            }
        }
    }

    private class NonfairSync(permits: Int) : Sync(permits) {

        override fun tryAcquireShared(acquires: Int): Int {
            return nonfairTryAcquireShared(acquires)
        }
    }
}