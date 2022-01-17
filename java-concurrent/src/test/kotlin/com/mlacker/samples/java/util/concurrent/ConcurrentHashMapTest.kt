package com.mlacker.samples.java.util.concurrent

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap

class ConcurrentHashMapTest {

    @Test
    fun `initial capacity`() {
        // 100 / 0.75 = 133.33~ + 1 = 134
        // cap = 256 > 134
        val map = ConcurrentHashMap<String, Long>(100)
        val field = ConcurrentHashMap::class.java.getDeclaredField("sizeCtl")
        field.trySetAccessible()
        val capacity = ConcurrentHashMap::class.java.getDeclaredField("sizeCtl")
            .also { it.trySetAccessible() }
            .get(map)
        assertEquals(256, capacity)
    }
}