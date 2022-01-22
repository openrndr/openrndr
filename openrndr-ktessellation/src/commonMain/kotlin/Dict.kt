package org.openrndr.ktessellation

internal class Dict private constructor() {
    var head: DictNode? = null
    var frame: Any? = null
    var leq: DictLeq? = null

    interface DictLeq {
        fun leq(frame: Any, key1: Any, key2: Any): Boolean
    }

    companion object {
        fun dictNewDict(frame: Any?, leq: DictLeq?): Dict {
            val dict = Dict()
            dict.head = DictNode()

            dict.head?.let {
                it.key = null
                it.next = it
                it.prev = it
            }
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
            val newNode: DictNode = DictNode()
            newNode.key = key
            newNode.next = node.next
            node.next?.prev = newNode
            newNode.prev = node
            node.next = newNode
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
            node.next?.prev = node.prev
            node.prev?.next = node.next
        }

        fun dictSearch(dict: Dict, key: Any?): DictNode? {
            var node: DictNode? = dict.head
            do {
                node = node?.next
            } while (node?.key != null && !dict.leq!!.leq(dict.frame!!, key!!, node.key!!))
            return node
        }
    }
}
