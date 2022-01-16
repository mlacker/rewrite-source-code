package com.mlacker.samples.java.util

import kotlin.math.min

class HashMap<K, V> : AbstractMap<K, V>, Map<K, V> {

    companion object {
        private const val DEFAULT_INITIAL_CAPACITY = 1 shl 4
        private const val MAXIMUM_CAPACITY = 1 shl 30
        private const val DEFAULT_LOAD_FACTOR = 0.75f
        private const val TREEIFY_THRESHOLD = 8
        private const val UNTREEIFY_THRESHOLD = 6
        private const val MIN_TREEIFY_CAPACITY = 64
    }

    private var table: Array<Node<K, V>?>? = null
    override var size: Int = 0
        private set
    private var modCount: Int = 0
    private var threshold: Int
    private val loadFactor: Float

    constructor() : this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR)
    constructor(initialCapacity: Int) : this(initialCapacity, DEFAULT_LOAD_FACTOR)
    constructor(initialCapacity: Int, loadFactor: Float) {
        this.loadFactor = loadFactor
        this.threshold = tableSizeFor(initialCapacity)
    }

    private fun hash(key: K): Int {
        val h = key?.hashCode() ?: return 0
        return h xor (h ushr 16)
    }

    private fun tableSizeFor(cap: Int): Int {
        val n = -1 ushr Integer.numberOfLeadingZeros(cap - 1)
        return if (n < 0) 1 else min(MAXIMUM_CAPACITY, n + 1)
    }

    override fun get(key: K): V? {
        return getNode(hash(key), key)?.value
    }

    private fun getNode(hash: Int, key: K): Node<K, V>? {
        val tab = table
        val n: Int? = tab?.size
        val first: Node<K, V>? = tab?.get((n!! - 1) and hash)
        if (tab != null && n!! > 0 && first != null) {
            if (first.hash == hash && first.key == key)
                return first
            var e = first.next
            if (e != null) {
                if (first is TreeNode)
                    return first.getTreeNode(hash, key)
                do {
                    if (e!!.hash == hash && e.key == key)
                        return e
                    e = e.next
                } while (e != null)
            }
        }
        return null
    }

    override fun containsKey(key: K): Boolean {
        return getNode(hash(key), key) != null
    }

    override fun put(key: K, value: V): V? {
        return putVal(hash(key), key, value)
    }

    private fun putVal(hash: Int, key: K, value: V): V? {
        var tab = table
        var n: Int = tab?.size ?: 0
        if (tab.isNullOrEmpty()) {
            // initial table
            tab = resize()
            n = tab.size
        }

        // compute bucket index by hash & (size - 1)
        val i = (n - 1) and hash
        var p = tab[i]

        if (p == null)
        // set bucket
            tab[i] = Node(hash, key, value, null)
        else {
            var e: Node<K, V>?

            // check first node
            if (p.hash == hash && p.key == key)
                e = p
            else if (p is TreeNode)
                e = p.putTreeVal(this, tab, hash, key, value)
            else {
                var binCount = 0
                while (true) {
                    e = p!!.next
                    if (e == null) {
                        p.next = Node(hash, key, value, null)
                        if (binCount >= TREEIFY_THRESHOLD - 1)
                            treeifyBin(tab, hash)
                        break
                    }
                    if (e.hash == hash && e.key == key)
                        break
                    p = e
                    binCount++
                }
            }

            if (e != null) {
                // change value
                val oldValue = e.value
                e.value = value
                return oldValue
            }
        }
        ++modCount
        if (++size > threshold)
        // resize
            resize()
        return null
    }

    private fun resize(): Array<Node<K, V>?> {
        val oldTab = table
        val oldCap = oldTab?.size ?: 0
        val oldThr = threshold
        val newCap: Int
        var newThr = 0
        if (oldCap > 0) {
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Int.MAX_VALUE
                return oldTab!!
            } else if ((oldCap shl 1).also { newCap = it } < MAXIMUM_CAPACITY &&
                oldCap >= DEFAULT_INITIAL_CAPACITY)
                newThr = oldThr shl 1
        } else if (oldThr > 0)
            newCap = oldThr
        else {
            newCap = DEFAULT_INITIAL_CAPACITY
            newThr = (DEFAULT_INITIAL_CAPACITY * DEFAULT_LOAD_FACTOR).toInt()
        }
        if (newThr == 0) {
            newThr = (newCap * loadFactor).toInt()
        }
        threshold = newThr

        val newTab = arrayOfNulls<Node<K, V>>(newCap)
        table = newTab
        if (oldTab != null) {
            for (j in 0 until oldCap) {
                var e = oldTab[j]
                if (e != null) {
                    oldTab[j] = null
                    if (e.next == null)
                        newTab[e.hash and (newCap - 1)] = e
                    else if (e is TreeNode)
                        e.split(this, newTab, j, oldCap)
                    else {
                        var loHead: Node<K, V>? = null
                        var loTail: Node<K, V>? = null
                        var hiHead: Node<K, V>? = null
                        var hiTail: Node<K, V>? = null
                        var next: Node<K, V>?
                        do {
                            next = e!!.next
                            if (e.hash and oldCap == 0) {
                                if (loTail == null) loHead = e else loTail.next = e
                                loTail = e
                            } else {
                                if (hiTail == null) hiHead = e else hiTail.next = e
                                hiTail = e
                            }
                        } while (next.also { e = it } != null)
                        if (loTail != null) {
                            loTail.next = null
                            newTab[j] = loHead
                        }
                        if (hiTail != null) {
                            hiTail.next = null
                            newTab[j + oldCap] = hiHead
                        }
                    }
                }
            }
        }
        return newTab
    }

    private fun treeifyBin(tab: Array<Node<K, V>?>, hash: Int) {
        TODO("Not yet implemented")
    }

    override fun remove(key: K): V? {
        TODO("Not yet implemented")
    }

    override fun clear() {
        modCount++
        val tab = table
        if (tab != null && size > 0) {
            size = 0
            for (i in tab.indices) {
                tab[i] = null
            }
        }
    }

    private open class Node<K, V>(
        val hash: Int,
        override val key: K,
        override var value: V,
        var next: Node<K, V>?
    ) : Map.Entry<K, V>

    private class TreeNode<K, V>(hash: Int, key: K, value: V, next: Node<K, V>?) :
        Node<K, V>(hash, key, value, next) {

        private var before: Map.Entry<K, V>? = null
        private var after: Map.Entry<K, V>? = null
        private var parent: TreeNode<K, V>? = null
        private var left: TreeNode<K, V>? = null
        private var right: TreeNode<K, V>? = null
        private var prev: TreeNode<K, V>? = null
        private var red: Boolean = false

        fun getTreeNode(hash: Int, key: K): TreeNode<K, V>? {
            TODO("Not yet implemented")
        }

        fun putTreeVal(map: HashMap<K, V>, tab: Array<Node<K, V>?>, hash: Int, key: K, value: V): TreeNode<K, V> {
            TODO("Not yet implemented")
        }

        fun split(map: HashMap<K, V>, tab: Array<Node<K, V>?>, index: Int, bit: Int) {
            TODO("Not yet implemented")
        }
    }
}