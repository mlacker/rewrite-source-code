package com.mlacker.samples.java.util.concurrent

import com.mlacker.samples.java.util.concurrent.locks.AbstractQueuedSynchronizer

class CountDownLatch(count: Int) {

    private val sync: Sync = Sync(count)

    @Throws(InterruptedException::class)
    fun await() {
        sync.acquireShared(1)
    }

    fun countDown() {
        sync.releaseShared(1)
    }

    fun getCount(): Long =
        sync.count.toLong()

    private class Sync(count: Int) : AbstractQueuedSynchronizer() {
        init {
            state = count
        }

        val count: Int get() = state

        override fun tryAcquireShared(acquires: Int): Int {
            return if (state == 0) 1 else -1
        }

        override fun tryReleaseShared(releases: Int): Boolean {
            while (true) {
                val c = state
                if (c == 0)
                    return false
                val nextc = c - 1
                if (compareAndSetState(c, nextc))
                    return nextc == 0
            }
        }
    }
}