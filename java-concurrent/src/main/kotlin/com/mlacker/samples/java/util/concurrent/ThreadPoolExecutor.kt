package com.mlacker.samples.java.util.concurrent

import com.mlacker.samples.java.lang.Runnable
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class ThreadPoolExecutor(
    private val corePoolSize: Int,
    private val maximumPoolSize: Int,
    keepAliveTime: Long,
    unit: TimeUnit,
    private val workQueue: BlockingQueue<Runnable>,
    private val threadFactory: ThreadFactory = Executors.defaultThreadFactory(),
    private val handler: RejectedExecutionHandler = ThreadPoolExecutor.AbortPolicy()
) : AbstractExecutorService() {

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
}