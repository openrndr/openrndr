@file:Suppress("NAME_SHADOWING", "FunctionName", "ConvertTwoComparisonsToRangeCheck")

package org.openrndr.ktessellation

@Suppress("MemberVisibilityCanBePrivate")
internal class PriorityQHeap(val leq: Leq) :
    PriorityQ() {
    var nodes: Array<PQnode?>?
    var handles: Array<PQhandleElem?>?
    var size = 0
    var max: Int
    var freeList: Int
    var initialized: Boolean

    /* really __gl_pqHeapDeletePriorityQ */
    override fun pqDeletePriorityQ() {
        handles = null
        nodes = null
    }

    fun FloatDown(curr: Int) {
        var curr = curr
        val n: Array<PQnode?>? = nodes
        val h: Array<PQhandleElem?>? = handles
        var hChild: Int
        var child: Int
        val hCurr: Int = n!![curr]!!.handle
        while (true) {
            child = curr shl 1
            if (child < size && LEQ(
                    leq, h!![n[child + 1]!!.handle]!!.key!!,
                    h[n[child]!!.handle]!!.key!!
                )
            ) {
                ++child
            }
            require(child <= max)
            hChild = n[child]!!.handle
            if (child > size || LEQ(leq, h!![hCurr]!!.key!!, h[hChild]!!.key!!)) {
                n[curr]!!.handle = hCurr
                h!![hCurr]!!.node = curr
                break
            }
            n[curr]!!.handle = hChild
            h[hChild]!!.node = curr
            curr = child
        }
    }

    fun FloatUp(curr: Int) {
        var curr = curr
        val n: Array<PQnode?>? = nodes
        val h: Array<PQhandleElem?>? = handles
        var hParent: Int
        var parent: Int
        val hCurr: Int = n!![curr]!!.handle
        while (true) {
            parent = curr shr 1
            hParent = n[parent]!!.handle
            if (parent == 0 || LEQ(leq, h!![hParent]!!.key!!, h[hCurr]!!.key!!)) {
                n[curr]!!.handle = hCurr
                h!![hCurr]!!.node = curr
                break
            }
            n[curr]!!.handle = hParent
            h[hParent]!!.node = curr
            curr = parent
        }
    }

    /* really __gl_pqHeapInit */
    override fun pqInit(): Boolean {

        /* This method of building a heap is O(n), rather than O(n lg n). */
        var i: Int = size
        while (i >= 1) {
            FloatDown(i)
            --i
        }
        initialized = true
        return true
    }

    /* really __gl_pqHeapInsert */ /* returns LONG_MAX iff out of memory */
    override fun pqInsert(keyNew: Any?): Int {
        val free: Int
        val curr: Int = ++size
        if (curr * 2 > max) {
            val saveNodes: Array<PQnode?>? = nodes
            val saveHandles: Array<PQhandleElem?>? = handles

            /* If the heap overflows, double its size. */max = max shl 1
            //            pq->nodes = (PQnode *)memRealloc( pq->nodes, (size_t) ((pq->max + 1) * sizeof( pq->nodes[0] )));
            val pqNodes: Array<PQnode?> =
                arrayOfNulls(max + 1)
            arraycopy(nodes!!, 0, pqNodes, 0, nodes!!.size)
            for (i in nodes!!.size until pqNodes.size) {
                pqNodes[i] = PQnode()
            }
            nodes = pqNodes
            if (nodes == null) {
                nodes = saveNodes /* restore ptr to free upon return */
                return Int.MAX_VALUE
            }

//            pq->handles = (PQhandleElem *)memRealloc( pq->handles,(size_t)((pq->max + 1) * sizeof( pq->handles[0] )));
            val pqHandles: Array<PQhandleElem?> =
                arrayOfNulls(max + 1)
            arraycopy(handles!!, 0, pqHandles, 0, handles!!.size)
            for (i in handles!!.size until pqHandles.size) {
                pqHandles[i] = PQhandleElem()
            }
            handles = pqHandles
            if (handles == null) {
                handles = saveHandles /* restore ptr to free upon return */
                return Int.MAX_VALUE
            }
        }
        if (freeList == 0) {
            free = curr
        } else {
            free = freeList
            freeList = handles!![free]!!.node
        }
        nodes!![curr]!!.handle = free
        handles!![free]!!.node = curr
        handles!![free]!!.key = keyNew
        if (initialized) {
            FloatUp(curr)
        }
        require(free != Int.MAX_VALUE)
        return free
    }

    /* really __gl_pqHeapExtractMin */
    override fun pqExtractMin(): Any? {
        val n: Array<PQnode?>? = nodes
        val h: Array<PQhandleElem?>? = handles
        val hMin: Int = n!![1]!!.handle
        val min: Any? = h!![hMin]!!.key
        if (size > 0) {
            n[1]!!.handle = n[size]!!.handle
            h[n[1]!!.handle]!!.node = 1
            h[hMin]!!.key = null
            h[hMin]!!.node = freeList
            freeList = hMin
            if (--size > 0) {
                FloatDown(1)
            }
        }
        return min
    }

    /* really __gl_pqHeapDelete */
    override fun pqDelete(hCurr: Int) {
        val n: Array<PQnode?>? = nodes
        val h: Array<PQhandleElem?>? = handles
        require(hCurr >= 1 && hCurr <= max && h!![hCurr]!!.key != null)
        val curr: Int = h!![hCurr]!!.node
        n!![curr]!!.handle = n[size]!!.handle
        h[n[curr]!!.handle]!!.node = curr
        if (curr <= --size) {
            if (curr <= 1 || LEQ(
                    leq,
                    h[n[curr shr 1]!!.handle]!!.key!!,
                    h[n[curr]!!.handle]!!.key!!
                )
            ) {
                FloatDown(curr)
            } else {
                FloatUp(curr)
            }
        }
        h[hCurr]!!.key = null
        h[hCurr]!!.node = freeList
        freeList = hCurr
    }

    override fun pqMinimum(): Any? {
        return handles!![nodes!![1]!!.handle]!!.key
    }

    override fun pqIsEmpty(): Boolean {
        return size == 0
    }

    /* really __gl_pqHeapNewPriorityQ */
    init {
        max = INIT_SIZE
        nodes =
            arrayOfNulls(INIT_SIZE + 1)
        for (i in nodes!!.indices) {
            nodes!![i] = PQnode()
        }
        handles =
            arrayOfNulls(INIT_SIZE + 1)
        for (i in handles!!.indices) {
            handles!![i] = PQhandleElem()
        }
        initialized = false
        freeList = 0
        nodes!![1]!!.handle = 1 /* so that Minimum() returns NULL */
        handles!![1]!!.key = null
    }
}
