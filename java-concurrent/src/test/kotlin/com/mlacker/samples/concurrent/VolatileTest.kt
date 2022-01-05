package com.mlacker.samples.concurrent

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

class VolatileTest {

    @Volatile
    private var state = 1
    private val arr = arrayOf(1)

    @Test
    fun `sync array memory`() {
        Thread {
            Thread.yield()
            arr[0] = 0
            state = 0
        }.start()

        val futureTask = FutureTask {
            var count = 0
            while (arr[0] != 0) {
                if (state == 0) {
                    println("cache refresh")
                }
                count++
            }
            println("done $count")
        }.also { Thread(it).start() }

        futureTask.get(5, TimeUnit.SECONDS)
    }
}