package org.openrndr.binpack

import org.openrndr.math.IntVector2
import org.openrndr.shape.IntRectangle

fun leafNodes(node: PackNode): List<PackNode> = (if (node.isLeaf()) listOf(node) else emptyList()).plus(node.children.flatMap { leafNodes(it) })

class PackNode(val area: IntRectangle, val parent: PackNode? = null) {
    var taken: Boolean = false
    var data: Any? = null
    var children: List<PackNode> = mutableListOf()
    var freeArea = IntVector2(area.width, area.height)

    fun isLeaf(): Boolean = children.isEmpty()

    fun populate(data: Any? = null) {
        if (!taken) {
            this.data = data
            taken = true
        } else {
            throw RuntimeException("node already taken")
        }
    }

    fun hasTakenDecendents(): Boolean = if (children.any { it.taken }) {
        true
    } else {
        children.any { it.hasTakenDecendents() }
    }

}