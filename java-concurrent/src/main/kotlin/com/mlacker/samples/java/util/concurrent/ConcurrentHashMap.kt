@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE", "UNCHECKED_CAST")

package com.mlacker.samples.java.util.concurrent

import com.mlacker.samples.java.util.AbstractMap
import com.mlacker.samples.java.util.Map
import jdk.internal.misc.Unsafe
import jdk.internal.vm.annotation.Contended

class ConcurrentHashMap<K, V>(
    initialCapacity: Int = 16,
    loadFactor: Float = LOAD_FACTOR,
    concurrencyLevel: Int = 1,
) : AbstractMap<K, V>(), ConcurrentMap<K, V> {

    @Volatile
    private var table: Array<Node<K, V>?>

    @Volatile
    private var sizeCtl: Int

    @Volatile
    private var baseCount: Long = 0

    @Volatile
    private var counterCells: Array<CounterCell> = emptyArray()

    init {
        val size = (initialCapacity / loadFactor + 1.0).toInt()
        val cap = tableSizeFor(size)

        table = arrayOfNulls(cap)
        this.sizeCtl = cap - (cap ushr 2)
    }

    override val size: Int
        get() = maxOf(sumCount().toInt(), 0)

    override val isEmpty: Boolean
        get() = sumCount() <= 0

    private fun sumCount(): Long {
        return baseCount + counterCells.sumOf { it.value }
    }

    override fun containsKey(key: K): Boolean {
        return get(key) != null
    }

    override fun get(key: K): V? {
        val tab = table
        val length = tab.size
        val hash = spread(key.hashCode())
        var element: Node<K, V>? = tabAt(tab, hash and (length - 1)) ?: return null

        if (element!!.hash == hash) {
            if (element.key == key)
                return element.value
        } else if (element.hash < 0)
            return element.find(hash, key)?.value

        element = element.next
        while (element != null) {
            if (element.hash == hash && element.key == key)
                return element.value
            element = element.next
        }

        return null
    }

    override fun put(key: K, value: V): V? {
        return putVal(key, value)
    }

    private fun putVal(key: K, value: V): V? {
        val hash = spread(key.hashCode())
        var binCount = 0
        var tab = table
        while (true) {
            val n = tab.size
            val i = hash and (n - 1)
            val f = tabAt(tab, i)
            if (f == null) {
                // spin cas
                if (casTabAt(tab, i, null, Node(hash, key, value)))
                    break
            } else if (f.hash == MOVED) {
                tab = helpTransfer(tab, f)
            } else {
                var oldVal: V? = null
                synchronized(f) {
                    if (tabAt(tab, i) == f) {
                        if (f.hash >= 0) {
                            binCount = 1
                            var e: Node<K, V> = f
                            while (true) {
                                if (e.hash == hash && e.key == key) {
                                    oldVal = e.value
                                    e.value = value
                                    break
                                }
                                if (e.next == null) {
                                    e.next = Node(hash, key, value)
                                    break
                                }
                                e = e.next!!
                                binCount++
                            }
                        } else if (f is TreeBin) {
                            val p: Node<K, V>?
                            binCount = 2
                            p = f.putTreeVal(hash, key, value)
                            if (p != null) {
                                oldVal = p.value
                                p.value = value
                            }
                        } else if (f is ReservationNode) {
                            throw IllegalStateException("Recursive update")
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i)
                    if (oldVal != null)
                        return oldVal
                    break
                }
            }
        }
        addCount(1L, binCount)
        return null
    }

    // 2316
    private fun addCount(x: Long, check: Int) {
        TODO("Not yet implemented")
    }

    // 2357
    private fun helpTransfer(tab: Array<Node<K, V>?>, f: Node<K, V>): Array<Node<K, V>?> {
        TODO("Not yet implemented")
    }

    // 2657
    private fun treeifyBin(tab: Array<Node<K, V>?>, i: Int) {
        TODO("Not yet implemented")
    }

    override fun remove(key: K): V? {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

    companion object {
        private const val MAXIMUM_CAPACITY = 1 shl 30
        private const val DEFAULT_CAPACITY = 16
        private const val MAX_ARRAY_SIZE = Int.MAX_VALUE - 8
        private const val DEFAULT_CONCURRENCY_LEVEL = 16
        private const val LOAD_FACTOR = 0.75f
        private const val TREEIFY_THRESHOLD = 8
        private const val UNTREEIFY_THRESHOLD = 6
        private const val MIN_TREEIFY_CAPACITY = 64
        private const val MIN_TRANSFER_STRIDE = 16

        private const val MOVED = -1
        private const val TREEBIN = -2
        private const val RESERVED = -3
        private const val HASH_BITS = 0x7fffffff

        private val U = Unsafe.getUnsafe()
        private val SIZECTL = U.objectFieldOffset(ConcurrentHashMap::class.java, "sizeCtl")
        private val ABASE = U.arrayBaseOffset(Array::class.java).toLong()
    }

    private fun spread(hash: Int): Int {
        return (hash or (hash ushr 16)) and HASH_BITS
    }

    /**
     * Returns a power of two table size for the given desired capacity.
     */
    private fun tableSizeFor(c: Int): Int {
        val n = -1 ushr Integer.numberOfLeadingZeros(c - 1)
        return minOf(n + 1, MAXIMUM_CAPACITY)
    }

    /*
     * Atomic access methods are used for table elements as well as
     * elements of in-progress next table while resizing.
     */

    private fun tabAt(tab: Array<Node<K, V>?>, i: Int): Node<K, V>? {
        return U.getObjectAcquire(tab, ABASE + i) as Node<K, V>?
    }

    private fun casTabAt(tab: Array<Node<K, V>?>, i: Int, c: Node<K, V>?, v: Node<K, V>): Boolean {
        return U.compareAndSetObject(tab, ABASE + i, c, v)
    }

    private fun setTabAt(tab: Array<Node<K, V>>, i: Int, v: Node<K, V>) {
        U.putObjectRelease(tab, ABASE + i, v)
    }

    // 625
    private open class Node<K, V>(
        val hash: Int,
        override val key: K,
        @Volatile
        override var value: V,
        @Volatile
        var next: Node<K, V>? = null
    ) : Map.Entry<K, V> {
        fun find(h: Int, k: K): Node<K, V>? {
            var e: Node<K, V>? = this
            while (e != null) {
                if (e.hash == h && e.key == k)
                    return e
                e = e.next
            }
            return null
        }
    }

    /**
     * Nodes for use in TreeBins.
     * @see #2704
     */
    private class TreeNode<K, V>(hash: Int, key: K, value: V, next: Node<K, V>?, parent: Node<K, V>?) :
        Node<K, V>(hash, key, value, next) {
        // red-block tree links
        var parent = parent
        var left: Node<K, V>? = null
        var right: Node<K, V>? = null
        var prev: Node<K, V>? = null
        var red: Boolean = false
    }

    /**
     * TreeNodes used at the heads of bins. TreeBins do not hold user
     * keys or values, but instead point to list of TreeNodes and
     * their root. They also maintain a parasitic read-write lock
     * forcing writers (who hold bin lock) to wait for readers (who do
     * not) to complete before tree restructuring operations.
     * @see #2764
     */
    private class TreeBin<K, V>(hash: Int, key: K, value: V, next: Node<K, V>? = null) :
        Node<K, V>(hash, key, value, next) {

        fun putTreeVal(hash: Int, key: K, value: V): TreeNode<K, V>? {
            TODO("Not yet implemented")
        }
    }

    /**
     * A place-holder node used in computeIfAbsent and compute.
     * @see #2260
     */
    private class ReservationNode<K, V> : Node<K, V>(RESERVED, Any() as K, Any() as V)

    /**
     * A padded cell for distributing counts. Adapted from LongAdder and Striped64.
     * @see #2557
     */
    @Contended
    private class CounterCell(@Volatile var value: Long)
}