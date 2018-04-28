package org.openrndr.shape

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix44

sealed class CompositionNode {
    var id: String? = null
    var parent: CompositionNode? = null
    var transform = Matrix44.IDENTITY
    var fill: CompositionColor = InheritColor
    var stroke: CompositionColor = InheritColor

    open val bounds: Rectangle
        get() = TODO("can't have it")


    val effectiveStroke: ColorRGBa?
        get() {
            return stroke.let {
                when (it) {
                    is InheritColor -> parent?.effectiveStroke
                    is Color -> it.color
                }
            }
        }

    val effectiveFill: ColorRGBa?
        get() {
            return fill.let {
                when (it) {
                    is InheritColor -> parent?.effectiveFill ?: ColorRGBa.BLACK
                    is Color -> it.color
                }
            }
        }
}


sealed class CompositionColor

object InheritColor : CompositionColor()
class Color(val color: ColorRGBa?) : CompositionColor()

private fun transform(node: CompositionNode): Matrix44 =
        (node.parent?.let { transform(it) } ?: Matrix44.IDENTITY) * node.transform

class ShapeNode(var shape: Shape) : CompositionNode() {
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

    fun copy(id: String? = this.id, parent: CompositionNode? = null, transform: Matrix44 = this.transform, fill: CompositionColor = this.fill, stroke: CompositionColor = this.stroke, shape: Shape = this.shape): ShapeNode {
        return ShapeNode(shape).also {
            it.id = id
            it.parent = parent
            it.transform = transform
            it.fill = fill
            it.stroke = stroke
            it.shape = shape
        }
    }
}

class TextNode : CompositionNode() {

}

open class GroupNode(val children: MutableList<CompositionNode> = mutableListOf()) : CompositionNode() {
    override val bounds: Rectangle
        get() {
            val b = rectangleBounds(children.map { it.bounds })
            return b
        }

    fun copy(id: String? = this.id, parent: CompositionNode? = null, transform: Matrix44 = this.transform, fill: CompositionColor = this.fill, stroke: CompositionColor = this.stroke, children: MutableList<CompositionNode> = this.children): GroupNode {
        return GroupNode(children).also {
            it.id = id
            it.parent = parent
            it.transform = transform
            it.fill = fill
            it.stroke = stroke
        }
    }
}

class GroupNodeStop(children: MutableList<CompositionNode>) : GroupNode(children)

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

fun CompositionNode.map(mapper: (CompositionNode) -> CompositionNode): CompositionNode {
    val r = mapper(this)
    return when (r) {
        is GroupNodeStop -> {
            r.copy().also { copy ->
                copy.children.forEach {
                    it.parent = copy
                }
            }
        }
        is GroupNode -> {
            val copy = r.copy(children = r.children.map { it.map(mapper) }.toMutableList())
            copy.children.forEach {
                it.parent = copy
            }
            copy
        }
        else -> r

    }
}
