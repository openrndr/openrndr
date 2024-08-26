package org.openrndr.ktessellation


import kotlin.math.abs

internal class PriorityQSort(leq: Leq) :
    PriorityQ() {
    var heap: PriorityQHeap?
    var keys: Array<Any?>?

    // JAVA: 'order' contains indices into the keys array.
    // This simulates the indirect pointers used in the original C code
    // (from Frank Suykens, Luciad.com).
    var order: IntArray? = null
    var size: Int
    var max: Int
    var initialized: Boolean
    var leq: Leq

    /* really __gl_pqSortDeletePriorityQ */
    override fun pqDeletePriorityQ() {
        if (heap != null) heap!!.pqDeletePriorityQ()
        order = null
        keys = null
    }

    private class Stack {
        var p = 0
        var r = 0
    }

    /* really __gl_pqSortInit */
    override fun pqInit(): Boolean {
        var p: Int
        var r: Int
        var i: Int
        var j: Int
        var piv: Int
        val stack = arrayOfNulls<Stack>(50)
        for (k in stack.indices) {
            stack[k] = Stack()
        }
        var top = 0
        var seed = 2016473283

        /* Create an array of indirect pointers to the keys, so that we
         * the handles we have returned are still valid.
         */order = IntArray(size + 1)
        /* the previous line is a patch to compensate for the fact that IBM */
/* machines return a null on a malloc of zero bytes (unlike SGI),   */
/* so we have to put in this defense to guard against a memory      */
/* fault four lines down. from fossum@austin.ibm.com.               */p = 0
        r = size - 1
        piv = 0
        i = p
        while (i <= r) {

            // indirect pointers: keep an index into the keys array, not a direct pointer to its contents
            order!![i] = piv
            ++piv
            ++i
        }

        /* Sort the indirect pointers in descending order,
         * using randomized Quicksort
         */stack[top]!!.p = p
        stack[top]!!.r = r
        ++top
        while (--top >= 0) {
            p = stack[top]!!.p
            r = stack[top]!!.r
            while (r > p + 10) {
                seed = abs(seed * 1539415821 + 1)
                i = p + seed % (r - p + 1)
                piv = order!![i]
                order!![i] = order!![p]
                order!![p] = piv
                i = p - 1
                j = r + 1
                do {
                    do {
                        ++i
                    } while (GT(leq, keys!![order!![i]]!!, keys!![piv]!!))
                    do {
                        --j
                    } while (LT(leq, keys!![order!![j]]!!, keys!![piv]!!))
                    Swap(order!!, i, j)
                } while (i < j)
                Swap(order!!, i, j) /* Undo last swap */
                if (i - p < r - j) {
                    stack[top]!!.p = j + 1
                    stack[top]!!.r = r
                    ++top
                    r = i - 1
                } else {
                    stack[top]!!.p = p
                    stack[top]!!.r = i - 1
                    ++top
                    p = j + 1
                }
            }
            /* Insertion sort small lists */i = p + 1
            while (i <= r) {
                piv = order!![i]
                j = i
                while (j > p && LT(leq, keys!![order!![j - 1]]!!, keys!![piv]!!)) {
                    order!![j] = order!![j - 1]
                    --j
                }
                order!![j] = piv
                ++i
            }
        }
        max = size
        initialized = true
        heap!!.pqInit() /* always succeeds */

/*        #ifndef NDEBUG
        p = order;
        r = p + size - 1;
        for (i = p; i < r; ++i) {
            Assertion.doAssert(LEQ(     * * (i + 1), **i ));
        }
        #endif*/return true
    }

    /* really __gl_pqSortInsert */ /* returns LONG_MAX iff out of memory */
    override fun pqInsert(keyNew: Any?): Int {
        val curr: Int
        if (initialized) {
            return heap!!.pqInsert(keyNew)
        }
        curr = size
        if (++size >= max) {
            val saveKey = keys

            /* If the heap overflows, double its size. */max = max shl 1
            //            pq->keys = (PQHeapKey *)memRealloc( pq->keys,(size_t)(pq->max * sizeof( pq->keys[0] )));
            val pqKeys = arrayOfNulls<Any>(max)
            arraycopy(keys!!, 0, pqKeys, 0, keys!!.size)
            keys = pqKeys
            if (keys == null) {
                keys = saveKey /* restore ptr to free upon return */
                return Int.MAX_VALUE
            }
        }
        require(curr != Int.MAX_VALUE)
        keys!![curr] = keyNew

        /* Negative handles index the sorted array. */return -(curr + 1)
    }

    /* really __gl_pqSortExtractMin */
    override fun pqExtractMin(): Any? {
        val sortMin: Any
        val heapMin: Any?
        if (size == 0) {
            return heap!!.pqExtractMin()
        }
        sortMin = keys!![order!![size - 1]]!!
        if (!heap!!.pqIsEmpty()) {
            heapMin = heap!!.pqMinimum()
            if (LEQ(leq, heapMin!!, sortMin)) {
                return heap!!.pqExtractMin()
            }
        }
        do {
            --size
        } while (size > 0 && keys!![order!![size - 1]] == null)
        return sortMin
    }

    /* really __gl_pqSortMinimum */
    override fun pqMinimum(): Any? {
        val sortMin: Any
        val heapMin: Any?
        if (size == 0) {
            return heap!!.pqMinimum()
        }
        sortMin = keys!![order!![size - 1]]!!
        if (!heap!!.pqIsEmpty()) {
            heapMin = heap!!.pqMinimum()
            if (LEQ(leq, heapMin!!, sortMin)) {
                return heapMin
            }
        }
        return sortMin
    }

    /* really __gl_pqSortIsEmpty */
    override fun pqIsEmpty(): Boolean {
        return size == 0 && heap!!.pqIsEmpty()
    }

    /* really __gl_pqSortDelete */
    override fun pqDelete(hCurr: Int) {
        var curr = hCurr
        if (curr >= 0) {
            heap!!.pqDelete(curr)
            return
        }
        curr = -(curr + 1)
        require(curr < max && keys!![curr] != null)
        keys!![curr] = null
        while (size > 0 && keys!![order!![size - 1]] == null) {
            --size
        }
    }

    companion object {
        private fun LT(leq: Leq, x: Any, y: Any): Boolean {
            return !LEQ(leq, y, x)
        }

        private fun GT(leq: Leq, x: Any, y: Any): Boolean {
            return !LEQ(leq, x, y)
        }

        private fun Swap(array: IntArray, a: Int, b: Int) {
            if (true) {
                val tmp = array[a]
                array[a] = array[b]
                array[b] = tmp
            } else {
            }
        }
    }

    init {
        heap = PriorityQHeap(leq)
        keys = arrayOfNulls(INIT_SIZE)
        size = 0
        max = INIT_SIZE
        initialized = false
        this.leq = leq
    }
}
