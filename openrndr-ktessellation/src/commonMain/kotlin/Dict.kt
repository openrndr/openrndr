@file:Suppress("UNUSED_PARAMETER", "NAME_SHADOWING", "unused")

package org.openrndr.ktessellation

internal class Dict private constructor() {
    var head: DictNode? = null
    var frame: Any? = null
    var leq: DictLeq? = null

    interface DictLeq {
        fun leq(frame: Any, key1: Any, key2: Any): Boolean
    }

    /** state of the generator of node heights, deterministic so tessellations are reproducible */
    private var random = 0x2545F491

    /** a random node height, each level is 4 times smaller than the one below it */
    private fun randomHeight(): Int {
        var height = 1
        while (height < MAX_HEIGHT) {
            random = random xor (random shl 13)
            random = random xor (random ushr 17)
            random = random xor (random shl 5)
            if (random and 3 != 0) break
            height++
        }
        return height
    }

    companion object {
        /**
         * The dictionary is a sorted, circular, doubly linked list with skip list levels on top of it, so a search
         * takes O(log n) instead of a walk from the head. The levels are linked by position in the list, they never
         * compare keys, so they always agree with the list.
         */
        private const val MAX_HEIGHT = 16

        fun dictNewDict(frame: Any?, leq: DictLeq?): Dict {
            val dict = Dict()
            val head = DictNode()
            head.key = null
            head.next = head
            head.prev = head
            head.height = MAX_HEIGHT
            head.upNext = Array(MAX_HEIGHT - 1) { head }
            head.upPrev = Array(MAX_HEIGHT - 1) { head }
            dict.head = head
            dict.frame = frame
            dict.leq = leq
            return dict
        }

        fun dictDeleteDict(dict: Dict) {
            dict.head = null
            dict.frame = null
            dict.leq = null
        }

        fun dictInsert(dict: Dict, key: Any): DictNode {
            return dictInsertBefore(dict, dict.head ?: error("dict.head == null"), key)
        }

        fun dictInsertBefore(
            dict: Dict,
            node: DictNode,
            key: Any
        ): DictNode {
            var node: DictNode = node
            do {
                node = node.prev ?: error("node.prev == null")
            } while (node.key != null && !dict.leq!!.leq(dict.frame!!, node.key!!, key))
            val newNode = DictNode()
            newNode.key = key
            val height = dict.randomHeight()
            newNode.height = height
            if (height > 1) {
                newNode.upNext = arrayOfNulls(height - 1)
                newNode.upPrev = arrayOfNulls(height - 1)
            }
            // the predecessor on each level is the nearest node before it that is high enough
            var predecessor = node
            for (level in 0 until height) {
                while (predecessor.height <= level) {
                    predecessor = predecessor.prevAt(level - 1)
                }
                val successor = predecessor.nextAt(level)
                newNode.setNextAt(level, successor)
                newNode.setPrevAt(level, predecessor)
                successor.setPrevAt(level, newNode)
                predecessor.setNextAt(level, newNode)
            }
            return newNode
        }

        fun dictKey(aNode: DictNode): Any? {
            return aNode.key
        }

        fun dictSucc(aNode: DictNode): DictNode? {
            return aNode.next
        }

        fun dictPred(aNode: DictNode): DictNode? {
            return aNode.prev
        }

        fun dictMin(aDict: Dict): DictNode {
            return aDict.head?.next ?: error("head.next == null")
        }

        fun dictMax(aDict: Dict): DictNode {
            return aDict.head?.prev ?: error("head.prev == null")
        }

        fun dictDelete(dict: Dict?, node: DictNode) {
            for (level in 0 until node.height) {
                val previous = node.prevAt(level)
                val next = node.nextAt(level)
                next.setPrevAt(level, previous)
                previous.setNextAt(level, next)
            }
        }

        /**
         * The first node whose key is not less than [key], the head when there is none. The keys are sorted, the
         * search descends the skip list levels.
         */
        fun dictSearch(dict: Dict, key: Any?): DictNode? {
            val head = dict.head ?: error("dict.head == null")
            val leq = dict.leq!!
            val frame = dict.frame!!
            var node: DictNode = head
            for (level in MAX_HEIGHT - 1 downTo 0) {
                while (true) {
                    val next = node.nextAt(level)
                    if (next === head || leq.leq(frame, key!!, next.key!!)) break
                    node = next
                }
            }
            return node.next
        }
    }
}
