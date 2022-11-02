package com.mlacker.samples.core.sort

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

class QuickTest {

    private val sorter: Sort = Quick()

    @Test
    fun `sort 1-5 elements`() {
        assertSort(intArrayOf(1))
        assertSort(intArrayOf(2, 1))
        assertSort(intArrayOf(2, 1, 3))
        assertSort(intArrayOf(2, 3, 1, 2))
        assertSort(intArrayOf(5, 3, 1, 2, 4))
        assertSort(intArrayOf(9, 8, 7, 6, 5, 1, 2, 3, 4))
    }

    @Test
    fun `1k random test 10 times`() {
        for (i in 1..10) {
            assertSort(IntArray(1000) { Random.nextInt() })
        }
    }

    private fun assertSort(numbers: IntArray) {
        val expected = numbers.sortedArray()

        sorter.sort(numbers)

        assertArrayEquals(expected, numbers)
    }

    @Test
    fun `sort empty`() {
        sorter.sort(intArrayOf())
    }

    @Test
    fun `sort sequence`() {
        sorter.sort(intArrayOf(1, 2, 3, 4, 5))
    }
}