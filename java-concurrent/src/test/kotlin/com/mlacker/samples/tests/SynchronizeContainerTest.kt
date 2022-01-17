package com.mlacker.samples.tests

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.LongAdder
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class SynchronizeContainerTest {

    // 写一个固定容量的同步容器，拥有put和get方法，以及getCount方法，能够支持2个生产者线程以及10个消费者线程的阻塞调用
    interface Container<E> {
        fun put(element: E)

        fun get(): E

        fun getCount(): Int
    }

    class SynchronizeContainer<E>(
        private val capacity: Int
    ) : Container<E> {

        private val elements: LinkedList<E> = LinkedList()
        private val lock: Lock = ReentrantLock()
        private val producer = lock.newCondition()
        private val consumer = lock.newCondition()

        override fun put(element: E) {
            try {
                lock.lock()

                while (elements.size == capacity) {
                    producer.await()
                }

                elements.add(element)
                consumer.signal()
            } finally {
                lock.unlock()
            }
        }

        override fun get(): E {
            try {
                lock.lock()

                while (elements.isEmpty()) {
                    consumer.await()
                }

                val element = elements.poll()
                producer.signal()
                return element
            } finally {
                lock.unlock()
            }
        }

        override fun getCount(): Int {
            return elements.size
        }
    }

    @Test
    fun `container test`() {
        val container: Container<Int> = SynchronizeContainer(10)
        val countDownLatch: CountDownLatch

        val producers = listOf(1..100, 101..200)
            .also {
                countDownLatch = CountDownLatch(it.size)
            }
            .map { range ->
                Thread {
                    for (i in range) {
                        container.put(i)
                    }
                    countDownLatch.countDown()
                }
            }
            .onEach { it.start() }

        val sum = LongAdder()
        val consumers = (1..10)
            .map {
                Thread {
                    try {
                        while (true) {
                            sum.add(container.get().toLong())
                        }
                    } catch (ex: InterruptedException) {
                        Thread.interrupted()
                    }
                }
            }
            .onEach { it.start() }
            .also { countDownLatch.await() }
            .onEach { it.interrupt() }

        assertEquals(200 * (200 + 1) / 2, sum.sum())
    }
}