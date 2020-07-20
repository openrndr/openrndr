package org.openrndr.shape

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.math.transforms.rotateZ
import org.openrndr.math.transforms.scale
import org.openrndr.math.transforms.transform
import org.openrndr.math.transforms.translate
import java.util.*


/**
 * A Drawer-like interface for the creation of Compositions
 * This should be easier than creating Compositions manually
 */

class CompositionDrawer(documentBounds: Rectangle = DefaultCompositionBounds) {
    val root = GroupNode()
    val composition = Composition(root, documentBounds)

    private var cursor = root
    private val modelStack = Stack<Matrix44>()

    var model = Matrix44.IDENTITY
    var fill: ColorRGBa? = null
    var stroke: ColorRGBa? = ColorRGBa.BLACK
    var strokeWeight = 1.0

    fun pushModel() {
        modelStack.push(model)
    }

    fun popModel() {
        model = modelStack.pop()
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

    fun contour(contour: ShapeContour): ShapeNode {
        val shape = Shape(listOf(contour))
        return shape(shape)
    }

    fun contours(contours: List<ShapeContour>) = contours.map { contour(it) }

    fun shape(shape: Shape): ShapeNode {
        val shapeNode = ShapeNode(shape)
        shapeNode.transform = model
        shapeNode.fill = Color(fill)
        shapeNode.stroke = Color(stroke)
        shapeNode.strokeWeight = StrokeWeight(strokeWeight)
        cursor.children.add(shapeNode)
        return shapeNode
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
}

fun drawComposition(
        documentBounds : Rectangle = DefaultCompositionBounds,
        drawFunction:CompositionDrawer.() -> Unit
) : Composition = CompositionDrawer(documentBounds).apply { drawFunction() }.composition