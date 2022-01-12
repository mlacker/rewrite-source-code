package com.mlacker.samples.java.util.concurrent.locks

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition

class ReentrantReadWriteLock : ReadWriteLock {

    override val readLock: Lock = ReadLock(this)
    override val writeLock: Lock = WriteLock(this)
    private val sync: Sync = NonfairSync()

    private abstract class Sync : AbstractQueuedSynchronizer() {

        private var readHolds: ThreadLocalHoldCounter = ThreadLocalHoldCounter()
        private var cachedHoldCounter: HoldCounter? = null
        private var firstReader: Thread? = null
        private var firstReaderHoldCount: Int = 0

        protected abstract fun readerShouldBlock(): Boolean

        protected abstract fun writerShouldBlock(): Boolean

        override fun tryRelease(releases: Int): Boolean {
            if (!isHeldExclusively())
                throw IllegalMonitorStateException()
            val nextc = state - releases
            val free = exclusiveCount(nextc) == 0
            if (free)
                exclusiveOwnerThread = null
            state = nextc
            return free
        }

        override fun tryAcquire(acquires: Int): Boolean {
            /*
             * Walkthrough:
             * 1. If read count nonzero or write count nonzero
             *    and owner is a different thread, fail.
             * 2. If count would saturate, fail. (This can only
             *    happen if count is already nonzero.)
             * 3. Otherwise, this thread is eligible for lock if
             *    it is either a reentrant acquire or
             *    queue policy allows it. If so, update state
             *    and set owner.
             */
            val current = Thread.currentThread()
            val c = state
            val w = exclusiveCount(c)
            if (c != 0) {
                // (Note: if c != 0 and w == 0 then shared count !=0)
                if (w == 0 || current != exclusiveOwnerThread)
                    return false
                if (w + exclusiveCount(acquires) > MAX_COUNT)
                    throw Error("Maximum lock count exceeded")
                // Reentrant acquire
                state = c + acquires
                return true
            }
            if (writerShouldBlock() || !compareAndSetState(c, c + acquires))
                return false
            exclusiveOwnerThread = current
            return true
        }

        override fun tryReleaseShared(releases: Int): Boolean {
            val current = Thread.currentThread()
            if (firstReader == current) {
                if (firstReaderHoldCount == 1)
                    firstReader = null
                else
                    firstReaderHoldCount--
            } else {
                var rh = cachedHoldCounter
                if (rh == null || rh.tid != current.id)
                    rh = readHolds.get()
                val count = rh!!.count
                if (count <= 1) {
                    readHolds.remove()
                    if (count <= 0)
                        throw IllegalMonitorStateException("attempt to unlock read lock, not locked by current thread")
                }
                --rh.count
            }
            while (true) {
                val c = state
                val nextc = c - SHARED_UNIT
                if (compareAndSetState(c, nextc))
                    return nextc == 0
            }
        }

        override fun tryAcquireShared(acquires: Int): Int {
            /*
             * Walkthrough:
             * 1. If write lock held by another thread, fail.
             * 2. Otherwise, this thread is eligible for lock wrt state,
             *    so ask if it should block because of queue policy.
             *    If not, try to grant by CASing state and updating count.
             *    Note that step does not check for reentrant acquires,
             *    which is postponed to full version to avoid having to
             *    check hold count in the more typical non-reentrant case.
             * 3. If step 2 fails either because thread apparently not eligible
             *    or CAS fails or count saturated, chain to version with
             *    full retry loop.
             */
            val current = Thread.currentThread()
            val c = state
            if (exclusiveCount(c) != 0 && exclusiveOwnerThread != current)
                return -1
            val r = sharedCount(c)
            if (!readerShouldBlock() && r < MAX_COUNT &&
                compareAndSetState(c, c + SHARED_UNIT)
            ) {
                if (r == 0) {
                    firstReader = current
                    firstReaderHoldCount = 1
                } else if (firstReader == current) {
                    firstReaderHoldCount++
                } else {
                    var rh = cachedHoldCounter
                    if (rh == null || rh.tid != current.id) {
                        rh = readHolds.get()
                        cachedHoldCounter = rh
                    } else if (rh.count == 0)
                        readHolds.set(rh)
                    rh!!.count++
                }
                return 1
            }
            return fullTryAcquireShared(current)
        }

        /**
         * Full version of acquire for reads, that handles CAS misses
         * and reentrant read not dealt with in tryAcquireShared.
         */
        private fun fullTryAcquireShared(current: Thread): Int {
            /*
             * This code is in part redundant with that in
             * tryAcquireShared but is simpler overall by not
             * complication tryAcquireShared with interactions between
             * retries and lazily reading hold counts.
             */
            var rh: HoldCounter? = null
            while (true) {
                val c = state
                if (exclusiveCount(c) != 0) {
                    if (exclusiveOwnerThread != current)
                        return -1
                    // else we hold the exclusive lock; blocking here
                    // would cause deadlock.
                } else if (readerShouldBlock()) {
                    // Make sure we're not acquiring read lock reentrantly
                    if (firstReader == current) {
                        // assert firstReaderHoldCount > 0
                    } else {
                        if (rh == null) {
                            rh = cachedHoldCounter
                            if (rh == null || rh.tid != current.id) {
                                rh = readHolds.get()
                                if (rh.count == 0)
                                    readHolds.remove()
                            }
                        }
                        if (rh!!.count == 0)
                            return -1
                    }
                }
                if (sharedCount(c) == MAX_COUNT)
                    throw Error("Maximum lock count exceeded")
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (sharedCount(c) == 0) {
                        firstReader = current
                        firstReaderHoldCount = 1
                    } else if (firstReader == current) {
                        firstReaderHoldCount++
                    } else {
                        if (rh == null)
                            rh = cachedHoldCounter
                        if (rh == null || rh.tid != current.id)
                            rh = readHolds.get()
                        else if (rh.count == 0)
                            readHolds.set(rh)
                        rh!!.count++
                        cachedHoldCounter = rh
                    }
                    return 1
                }
            }
        }

        fun tryWriteLock(): Boolean {
            val current = Thread.currentThread()
            val c = state
            if (c != 0) {
                val w = exclusiveCount(c)
                if (w == 0 || current != exclusiveOwnerThread)
                    return false
                if (w == MAX_COUNT)
                    throw Error("Maximum lock count exceeded")
            }
            if (!compareAndSetState(c, c + 1))
                return false
            exclusiveOwnerThread = current
            return true
        }

        fun tryReadLock(): Boolean {
            val current = Thread.currentThread()
            while (true) {
                val c = state
                if (exclusiveCount(c) != 0 && exclusiveOwnerThread != current)
                    return false
                val r = sharedCount(c)
                if (r == MAX_COUNT)
                    throw Error("Maximum lock count exceeded")
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (r == 0) {
                        firstReader = current
                        firstReaderHoldCount = 1
                    } else if (firstReader == current) {
                        firstReaderHoldCount++
                    } else {
                        var rh = cachedHoldCounter
                        if (rh == null || rh.tid != current.id)
                            cachedHoldCounter = readHolds.get().also { rh = it }
                        else if (rh!!.count == 0)
                            readHolds.set(rh)
                        rh!!.count++
                    }
                    return true
                }
            }
        }

        override fun isHeldExclusively(): Boolean {
            return exclusiveOwnerThread == Thread.currentThread()
        }

        fun newCondition(): Condition {
            return ConditionObject()
        }

        companion object {
            const val SHARED_SHIFT = 16
            const val SHARED_UNIT = (1 shl SHARED_SHIFT)
            const val MAX_COUNT = (1 shl SHARED_SHIFT) - 1
            const val EXCLUSIVE_MASK = (1 shl SHARED_SHIFT) - 1

            fun sharedCount(c: Int) = c ushr SHARED_SHIFT
            fun exclusiveCount(c: Int) = c and EXCLUSIVE_MASK
        }

        private class HoldCounter {
            var count = 0
            val tid = Thread.currentThread().id
        }

        private class ThreadLocalHoldCounter : ThreadLocal<HoldCounter>() {
            override fun initialValue(): HoldCounter =
                HoldCounter()
        }
    }

    private class NonfairSync : Sync() {

        override fun writerShouldBlock(): Boolean =
            false

        override fun readerShouldBlock(): Boolean =
            apparentlyFirstQueuedIsExclusive()
    }

    private class ReadLock(lock: ReentrantReadWriteLock) : Lock {

        private val sync: Sync = lock.sync

        override fun lock() {
            sync.acquireShared(1)
        }

        override fun tryLock(): Boolean {
            return sync.tryReadLock()
        }

        override fun lockInterruptibly() {
            TODO("Not yet implemented")
        }

        override fun tryLock(time: Long, unit: TimeUnit): Boolean {
            TODO("Not yet implemented")
        }

        override fun unlock() {
            sync.releaseShared(1)
        }

        override fun newCondition(): Condition {
            throw UnsupportedOperationException()
        }
    }

    private class WriteLock(lock: ReentrantReadWriteLock) : Lock {

        private val sync: Sync = lock.sync

        override fun lock() {
            sync.acquire(1)
        }

        override fun tryLock(): Boolean {
            return sync.tryWriteLock()
        }

        override fun lockInterruptibly() {
            sync.acquireInterruptibly(1)
        }

        override fun tryLock(time: Long, unit: TimeUnit): Boolean {
            return sync.tryAcquireNanos(1, unit.toNanos(time))
        }

        override fun unlock() {
            sync.release(1)
        }

        override fun newCondition(): Condition {
            return sync.newCondition()
        }
    }
}