package org.openrndr.shape

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix44

sealed class CompositionNode {
    var id: String? = null
    var parent: CompositionNode? = null
    var transform = Matrix44.IDENTITY
    var fill: ColorRGBa? = null
    var stroke: ColorRGBa? = null
}
private fun transform(node:CompositionNode):Matrix44 =
        (node.parent?.let { transform(it) }?: Matrix44.IDENTITY) * node.transform

data class ShapeNode(var shape: Shape) : CompositionNode() {



    /**
     * Applies transforms of all ancestor nodes and returns a new detached ShapeNode with conflated transform
     */
    fun conflate():ShapeNode {
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

    fun flatten():ShapeNode {
        return ShapeNode(shape.transform(transform(this))).also {
            it.fill = fill
            it.stroke = stroke
            it.transform = transform
            it.id = id
        }
    }

}

class TextNode : CompositionNode() {

}

class GroupNode : CompositionNode() {
    val children = mutableListOf<CompositionNode>()
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