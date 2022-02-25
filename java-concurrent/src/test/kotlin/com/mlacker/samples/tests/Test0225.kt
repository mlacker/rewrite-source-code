package com.mlacker.samples.tests

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random

class Test0225 {

    @Test
    fun `quick sort test`() {
        val size = 100
        val numbers =
            Array(size) { Random.nextInt(size) }
        val expected = numbers.sortedArray()

        quickSort(numbers, 0, numbers.size - 1)

        assertArrayEquals(expected, numbers)
    }

    private fun quickSort(numbers: Array<Int>, min: Int, max: Int) {
        if (min >= max) {
            return
        }

        var i = min
        var j = max
        val key = numbers[i]

        while (i < j) {
            while (i < j && numbers[j] > key) {
                j--
            }
            numbers[i] = numbers[j]

            while (i < j && numbers[i] <= key) {
                i++
            }

            numbers[j] = numbers[i]
        }
        numbers[i] = key

        quickSort(numbers, min, i - 1)
        quickSort(numbers, i + 1, max)
    }
}