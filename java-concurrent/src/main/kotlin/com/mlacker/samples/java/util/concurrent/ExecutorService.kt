package com.mlacker.samples.java.util.concurrent

import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

interface ExecutorService : Executor {

    // Initiates an orderly shutdown in which previously submitted
    // tasks are executed, but no new tasks will be accepted.
    fun shutdown()

    fun shutdownNow(): List<Runnable>

    fun isShutdown(): Boolean

    fun isTerminated(): Boolean

    @Throws(InterruptedException::class)
    fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean

    fun <T> submit(task: Callable<T>): Future<T>
}

