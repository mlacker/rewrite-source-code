package com.mlacker.samples.java.util.concurrent

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit

class Executors {

    companion object {

        fun newFixedThreadPool(nThreads: Int): ExecutorService {
            return ThreadPoolExecutor(
                nThreads, nThreads,
                0, TimeUnit.MILLISECONDS, LinkedBlockingQueue()
            )
        }

        fun newSingleThreadExecutor(): ThreadPoolExecutor {
            return ThreadPoolExecutor(
                1, 1,
                0L, TimeUnit.MILLISECONDS, LinkedBlockingQueue()
            )
        }

        fun newCachedThreadPool(): ThreadPoolExecutor {
            return ThreadPoolExecutor(
                0, Int.MAX_VALUE,
                60L, TimeUnit.SECONDS, SynchronousQueue()
            )
        }

        fun newSingleThreadScheduledExecutor(): ScheduledExecutorService {
            return ScheduledThreadPoolExecutor(0)
        }

        fun newScheduledThreadPool(corePoolSize: Int): ScheduledExecutorService {
            return ScheduledThreadPoolExecutor(corePoolSize)
        }
    }
}