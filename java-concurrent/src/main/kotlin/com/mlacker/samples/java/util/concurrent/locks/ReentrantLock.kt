package com.mlacker.samples.java.util.concurrent.locks

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition

class ReentrantLock : Lock {

    private val sync = NonfairSync()

    override fun lock() {
        sync.acquire(1)
    }

    override fun lockInterruptibly() {
        sync.acquireInterruptibly(1)
    }

    override fun tryLock(): Boolean {
        return sync.nonfairTryAcquire(1)
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

    class NonfairSync : AbstractQueuedSynchronizer() {

        fun nonfairTryAcquire(acquires: Int): Boolean {
            val current = Thread.currentThread()
            val c = state
            if (c == 0) {
                if (compareAndSetState(0, acquires)) {
                    exclusiveOwnerThread = current
                    return true
                }
            } else if (current == exclusiveOwnerThread) {
                val nextc = c + acquires
                if (nextc < 0)
                    throw Error("Maximum lock count exceeded")
                state = nextc
                return true
            }
            return false
        }

        override fun tryAcquire(acquires: Int): Boolean {
            return nonfairTryAcquire(acquires)
        }

        override fun tryRelease(releases: Int): Boolean {
            val c = state - releases
            if (Thread.currentThread() != exclusiveOwnerThread)
                throw IllegalMonitorStateException()
            var free = false
            if (c == 0) {
                free = true
                exclusiveOwnerThread = null
            }
            state = c
            return free
        }

        override fun isHeldExclusively(): Boolean {
            return exclusiveOwnerThread == Thread.currentThread()
        }

        fun newCondition(): ConditionObject {
            return ConditionObject()
        }

        fun getOwner(): Thread? {
            return if (state != 0) exclusiveOwnerThread else null
        }

        fun getHoldCount(): Int {
            return if (isHeldExclusively()) state else 0
        }

        fun isLocked(): Boolean {
            return state != 0
        }
    }
}