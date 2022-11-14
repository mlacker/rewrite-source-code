package com.mlacker.samples.java.util.concurrent

import com.mlacker.samples.java.util.concurrent.atomic.AtomicInteger
import com.mlacker.samples.java.util.concurrent.locks.AbstractQueuedSynchronizer
import com.mlacker.samples.java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition

class ThreadPoolExecutor(
    private val corePoolSize: Int,
    private val maximumPoolSize: Int,
    keepAliveTime: Long,
    unit: TimeUnit,
    private val workQueue: BlockingQueue<Runnable>,
    private val threadFactory: ThreadFactory = Executors.defaultThreadFactory(),
    private val handler: RejectedExecutionHandler = ThreadPoolExecutor.AbortPolicy()
) : AbstractExecutorService() {

    private val ctl: AtomicInteger = AtomicInteger(ctlOf(RUNNING, 0))
    private val mainLock: ReentrantLock = ReentrantLock()
    private val workers: HashSet<Worker> = HashSet()
    private val termination: Condition = mainLock.newCondition()
    private var completedTaskCount: Long = 0

    @Volatile
    private var keepAliveTime: Long = unit.toNanos(keepAliveTime)

    override fun execute(command: Runnable) {
        TODO("Not yet implemented")
    }

    override fun shutdown() {
        TODO("Not yet implemented")
    }

    override fun shutdownNow(): List<Runnable> {
        TODO("Not yet implemented")
    }

    override fun isShutdown(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isTerminated(): Boolean {
        TODO("Not yet implemented")
    }

    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
        TODO("Not yet implemented")
    }

    private fun runWorker(worker: Worker) {
        TODO("Not yet implemented")
    }

    companion object {
        private const val COUNT_BITS = Int.SIZE_BITS - 3
        private const val COUNT_MASK = (1 shl COUNT_BITS) - 1

        private const val RUNNING = -1 shl COUNT_BITS
        private const val SHUTDOWN = 0 shl COUNT_BITS
        private const val STOP = 1 shl COUNT_BITS
        private const val TIDYING = 2 shl COUNT_BITS
        private const val TERMINATED = 3 shl COUNT_BITS

        private fun runStateOf(c: Int) = c and COUNT_MASK.inv()
        private fun workerCountOf(c: Int) = c and COUNT_MASK
        private fun ctlOf(rs: Int, wc: Int) = rs or wc
    }

    private inner class Worker(private val firstTask: Runnable) : AbstractQueuedSynchronizer(), Runnable {

        private val thread: Thread = threadFactory.newThread(this)
        @Volatile
        private var completedTasks: Long = 0

        override fun run() {
            runWorker(this)
        }

        override fun isHeldExclusively(): Boolean =
            state != 0

        override fun tryAcquire(acquires: Int): Boolean {
            if (compareAndSetState(0, 1)) {
                exclusiveOwnerThread = Thread.currentThread()
                return true
            }
            return false
        }

        override fun tryRelease(releases: Int): Boolean {
            exclusiveOwnerThread = null
            state = 0
            return true
        }

        fun lock() = acquire(1)
        fun tryLock(): Boolean = tryAcquire(1)
        fun unlock() = release(1)
        fun isLocked(): Boolean = isHeldExclusively()

        private fun interruptIfStarted() {
            if (state >= 0 && !thread.isInterrupted) {
                thread.interrupt()
            }
        }
    }
}