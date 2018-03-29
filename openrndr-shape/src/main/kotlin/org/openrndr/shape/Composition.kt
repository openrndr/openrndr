package org.openrndr.shape

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2

sealed class CompositionNode {
    var id: String? = null
    var parent: CompositionNode? = null
    var transform = Matrix44.IDENTITY
    var fill: ColorRGBa? = null
    var stroke: ColorRGBa? = null

    open val bounds: Rectangle
        get() = TODO("can't have it")

}

private fun transform(node: CompositionNode): Matrix44 =
        (node.parent?.let { transform(it) } ?: Matrix44.IDENTITY) * node.transform

data class ShapeNode(var shape: Shape) : CompositionNode() {
    override val bounds: Rectangle
        get() = shape.contours[0].transform(transform(this)).bounds

    /**
     * Applies transforms of all ancestor nodes and returns a new detached ShapeNode with conflated transform
     */
    fun conflate(): ShapeNode {
        return ShapeNode(shape).also {
            it.fill = fill
            it.stroke = stroke
            it.transform = transform(this)
            it.id = id
        }

    }

    /**
     * Applies transforms of all ancestor nodes and returns a new detached shape node with identity transform and transformed Shape
     */

    fun flatten(): ShapeNode {
        return ShapeNode(shape.transform(transform(this))).also {
            it.fill = fill
            it.stroke = stroke
            it.transform = Matrix44.IDENTITY
            it.id = id
        }
    }

}

class TextNode : CompositionNode() {

}

class GroupNode(val children:MutableList<CompositionNode> = mutableListOf() ): CompositionNode() {
    override val bounds: Rectangle
    get() {
        val b = bounds(children.map { it.bounds })
        return b
    }
}

class Composition(val root: CompositionNode) {

    fun findTerminals(filter: (CompositionNode) -> Boolean): List<CompositionNode> {
        val result = mutableListOf<CompositionNode>()
        fun find(node: CompositionNode) {
            when (node) {
                is GroupNode -> node.children.forEach { find(it) }
                else -> if (filter(node)) {
                    result.add(node)
                }
            }
        }
        find(root)
        return result
    }

    fun findShapes(): List<ShapeNode> = findTerminals { it is ShapeNode }.map { it as ShapeNode }

}


fun CompositionNode.map( mapper: (CompositionNode) -> CompositionNode):CompositionNode {

    val r = mapper(this)
    if (r is GroupNode) {
        return GroupNode(r.children.map { it.map(mapper) }.toMutableList())
    } else {
        return r
    }

}