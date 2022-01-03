package com.mlacker.samples.java.util.concurrent

import java.util.concurrent.Future

abstract class AbstractExecutorService : ExecutorService {

    protected open fun <T> newTaskFor(callable: Callable<T>): RunnableFuture<T> {
        return FutureTask(callable)
    }

    override fun <T> submit(task: Callable<T>): Future<T> {
        return newTaskFor(task).also {
            execute(it)
        }
    }
}