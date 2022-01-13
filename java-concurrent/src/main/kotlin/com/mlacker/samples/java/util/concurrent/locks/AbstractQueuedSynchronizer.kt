package com.mlacker.samples.java.util.concurrent.locks

import java.lang.invoke.MethodHandles
import java.lang.invoke.VarHandle
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.LockSupport
import kotlin.jvm.Throws

abstract class AbstractQueuedSynchronizer {

    protected var exclusiveOwnerThread: Thread? = null

    @Volatile
    private var head: Node? = null

    @Volatile
    private var tail: Node? = null

    @Volatile
    protected var state: Int = 0

    protected fun compareAndSetState(expect: Int, update: Int): Boolean {
        return STATE.compareAndSet(this, expect, update)
    }

    fun acquire(acquires: Int) {
        if (!tryAcquire(acquires) &&
            acquireQueued(addWaiter(Node.EXCLUSIVE), acquires)
        ) selfInterrupt()
    }

    @Throws(InterruptedException::class)
    fun acquireInterruptibly(acquires: Int) {
        if (Thread.interrupted()) {
            throw InterruptedException()
        }
        if (!tryAcquire(acquires)) {
            doAcquireInterruptibly(acquires)
        }
    }

    @Throws(InterruptedException::class)
    fun tryAcquireNanos(acquires: Int, nanosTimeout: Long): Boolean {
        if (Thread.interrupted()) {
            throw InterruptedException()
        }
        return tryAcquire(acquires) ||
                doAcquireNanos(acquires, nanosTimeout)
    }

    fun acquireShared(acquires: Int) {
        if (tryAcquireShared(acquires) < 0) {
            doAcquireShared(acquires)
        }
    }

    fun release(acquires: Int): Boolean {
        if (tryRelease(acquires)) {
            doRelease()
            return true
        }
        return false
    }

    fun releaseShared(acquires: Int): Boolean {
        if (tryReleaseShared(acquires)) {
            doReleaseShared()
            return true
        }
        return false
    }

    protected open fun tryAcquire(acquires: Int): Boolean {
        throw UnsupportedOperationException()
    }

    protected open fun tryRelease(releases: Int): Boolean {
        throw UnsupportedOperationException()
    }

    protected open fun tryAcquireShared(acquires: Int): Int {
        throw UnsupportedOperationException()
    }

    protected open fun tryReleaseShared(releases: Int): Boolean {
        throw UnsupportedOperationException()
    }

    protected open fun isHeldExclusively(): Boolean {
        throw UnsupportedOperationException()
    }

    // Creates and enqueues node for current thread and given mode.
    private fun addWaiter(mode: Node?): Node {
        val node = Node(mode)

        while (true) {
            val oldTail = tail
            if (oldTail != null) {
                node.setPrevRelaxed(oldTail)
                if (compareAndSetTail(oldTail, node)) {
                    oldTail.next = node
                    return node
                }
            } else {
                initializeSyncQueue()
            }
        }
    }

    private fun initializeSyncQueue() {
        val h = Node()
        if (HEAD.compareAndSet(this, null, h)) {
            tail = h
        }
    }

    /**
     * Acquires in exclusive uninterruptible mode for thread already in queue.
     * Used by condition wait methods as well as acquire.
     * @return true if interrupted while waiting
     */
    private fun acquireQueued(node: Node, acquires: Int): Boolean {
        var interrupted = false
        try {
            while (true) {
                val p = node.predecessor()
                // if head is released, unpark secondary node and try acquire
                if (p == head && tryAcquire(acquires)) {
                    setHead(node)
                    p.next = null
                    return interrupted
                }
                if (shouldParkAfterFailedAcquire(p, node))
                    interrupted = interrupted || parkAndCheckInterrupt()
            }
        } catch (t: Throwable) {
            cancelAcquire(node)
            if (interrupted)
                selfInterrupt()
            throw t
        }
    }

    private fun setHead(node: Node) {
        head = node
        node.thread = null
        node.prev = null
    }

    /**
     * Checks and updates status for a node that failed to acquire. Returns true if thread should block.
     * This is main signal control in all acquire loops. Requires that pred == node.prev.
     */
    private fun shouldParkAfterFailedAcquire(predecessor: Node, node: Node): Boolean {
        val ws = predecessor.waitStatus
        if (ws == Node.SIGNAL) {
            // This node has already set status asking a release
            // to signal it, so it can safely park.
            return true
        }

        var pred = predecessor
        if (ws > 0) {
            // Predecessor was cancelled. Skip over predecessors and indicate retry
            do {
                pred = pred.prev!!
                node.prev = pred
            } while (pred.waitStatus > 0)
            pred.next = node
        } else {
            /*
             * waitStatus must be 0 or PROPAGATE. Indicate that we
             * need a signal, but don't park yet. Caller will need to
             * retry to make sure it cannot acquire before parking.
             */
            pred.compareAndSetWaitStatus(ws, Node.SIGNAL)
        }
        return false
    }

    private fun parkAndCheckInterrupt(): Boolean {
        LockSupport.park(this)
        return Thread.interrupted()
    }

    @Throws(InterruptedException::class)
    private fun doAcquireInterruptibly(acquires: Int) {
        val node = addWaiter(Node.EXCLUSIVE)
        try {
            while (true) {
                val p = node.predecessor()
                if (p == head && tryAcquire(acquires)) {
                    setHead(node)
                    p.next = null
                    return
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt()
                ) {
                    throw InterruptedException()
                }
            }
        } catch (t: Throwable) {
            cancelAcquire(node)
            throw t
        }
    }

    private fun doAcquireNanos(acquires: Int, nanosTimeout: Long): Boolean {
        var nanosTimeoutL = nanosTimeout
        if (nanosTimeoutL <= 0L)
            return false

        val deadline = System.nanoTime() + nanosTimeoutL
        val node = addWaiter(Node.EXCLUSIVE)
        try {
            while (true) {
                val p = node.predecessor()
                if (p == head && tryAcquire(acquires)) {
                    setHead(node)
                    p.next = null
                    return true
                }
                nanosTimeoutL = deadline - System.nanoTime()
                if (nanosTimeoutL <= 0L) {
                    cancelAcquire(node)
                    return false
                }
                if (shouldParkAfterFailedAcquire(p, node) && nanosTimeoutL > SPIN_FOR_TIMEOUT_THRESHOLD) {
                    LockSupport.parkNanos(this, nanosTimeoutL)
                }
                if (Thread.interrupted()) {
                    throw InterruptedException()
                }
            }
        } catch (t: Throwable) {
            cancelAcquire(node)
            throw t
        }
    }

    private fun doAcquireShared(acquires: Int) {
        val node = addWaiter(Node.SHARED)
        var interrupted = false
        try {
            while (true) {
                val p = node.predecessor()
                if (p == head) {
                    val r = tryAcquireShared(acquires)
                    if (r >= 0) {
                        setHeadAndPropagate(node, r)
                        p.next = null
                        return
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node)) {
                    interrupted = interrupted || parkAndCheckInterrupt()
                }
            }
        } catch (t: Throwable) {
            cancelAcquire(node)
            throw t
        } finally {
            if (interrupted) {
                selfInterrupt()
            }
        }
    }

    private fun setHeadAndPropagate(node: Node, propagate: Int) {
        val h = head
        setHead(node)

        /*
         * Try to signal next queued node if:
         *   Propagation was indicated by caller,
         *     or was recorded (as h.waitStatus either before
         *     or after setHead) by a previous operation
         *     (note: this uses sign-check of waitStatus because
         *      PROPAGATE status may transition to SIGNAL.)
         *   and
         *     The next node is waiting in shared mode,
         *       or we don't know, because it appears null
         */
        val h2 = head
        if (propagate > 0 || h == null || h.waitStatus < 0 || h2 == null || h2.waitStatus < 0) {
            val s = node.next
            if (s == null || s.isShared()) {
                doReleaseShared()
            }
        }
    }

    private fun doRelease() {
        val h = head
        if (h != null && h.waitStatus != 0) {
            unparkSuccessor(h)
        }
    }

    private fun doReleaseShared() {
        while (true) {
            val h = head
            if (h != null && h != tail) {
                val ws = h.waitStatus
                if (ws == Node.SIGNAL) {
                    if (!h.compareAndSetWaitStatus(Node.SIGNAL, 0)) {
                        continue
                    }
                    unparkSuccessor(h)
                } else if (ws == 0 && !h.compareAndSetWaitStatus(0, Node.PROPAGATE)) {
                    continue
                }
            }
            if (h == head) {
                break
            }
        }
    }

    // Wakes up node's successor, if one exists.
    private fun unparkSuccessor(node: Node) {
        val ws = node.waitStatus
        if (ws < 0) {
            node.compareAndSetWaitStatus(ws, 0)
        }

        var s = node.next
        if (s == null || s.waitStatus > 0) {
            s = null

            var p = tail
            while (p != node && p != null) {
                if (p.waitStatus <= 0) {
                    s = p
                }

                p = p.prev
            }
        }

        if (s != null) {
            LockSupport.unpark(s.thread)
        }
    }

    private fun cancelAcquire(node: Node) {
        node.thread = null

        var pred = node.prev!!
        while (pred.waitStatus > 0) {
            pred = pred.prev!!
            node.prev = pred
        }

        val predNext = pred.next!!

        node.waitStatus = Node.CANCELLED

        if (node == tail && compareAndSetTail(node, pred)) {
            pred.compareAndSetNext(predNext, null)
        } else {
            val ws = pred.waitStatus
            if (pred != head &&
                (ws == Node.SIGNAL || (ws <= 0 && pred.compareAndSetWaitStatus(ws, Node.SIGNAL))) &&
                pred.thread != null
            ) {
                val next = node.next
                if (next != null && next.waitStatus <= 0) {
                    pred.compareAndSetNext(predNext, next)
                }
            } else {
                unparkSuccessor(node)
            }

            node.next = node
        }
    }

    private fun compareAndSetTail(expect: Node, update: Node): Boolean {
        return TAIL.compareAndSet(this, expect, update)
    }

    fun hasQueuedThreads(): Boolean {
        var p = tail
        val h = head
        while (p != h && p != null) {
            if (p.waitStatus <= 0) {
                return true
            }
            p = p.prev
        }
        return false
    }

    internal fun transferAfterCancelledWait(node: Node): Boolean {
        if (node.compareAndSetWaitStatus(Node.CONDITION, 0)) {
            enq(node)
            return true
        }
        while (!isOnSyncQueue(node)) {
            Thread.yield()
        }
        return false
    }

    private fun enq(node: Node): Node {
        while (true) {
            val oldTail = tail
            if (oldTail != null) {
                node.setPrevRelaxed(oldTail)
                if (compareAndSetTail(oldTail, node)) {
                    oldTail.next = node
                    return oldTail
                }
            } else {
                initializeSyncQueue()
            }
        }
    }

    internal fun isOnSyncQueue(node: Node): Boolean {
        if (node.waitStatus == Node.CONDITION || node.prev == null)
            return false
        if (node.next != null)
            return true
        return findNodeFromTail(node)
    }

    private fun findNodeFromTail(node: Node): Boolean {
        var p = tail
        while (true) {
            if (p == node)
                return true
            if (p == null)
                return false
            p = p.prev
        }
    }

    internal fun fullyRelease(node: Node): Int {
        try {
            val savedState = state
            if (release(savedState)) {
                return savedState
            }
            throw IllegalMonitorStateException()
        } catch (t: Throwable) {
            node.waitStatus = Node.CANCELLED
            throw t
        }
    }

    protected fun apparentlyFirstQueuedIsExclusive(): Boolean {
        val h = head
        val s = head?.next
        return h != null &&
                s != null &&
                !s.isShared() &&
                s.thread != null
    }

    companion object {

        private val STATE: VarHandle
        private val HEAD: VarHandle
        private val TAIL: VarHandle

        const val SPIN_FOR_TIMEOUT_THRESHOLD: Long = 1000L

        init {
            val lookup = MethodHandles.lookup()
            STATE = lookup.findVarHandle(AbstractQueuedSynchronizer::class.java, "state", Int::class.java)
            HEAD = lookup.findVarHandle(AbstractQueuedSynchronizer::class.java, "head", Node::class.java)
            TAIL = lookup.findVarHandle(AbstractQueuedSynchronizer::class.java, "tail", Node::class.java)
        }

        fun selfInterrupt() {
            Thread.currentThread().interrupt()
        }
    }

    class Node {

        @Volatile
        internal var waitStatus: Int = 0

        @Volatile
        internal var prev: Node? = null

        @Volatile
        internal var next: Node? = null

        @Volatile
        internal var thread: Thread? = null

        internal var nextWaiter: Node? = null

        fun isShared(): Boolean = nextWaiter == SHARED

        fun predecessor(): Node {
            return prev ?: throw NullPointerException()
        }

        constructor()
        constructor(nextWaiter: Node?) {
            this.nextWaiter = nextWaiter
            THREAD.set(this, Thread.currentThread())
        }

        constructor(waitStatus: Int) {
            WAITSTATUS.set(this, waitStatus)
            THREAD.set(this, Thread.currentThread())
        }

        fun compareAndSetWaitStatus(expect: Int, update: Int): Boolean {
            return WAITSTATUS.compareAndSet(this, expect, update)
        }

        fun compareAndSetNext(expect: Node, update: Node?): Boolean {
            return NEXT.compareAndSet(this, expect, update)
        }

        fun setPrevRelaxed(p: Node) {
            PREV.set(this, p)
        }

        companion object {
            val SHARED: Node = Node()
            val EXCLUSIVE: Node? = null

            const val CANCELLED: Int = 1
            const val SIGNAL: Int = -1
            const val CONDITION: Int = -2
            const val PROPAGATE: Int = -3

            private val NEXT: VarHandle
            private val PREV: VarHandle
            private val THREAD: VarHandle
            private val WAITSTATUS: VarHandle

            init {
                val lookup = MethodHandles.lookup()
                NEXT = lookup.findVarHandle(Node::class.java, "next", Node::class.java)
                PREV = lookup.findVarHandle(Node::class.java, "prev", Node::class.java)
                THREAD = lookup.findVarHandle(Node::class.java, "thread", Thread::class.java)
                WAITSTATUS = lookup.findVarHandle(Node::class.java, "waitStatus", Int::class.java)
            }
        }
    }

    inner class ConditionObject : Condition {

        private var firstWaiter: Node? = null
        private var lastWaiter: Node? = null

        private fun addConditionWaiter(): Node {
            if (!isHeldExclusively()) {
                throw IllegalMonitorStateException()
            }
            var t = lastWaiter
            if (t != null && t.waitStatus != Node.CONDITION) {
                unlinkCancelledWaiters()
                t = lastWaiter
            }

            val node = Node(Node.CONDITION)

            if (t == null) {
                firstWaiter = node
            } else {
                t.nextWaiter = node
            }
            lastWaiter = node
            return node
        }

        private fun doSignal(_first: Node) {
            var first: Node? = _first
            do {
                firstWaiter = first!!.nextWaiter
                if (firstWaiter == null) {
                    lastWaiter = null
                }
                first.nextWaiter = null

                if (!transferForSignal(first)) {
                    first = firstWaiter
                } else {
                    break
                }
            } while (first != null)
        }

        private fun transferForSignal(node: Node): Boolean {
            if (!node.compareAndSetWaitStatus(Node.CONDITION, 0))
                return false

            val p = enq(node)
            val ws = p.waitStatus
            if (ws > 0 || !p.compareAndSetWaitStatus(ws, Node.SIGNAL))
                LockSupport.unpark(node.thread)
            return true
        }

        private fun doSignalAll(first: Node) {
            firstWaiter = null
            lastWaiter = null

            var _first: Node? = first
            while (_first != null) {
                val next = _first.nextWaiter
                first.nextWaiter = null
                transferForSignal(first)
                _first = next
            }
        }

        private fun unlinkCancelledWaiters() {
            var t = firstWaiter
            var trail: Node? = null
            while (t != null) {
                val next = t.nextWaiter
                if (t.waitStatus != Node.CONDITION) {
                    t.nextWaiter = null
                    if (trail == null) {
                        firstWaiter = next
                    } else {
                        trail.nextWaiter = next
                    }
                    if (next == null) {
                        lastWaiter = trail
                    }
                } else {
                    trail = t
                }
                t = next
            }
        }

        override fun signal() {
            if (!isHeldExclusively()) {
                throw IllegalMonitorStateException()
            }
            val first = firstWaiter
            if (first != null) {
                doSignal(first)
            }
        }

        override fun signalAll() {
            if (!isHeldExclusively()) {
                throw IllegalMonitorStateException()
            }
            val first = firstWaiter
            if (first != null) {
                doSignalAll(first)
            }
        }

        override fun await() {
            if (Thread.interrupted()) {
                throw InterruptedException()
            }
            val node = addConditionWaiter()
            val savedState = fullyRelease(node)
            var interruptMode = 0
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this)
                interruptMode = checkInterruptWhileWaiting(node)
                if (interruptMode != 0) {
                    break
                }
            }
            if (acquireQueued(node, savedState) && interruptMode != -1) {
                interruptMode = 1
            }
            if (node.nextWaiter != null) {
                unlinkCancelledWaiters()
            }
            if (interruptMode != 0) {
                reportInterruptAfterWait(interruptMode)
            }
        }

        private fun checkInterruptWhileWaiting(node: Node): Int {
            return if (Thread.interrupted())
                if (transferAfterCancelledWait(node))
                    -1
                else 1
            else 0
        }

        private fun reportInterruptAfterWait(interruptMode: Int) {
            if (interruptMode == -1) {
                throw InterruptedException()
            } else if (interruptMode == 1) {
                selfInterrupt()
            }
        }

        override fun await(time: Long, unit: TimeUnit?): Boolean {
            TODO("Not yet implemented")
        }

        override fun awaitUninterruptibly() {
            TODO("Not yet implemented")
        }

        override fun awaitNanos(nanosTimeout: Long): Long {
            TODO("Not yet implemented")
        }

        override fun awaitUntil(deadline: Date): Boolean {
            TODO("Not yet implemented")
        }
    }
}