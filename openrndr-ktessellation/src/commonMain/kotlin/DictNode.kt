package org.openrndr.ktessellation

internal class DictNode {
    var key: Any? = null
    var next: DictNode? = null
    var prev: DictNode? = null

    /** the number of skip list levels of this node, level 0 is [next] and [prev] */
    var height = 1

    /** the successors on levels 1 until [height], null for nodes of height 1 */
    var upNext: Array<DictNode?>? = null

    /** the predecessors on levels 1 until [height], null for nodes of height 1 */
    var upPrev: Array<DictNode?>? = null

    fun nextAt(level: Int): DictNode = (if (level == 0) next else upNext!![level - 1])!!
    fun prevAt(level: Int): DictNode = (if (level == 0) prev else upPrev!![level - 1])!!

    fun setNextAt(level: Int, node: DictNode) {
        if (level == 0) next = node else upNext!![level - 1] = node
    }

    fun setPrevAt(level: Int, node: DictNode) {
        if (level == 0) prev = node else upPrev!![level - 1] = node
    }
}
