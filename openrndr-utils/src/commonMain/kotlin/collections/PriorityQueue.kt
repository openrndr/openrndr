//https://codereview.stackexchange.com/questions/175291/minimum-priority-queue-implementation-in-kotlin
package org.openrndr.collections

class PriorityQueue<Key> : Iterable<Key> {
    private var pq: Array<Key?>
    private var n = 0
    private val comparator: Comparator<Key>?

    constructor(initCapacity: Int) : this(initCapacity, null)
    constructor() : this(1)
    constructor(comparator: Comparator<Key>?) : this(1, comparator)

    constructor(initCapacity: Int, comparator: Comparator<Key>?) {
        this.comparator = comparator
        @Suppress("UNCHECKED_CAST")
        pq = arrayOfNulls<Any?>(initCapacity + 1) as Array<Key?>
    }

    constructor(keys: Array<Key>) : this(keys.size) {
        @Suppress("UNCHECKED_CAST")
        pq = arrayOfNulls<Any?>(keys.size + 1) as Array<Key?>
        n = keys.size
        for (i in 0 until n) {
            pq[i + 1] = keys[i]
        }
        for (k in n / 2 downTo 1) {
            sink(k)
        }
        //assert(isMinHeap())
    }

    fun isEmpty() = n == 0

    fun size() = n

    fun peek(): Key {
        if (!isEmpty()) {
            return pq[1]!!
        } else {
            error("pq empty")
        }
    }


    fun add(x: Key): PriorityQueue<Key> = insert(x)
    fun insert(x: Key): PriorityQueue<Key> {
        if (n == pq.size - 1) {
            resize(2 * pq.size)
        }
        pq[++n] = x
        swim(n)
        //assert(isMinHeap())
        return this
    }

    fun poll(): Key? {
        if (isEmpty()) {
            return null
        }

        //require(!isEmpty()) { "Cannot retrieve minimum record. Priority queue is empty" }
        val min = pq[1]
        exch(1, n--)
        sink(1)
        pq[n + 1] = null
        //assert(isMinHeap())
        return min ?: throw NullPointerException("'min' must not be null")
    }

    override fun iterator(): Iterator<Key> {
        return HeapIterator(comparator, size(), n, pq)
    }

    private fun swim(k: Int) {
        var myK = k
        while (myK > 1 && greater(myK / 2, myK)) {
            exch(myK, myK / 2)
            myK /= 2
        }
    }

    private fun sink(k: Int) {
        var myK = k
        while (2 * myK <= n) {
            var j = 2 * myK
            if (j < n && greater(j, j + 1)) j++
            if (!greater(myK, j)) return
            exch(myK, j)
            myK = j
        }
    }

    private fun greater(i: Int, j: Int): Boolean {
        @Suppress("UNCHECKED_CAST")
        return if (comparator == null) (pq[i] as Comparable<Key>) > pq[j]!!
        else comparator.compare(pq[i]!!, pq[j]!!) > 0
    }

    private fun exch(i: Int, j: Int) {
        pq[i] = pq[j].also { pq[j] = pq[i] }
    }

    private fun isMinHeap(): Boolean = isMinHeap(1)

    private fun isMinHeap(k: Int): Boolean {
        if (k > n) return true
        val left = 2 * k
        val right = 2 * k + 1
        return when {
            left <= n && greater(k, left) -> false
            right <= n && greater(k, right) -> false
            else -> {
                isMinHeap(left) && isMinHeap(right)
            }
        }
    }

    private fun resize(capacity: Int) {
        //assert(capacity > n)
        @Suppress("UNCHECKED_CAST") val temp = arrayOfNulls<Any>(capacity) as Array<Key?>
        for (i in 1..n) {
            temp[i] = pq[i]
        }
        pq = temp
    }

    class HeapIterator<out Key>(comparator: Comparator<Key>?, size: Int, n: Int, pq: Array<Key?>) : Iterator<Key> {

        private val copy: PriorityQueue<Key> = if (comparator == null) PriorityQueue(size) else PriorityQueue(size, comparator)

        override fun hasNext(): Boolean {
            return !copy.isEmpty()
        }

        override fun next(): Key {
            require(hasNext()) {"Queue is empty"}
            return copy.poll() ?: error("Queue is empty")
        }

        init {
            for (i in 1..n)
                copy.insert(pq[i]!!)
        }
    }
}