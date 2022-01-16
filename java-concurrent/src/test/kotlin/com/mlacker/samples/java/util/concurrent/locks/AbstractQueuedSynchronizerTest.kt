package com.mlacker.samples.java.util.concurrent.locks

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.util.concurrent.CompletableFuture

class AbstractQueuedSynchronizerTest {

    private val lock: Lock = ReentrantLock()

    @BeforeEach
    fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun `1 threads`() {
        val thread = Thread {
            lock.lock()
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(1000)
                } catch (ex: InterruptedException) {
                    println(ex.message)
                }
            }
            lock.unlock()
        }

        thread.start()
    }

    @Test
    fun `2 threads, no concurrent`() {
    }

    @Test
    fun `2 threads, concurrent`() {
    }

    @Test
    fun `3 threads, concurrent`() {
    }

    @Test
    fun `4 threads, 1 await, concurrent`() {
        val condition = lock.newCondition()
        val runnable = Runnable {
            lock.lock()
            Thread.sleep(3000)
            lock.unlock()
        }

        val threads = arrayOf(
            Thread {
                lock.lock()
                condition.await()
                Thread.sleep(3000)
                lock.unlock()
            },
            Thread(runnable),
            Thread(runnable),
            Thread(runnable)
        )
        for (thread in threads) {
            thread.start()
            Thread.sleep(10)
        }

        lock.lock()
        println("main")
        condition.signal()
        lock.unlock()

        threads[0].join()
        println("exit")
    }
}