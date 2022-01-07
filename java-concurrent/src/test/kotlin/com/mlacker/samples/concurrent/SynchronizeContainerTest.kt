package com.mlacker.samples.concurrent

import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
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
        val executor = Executors.newCachedThreadPool()
        val container: Container<Int> = SynchronizeContainer(10)

        val producers = listOf(0..100, 100..200)
            .map { range ->
                executor.submit {
                    for (i in range) {
                        container.put(i)
                    }
                }
            }
        val consumers = (1..10).map {
            executor.submit(Callable {
                var sum = 0
                while (true) {
                    sum += container.get()
                }
                sum
            })
        }
    }
}