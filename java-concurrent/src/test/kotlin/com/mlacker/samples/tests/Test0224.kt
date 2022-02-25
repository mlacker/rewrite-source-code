package com.mlacker.samples.tests

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.lang.StringBuilder
import java.time.Duration
import kotlin.random.Random

class Test0224 {

    @Test
    fun test1() {
        val input = "lsdfkddasdf"
        val expected = "lsdfka"

        val builder = StringBuilder()
        input.forEach {
            if (!builder.contains(it))
                builder.append(it)
        }
        val result = builder.toString()

        assertEquals(expected, result)
    }

    @Test
    fun test2() {
        val input = "12asdf"
        val expected = "fdsa21"

        val result = String(input.toCharArray().reversedArray())

        assertEquals(expected, result)
    }

    @Volatile
    private var buffer: CharArray = "hello1".toCharArray()

    @Test
    fun test3() {
        val minutes1 = Duration.ofMinutes(1).toMillis()
        val millis100 = 100L
        for (i in 1..3) {
            Thread {
                while (true) {
                    Thread.sleep(minutes1)
                    buffer = String(Random.nextBytes(8)).toCharArray()
                }
            }.start()
            Thread.sleep(Random.nextLong(minutes1))
        }
        for (i in 1..10) {
            Thread {
                while (true) {
                    Thread.sleep(millis100)
                    println(buffer.concatToString())
                }
            }.start()
            Thread.sleep(Random.nextLong(minutes1))
        }

        Thread.sleep(minutes1 * 10)
    }
}