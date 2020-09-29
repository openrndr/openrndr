package org.openrndr.shape

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.math.transforms.rotateZ
import org.openrndr.math.transforms.scale
import org.openrndr.math.transforms.transform
import org.openrndr.math.transforms.translate
import java.util.*

enum class ClipOp {
    DISABLED,
    DIFFERENCE,
    INTERSECT,
    UNION
}

enum class ClipMode(val grouped: Boolean, val op: ClipOp) {
    DISABLED(false, ClipOp.DISABLED),
    DIFFERENCE(false, ClipOp.DIFFERENCE),
    DIFFERENCE_GROUP(true, ClipOp.DIFFERENCE),
    INTERSECT(false, ClipOp.INTERSECT),
    INTERSECT_GROUP(true, ClipOp.INTERSECT),
    UNION(false, ClipOp.UNION),
    UNION_GROUP(true, ClipOp.UNION)
}

private data class CompositionDrawStyle(
        var fill: ColorRGBa? = null,
        var stroke: ColorRGBa? = ColorRGBa.BLACK,
        var strokeWeight: Double = 1.0,
        var clipMode: ClipMode = ClipMode.DISABLED
)


/**
 * A Drawer-like interface for the creation of Compositions
 * This should be easier than creating Compositions manually
 */

class CompositionDrawer(documentBounds: Rectangle = DefaultCompositionBounds) {
    val root = GroupNode()
    val composition = Composition(root, documentBounds)

    private var cursor = root
    private val modelStack = Stack<Matrix44>()
    private val styleStack = Stack<CompositionDrawStyle>()
    private var drawStyle = CompositionDrawStyle()

    var model = Matrix44.IDENTITY

    var fill by drawStyle::fill
    var stroke by drawStyle::stroke
    var strokeWeight by drawStyle::strokeWeight
    var clipMode by drawStyle::clipMode

    fun pushModel() {
        modelStack.push(model)
    }

    fun popModel() {
        model = modelStack.pop()
    }

    fun pushStyle() {
        styleStack.push(drawStyle.copy())
    }

    fun popStyle() {
        drawStyle = styleStack.pop()
    }

    fun isolated(draw: CompositionDrawer.() -> Unit) {
        pushModel()
        pushStyle()
        draw()
        popModel()
        popStyle()
    }

    fun group(id: String? = null, builder: CompositionDrawer.() -> Unit): GroupNode {
        val g = GroupNode()
        g.id = id
        val oldCursor = cursor

        cursor.children.add(g)
        cursor = g
        builder()

        cursor = oldCursor
        return g
    }

    fun translate(x: Double, y: Double) = translate(Vector2(x, y))

    fun rotate(rotationInDegrees: Double) {
        model *= Matrix44.rotateZ(rotationInDegrees)
    }

    fun scale(s: Double) {
        model *= Matrix44.scale(s, s, s)
    }

    fun scale(x: Double, y: Double) {
        model *= Matrix44.scale(x, y, 1.0)
    }

    fun translate(t: Vector2) {
        model *= Matrix44.translate(t.vector3())
    }

    fun contour(contour: ShapeContour): ShapeNode? {
        val shape = Shape(listOf(contour))
        return shape(shape)
    }

    fun contours(contours: List<ShapeContour>) = contours.map { contour(it) }

    fun shape(shape: Shape): ShapeNode? {
        // only use clipping for open shapes
        val clipMode = if (shape.topology == ShapeTopology.CLOSED) clipMode else ClipMode.DISABLED

        return when (clipMode) {
            ClipMode.DISABLED -> {
                val shapeNode = ShapeNode(shape)
                shapeNode.transform = model
                shapeNode.fill = Color(fill)
                shapeNode.stroke = Color(stroke)
                shapeNode.strokeWeight = StrokeWeight(strokeWeight)
                cursor.children.add(shapeNode)
                shapeNode.parent = cursor
                shapeNode
            }
            else -> {
                val shapeNodes = (if (!clipMode.grouped) composition.findShapes() else cursor.findShapes())

                shapeNodes.forEach { shapeNode ->
                    val transform = shapeNode.effectiveTransform
                    val inverse = if (transform === Matrix44.IDENTITY) Matrix44.IDENTITY else transform.inversed
                    val transformedShape = if (inverse === Matrix44.IDENTITY) shape else shape.transform(inverse)
                    val operated =
                            when (clipMode.op) {
                                ClipOp.INTERSECT -> intersection(shapeNode.shape, transformedShape)
                                ClipOp.UNION -> union(shapeNode.shape, transformedShape).take(1)
                                ClipOp.DIFFERENCE -> difference(shapeNode.shape, transformedShape)
                                else -> error("unsupported base op ${clipMode.op}")
                            }

                    when (operated.size) {
                        0 -> {
                            shapeNode.remove()
                        }
                        1 -> {
                            shapeNode.shape = operated.first()
                        }
                        else -> {
                            shapeNode.shape = Shape.compound(operated)
//                            val groupNode = GroupNode(operated.map { ShapeNode(it) }.toMutableList())
//                            (shapeNode.parent as? GroupNode)?.children?.replace(shapeNode, groupNode)
                        }
                    }
                }
                null
            }
        }
    }

    fun shapes(shapes: List<Shape>) = shapes.map { shape(it) }

    fun rectangle(rectangle: Rectangle) = contour(rectangle.contour)

    fun rectangle(x: Double, y: Double, width: Double, height: Double) = rectangle(Rectangle(x, y, width, height))

    fun rectangles(rectangles: List<Rectangle>) = rectangles.map { rectangle(it) }

    fun rectangles(positions: List<Vector2>, width: Double, height: Double) = rectangles(positions.map {
        Rectangle(it, width, height)
    })

    fun rectangles(positions: List<Vector2>, dimensions: List<Vector2>) = rectangles((positions zip dimensions).map {
        Rectangle(it.first, it.second.x, it.second.y)
    })

    fun circle(position: Vector2, radius: Double) = circle(Circle(position, radius))

    fun circle(circle: Circle) = contour(circle.contour)

    fun circles(circles: List<Circle>) = circles.map { circle(it) }

    fun circles(positions: List<Vector2>, radius: Double) = circles(positions.map { Circle(it, radius) })

    fun circles(positions: List<Vector2>, radii: List<Double>) = circles((positions zip radii).map { Circle(it.first, it.second) })

    fun lineSegment(start: Vector2, end: Vector2) = lineSegment(LineSegment(start, end))

    fun lineSegment(lineSegment: LineSegment) = contour(lineSegment.contour)

    fun lineSegments(lineSegments: List<LineSegment>) = lineSegments.map {
        lineSegment(it)
    }

    fun lineStrip(points: List<Vector2>) = contour(ShapeContour.fromPoints(points, false, YPolarity.CW_NEGATIVE_Y))

    fun lineLoop(points: List<Vector2>) = contour(ShapeContour.fromPoints(points, true, YPolarity.CW_NEGATIVE_Y))

    fun text(text: String, position: Vector2): TextNode {
        val g = GroupNode()
        g.transform = transform { translate(position.xy0) }
        val textNode = TextNode(text, null).apply {
            this.fill = Color(this@CompositionDrawer.fill)
        }
        g.children.add(textNode)
        cursor.children.add(g)
        return textNode
    }

    fun textOnContour(text: String, path: ShapeContour) {
        cursor.children.add(TextNode(text, path))
    }

    fun texts(text: List<String>, positions: List<Vector2>) =
            (text zip positions).map {
                text(it.first, it.second)
            }

    fun image(image: ColorBuffer, x: Double = 0.0, y: Double = 0.0) : ImageNode {
        val node = ImageNode(image, x, y, width = image.width.toDouble(), height = image.height.toDouble())
        node.transform = this.model
        cursor.children.add(node)
        return node
    }
}

private fun <E> MutableList<E>.replace(search: E, replace: E) {
    val index = this.indexOf(search)
    if (index != -1) {
        this[index] = replace
    }
}

fun drawComposition(
        documentBounds: Rectangle = DefaultCompositionBounds,
        drawFunction: CompositionDrawer.() -> Unit
): Composition = CompositionDrawer(documentBounds).apply { drawFunction() }.composition