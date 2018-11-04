@file:Suppress("unused")

package org.openrndr.draw

import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.internal.*
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.shape.*
import org.openrndr.math.transforms.normalMatrix
import org.openrndr.math.transforms.perspective as _perspective
import org.openrndr.math.transforms.lookAt as _lookAt
import org.openrndr.math.transforms.rotateZ
import org.openrndr.math.transforms.ortho as _ortho
import org.openrndr.math.transforms.translate as _translate
import org.openrndr.math.transforms.rotate as _rotate
import org.openrndr.math.transforms.scale as _scale

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL

import java.nio.ByteBuffer
import java.util.*

data class VertexElement(val attribute: String, val offset: Int, val type: VertexElementType, val arraySize: Int)

@Suppress("MemberVisibilityCanPrivate")


fun codeFromStream(stream: InputStream): String {
    BufferedReader(InputStreamReader(stream)).use {
        return it.readText()
    }
}

fun codeFromURL(url: URL): String {
    url.openStream().use {
        return codeFromStream(it)
    }
}

fun codeFromURL(url: String): String {
    return codeFromURL(URL(url))
}

interface ProgramRenderTarget : RenderTarget {
    val program: Program
    override val width get() = program.width
    override val height get() = program.height
}

enum class DrawQuality {
    QUALITY,
    PERFORMANCE
}

@Suppress("MemberVisibilityCanPrivate", "unused")
class Drawer(val driver: Driver) {

    val bounds: Rectangle
        get() = Rectangle(Vector2(0.0, 0.0), width * 1.0, height * 1.0)

    private val drawStyles = Stack<DrawStyle>().apply {
        push(DrawStyle())
    }

    private var rectangleDrawer = RectangleDrawer()
    private var vertexBufferDrawer = VertexBufferDrawer()
    private var circleDrawer = CircleDrawer()
    private var imageDrawer = ImageDrawer()
    private var fastLineDrawer = PerformanceLineDrawer()
    private var qualityLineDrawer = QualityLineDrawer()
    private var qualityPolygonDrawer = QualityPolygonDrawer()
    internal val fontImageMapDrawer = FontImageMapDrawer()

    val modelStack = Stack<Matrix44>()
    val viewStack = Stack<Matrix44>()
    val projectionStack = Stack<Matrix44>()

    var width: Int = 0
    var height: Int = 0

    var model: Matrix44 = Matrix44.IDENTITY
    var view: Matrix44 = Matrix44.IDENTITY
    var projection: Matrix44 = Matrix44.IDENTITY

    val context: DrawContext
        get() = DrawContext(model, view, projection, width, height, RenderTarget.active.contentScale)

    var drawStyle = DrawStyle()

    fun withTarget(target: RenderTarget, action: Drawer.() -> Unit) {
        target.bind()
        this.action()
        target.unbind()
    }

    fun reset() {
        viewStack.clear()
        modelStack.clear()
        projectionStack.clear()
        drawStyles.clear()
        ortho()
        drawStyle = DrawStyle()
        view = Matrix44.IDENTITY
        model = Matrix44.IDENTITY
    }

    fun ortho(renderTarget: RenderTarget) {
        ortho(0.0, renderTarget.width.toDouble(), renderTarget.height.toDouble(), 0.0, -1.0, 1.0)
    }

    fun ortho() {
        ortho(0.0, width.toDouble(), height.toDouble(), 0.0, -1.0, 1.0)
    }

    fun ortho(left: Double, right: Double, bottom: Double, top: Double, near: Double, far: Double) {
        projection = _ortho(left, right, bottom, top, near, far)
    }

    /**
     *  Sets the projection to a perspective projection matrix
     *
     *  [fovY] Y field of view in degrees
     *  [aspectRatio] lens aspect aspectRatio
     *  [zNear] The distance to the zNear clipping plane along the -Z axis.
     *  [zFar]The distance to the zFar clipping plane along the -Z axis.
     */
    fun perspective(fovY: Double, aspectRatio: Double, zNear: Double, zFar: Double) {
        projection = _perspective(fovY, aspectRatio, zNear, zFar)
    }

    fun lookAt(from: Vector3, to: Vector3, up: Vector3 = Vector3.UNIT_Y) {
        view *= _lookAt(from, to, up)
    }

    fun scale(s: Double) {
        model *= _scale(s, s, s)
    }

    fun scale(x: Double, y: Double) {
        model *= _scale(x, y, 1.0)
    }

    fun scale(x: Double, y: Double, z: Double) {
        model *= _scale(x, y, z)
    }

    fun translate(t: Vector2) {
        model *= _translate(t.vector3())
    }

    fun translate(t: Vector3) {
        model *= _translate(t)
    }

    fun translate(x: Double, y: Double) {
        translate(x, y, 0.0)
    }

    fun translate(x: Double, y: Double, z: Double) {
        model *= _translate(Vector3(x, y, z))
    }

    fun rotate(rotationInDegrees: Double) {
        model *= rotateZ(rotationInDegrees)
    }

    fun rotate(axis: Vector3, rotationInDegrees: Double) {
        model *= _rotate(axis, rotationInDegrees)
    }

    fun background(r: Double, g: Double, b: Double, a: Double) {
        driver.clear(r, g, b, a)
    }

    fun background(color: ColorRGBa) {
        driver.clear(color)
    }

    fun pushStyle(): DrawStyle = drawStyles.push(drawStyle.copy())
    fun popStyle() {
        drawStyle = drawStyles.pop().copy()
    }

    fun pushView(): Matrix44 = viewStack.push(view)
    fun popView() {
        view = viewStack.pop()
    }

    fun pushModel(): Matrix44 = modelStack.push(model)
    fun popModel() {
        model = modelStack.pop()
    }

    fun pushProjection(): Matrix44 = projectionStack.push(projection)
    fun popProjection() {
        projection = projectionStack.pop()
    }

    fun pushTransforms() {
        pushModel()
        pushView()
        pushProjection()
    }

    fun popTransforms() {
        popModel()
        popView()
        popProjection()
    }

    var depthWrite: Boolean
        set(value) {
            drawStyle.depthWrite = value
        }
        get() = drawStyle.depthWrite

    var cullTestPass: CullTestPass
        set(value) {
            drawStyle.cullTestPass = value
        }
        get() = drawStyle.cullTestPass

    var depthTestPass: DepthTestPass
        set(value) {
            drawStyle.depthTestPass = value
        }
        get() = drawStyle.depthTestPass


    var shadeStyle: ShadeStyle?
        set(value) {
            drawStyle.shadeStyle = value
        }
        get() = drawStyle.shadeStyle


    var fill: ColorRGBa?
        set(value) {
            drawStyle.fill = value
        }
        get() = drawStyle.fill

    var stroke: ColorRGBa?
        set(value) {
            drawStyle.stroke = value
        }
        get() = drawStyle.stroke

    var strokeWeight: Double
        set(value) {
            drawStyle.strokeWeight = value
        }
        get() = drawStyle.strokeWeight


    var lineCap: LineCap
        set(value) {
            drawStyle.lineCap = value
        }
        get() = drawStyle.lineCap

    var lineJoin: LineJoin
        set(value) {
            drawStyle.lineJoin = value
        }
        get() = drawStyle.lineJoin

    var fontMap: FontMap?
        set(value) {
            drawStyle.fontMap = value
        }
        get() = drawStyle.fontMap


    fun rectangle(rectangle: Rectangle) {
        rectangleDrawer.drawRectangle(context, drawStyle, rectangle.x, rectangle.y, rectangle.width, rectangle.height)
    }

    fun rectangle(x: Double, y: Double, width: Double, height: Double) {
        rectangleDrawer.drawRectangle(context, drawStyle, x, y, width, height)
    }

    fun rectangle(corner: Vector2, width: Double, height: Double) {
        rectangleDrawer.drawRectangle(context, drawStyle, corner.x, corner.y, width, height)
    }

    fun rectangles(positions: List<Vector2>, width: Double, height: Double) {
        rectangleDrawer.drawRectangles(context, drawStyle, positions, width, height)
    }

    fun rectangles(positions: List<Vector2>, dimensions: List<Vector2>) {
        rectangleDrawer.drawRectangles(context, drawStyle, positions, dimensions)
    }

    fun rectangles(rectangles: List<Rectangle>) {
        rectangleDrawer.drawRectangles(context, drawStyle, rectangles)
    }

    fun circle(x: Double, y: Double, radius: Double) {
        circleDrawer.drawCircle(context, drawStyle, x, y, radius)
    }

    fun circle(position: Vector2, radius: Double) {
        circleDrawer.drawCircle(context, drawStyle, position.x, position.y, radius)
    }

    fun circle(circle: Circle) {
        circleDrawer.drawCircle(context, drawStyle, circle.center.x, circle.center.y, circle.radius)
    }

    fun circles(positions: List<Vector2>, radius: Double) {
        circleDrawer.drawCircles(context, drawStyle, positions, radius)
    }

    fun circles(positions: List<Vector2>, radii: List<Double>) {
        circleDrawer.drawCircles(context, drawStyle, positions, radii)
    }

    fun circles(circles: List<Circle>) {
        circleDrawer.drawCircles(context, drawStyle, circles)
    }

    fun shape(shape: Shape) {
        if (RenderTarget.active.hasDepthBuffer) {
            if (shape.contours.size > 1 || shape.contours[0].closed) {
                qualityPolygonDrawer.drawPolygon(context, drawStyle,
                        shape.contours.map { it.adaptivePositions() })
            } else if (!shape.contours[0].closed && drawStyle.stroke != null) {
                qualityLineDrawer.drawLineStrips(context, drawStyle, listOf(shape.contours[0].adaptivePositions()))
            }

        } else {
            throw RuntimeException("drawing shapes requires a render target with a depth buffer attachment")
        }
    }

    fun shapes(shapes: List<Shape>) {
        shapes.forEach {
            shape(it)
        }
    }

    fun contour(contour: ShapeContour) {
        if (RenderTarget.active.hasDepthBuffer) {
            if (drawStyle.fill != null && contour.closed) {
                qualityPolygonDrawer.drawPolygon(context, drawStyle, listOf(contour.adaptivePositions()))
            }

            if (drawStyle.stroke != null) {
                when (drawStyle.quality) {
                    DrawQuality.PERFORMANCE -> when (contour.closed) {
                        true -> fastLineDrawer.drawLineLoops(context, drawStyle, listOf(contour.adaptivePositions()))
                        false -> fastLineDrawer.drawLineLoops(context, drawStyle, listOf(contour.adaptivePositions()))
                    }
                    DrawQuality.QUALITY -> when (contour.closed) {
                        true -> qualityLineDrawer.drawLineLoops(context, drawStyle, listOf(contour.adaptivePositions()))
                        false -> qualityLineDrawer.drawLineStrips(context, drawStyle, listOf(contour.adaptivePositions()))
                    }
                }
            }
        } else {
            throw RuntimeException("drawing contours requires a render target with a depth buffer attachment")
        }
    }

    fun contours(contours: List<ShapeContour>) {
        if (drawStyle.fill != null) {
            qualityPolygonDrawer.drawPolygons(context, drawStyle, contours.map { listOf(it.adaptivePositions()) })
        }

        if (drawStyle.stroke != null) {
            qualityLineDrawer.drawLineStrips(context, drawStyle, contours.map { it.adaptivePositions() })
        }
    }

    fun lineSegment(x0: Double, y0: Double, x1: Double, y1: Double) {
        lineSegment(Vector2(x0, y0), Vector2(x1, y1))
    }

    fun lineSegment(lineSegment: LineSegment) {
        lineSegment(lineSegment.start, lineSegment.end)
    }

    fun lineSegment(start: Vector2, end: Vector2) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineSegments2(context, drawStyle, listOf(start, end))
            DrawQuality.QUALITY -> qualityLineDrawer.drawLineStrips(context, drawStyle, listOf(listOf(start, end)))
        }
    }

    fun lineSegment(start: Vector3, end: Vector3) {
        fastLineDrawer.drawLineSegments3(context, drawStyle, listOf(start, end))
    }

    fun lineSegments(segments: List<Vector2>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineSegments2(context, drawStyle, segments)
            DrawQuality.QUALITY -> {

                val pairs = (0 until segments.size / 2).map {
                    listOf(segments[it * 2], segments[it * 2 + 1])
                }
                qualityLineDrawer.drawLineStrips(context, drawStyle, pairs)
            }
        }
    }

    fun lineSegments(segments: List<Vector2>, weights: List<Double>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineSegments2(context, drawStyle, segments)
            DrawQuality.QUALITY -> {

                val pairs = (0 until segments.size / 2).map {
                    listOf(segments[it * 2], segments[it * 2 + 1])
                }
                qualityLineDrawer.drawLineStrips(context, drawStyle, pairs, weights)
            }
        }
    }

    fun lineSegments3d(segments: List<Vector3>) {
        fastLineDrawer.drawLineSegments3(context, drawStyle, segments)
    }

    fun lineLoop(points: List<Vector2>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, listOf(points))
            DrawQuality.QUALITY -> qualityLineDrawer.drawLineLoops(context, drawStyle, listOf(points))
        }
    }

    fun lineLoops(loops: List<List<Vector2>>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, loops)
            DrawQuality.QUALITY -> qualityLineDrawer.drawLineLoops(context, drawStyle, loops)
        }
    }

    fun lineLoops(loops: List<List<Vector2>>, weights: List<Double>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, loops)
            DrawQuality.QUALITY -> qualityLineDrawer.drawLineLoops(context, drawStyle, loops, weights)
        }
    }

    fun lineStrip(points: List<Vector2>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, listOf(points))
            DrawQuality.QUALITY -> qualityLineDrawer.drawLineStrips(context, drawStyle, listOf(points))
        }
    }

    fun lineStrips(strips: List<List<Vector2>>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, strips)
            DrawQuality.QUALITY -> qualityLineDrawer.drawLineStrips(context, drawStyle, strips)
        }
    }

    fun lineStrips(strips: List<List<Vector2>>, weights: List<Double>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, strips)
            DrawQuality.QUALITY -> qualityLineDrawer.drawLineStrips(context, drawStyle, strips, weights)
        }
    }


    fun composition(composition: Composition) {
        pushStyle()
        fill = ColorRGBa.BLACK
        stroke = null

        fun node(compositionNode: CompositionNode) {
            pushModel()
            pushStyle()
            model *= compositionNode.transform

            when (compositionNode) {
                is ShapeNode -> {

                    compositionNode.fill.let {
                        if (it is Color) {
                            fill = it.color
                        }
                    }

                    compositionNode.stroke.let {
                        if (it is Color) {
                            stroke = it.color
                        }
                    }
                    shape(compositionNode.shape)
                }
                is TextNode -> TODO()
                is GroupNode -> compositionNode.children.forEach { node(it) }
            }
            popModel()
            popStyle()
        }
        node(composition.root)
        popStyle()
    }

    fun image(colorBuffer: ColorBuffer, source: Rectangle, target: Rectangle) {
        imageDrawer.drawImage(context, drawStyle, colorBuffer, listOf(source to target))
    }

    fun image(colorBuffer: ColorBuffer, x: Double, y: Double, width: Double = colorBuffer.width.toDouble(), height: Double = colorBuffer.height.toDouble()) {
        imageDrawer.drawImage(context, drawStyle, colorBuffer, x, y, width, height)
    }

    fun image(colorBuffer: ColorBuffer, position: Vector2, width: Double = colorBuffer.width.toDouble(), height: Double = colorBuffer.height.toDouble()) {
        imageDrawer.drawImage(context, drawStyle, colorBuffer, position.x, position.y, width, height)
    }

    fun image(colorBuffer: ColorBuffer) = image(colorBuffer, 0.0, 0.0)

    fun image(colorBuffer: ColorBuffer, rectangles: List<Pair<Rectangle, Rectangle>>) {
        imageDrawer.drawImage(context, drawStyle, colorBuffer, rectangles)
    }

    fun text(text: String, position: Vector2) {
        if (fontMap is FontImageMap) {
            fontImageMapDrawer.drawText(context, drawStyle, text, position.x, position.y)
        }
    }

    fun text(text: String, x: Double = 0.0, y: Double = 0.0) {
        if (fontMap is FontImageMap) {
            fontImageMapDrawer.drawText(context, drawStyle, text, x, y)
        }
    }

    fun texts(texts: List<String>, positions: List<Vector2>) {
        if (fontMap is FontImageMap) {
            fontImageMapDrawer.drawTexts(context, drawStyle, texts, positions)
        }
    }

    fun size(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    fun vertexBuffer(vertexBuffer: VertexBuffer, primitive: DrawPrimitive, vertexOffset: Int = 0, vertexCount: Int = vertexBuffer.vertexCount) {
        vertexBuffer(listOf(vertexBuffer), primitive, vertexOffset, vertexCount)
    }

    fun vertexBuffer(vertexBuffers: List<VertexBuffer>, primitive: DrawPrimitive, offset: Int = 0, vertexCount: Int = vertexBuffers[0].vertexCount) {
        vertexBufferDrawer.drawVertexBuffer(context, drawStyle, primitive, vertexBuffers, offset, vertexCount)
    }

    fun vertexBuffer(indexBuffer: IndexBuffer, vertexBuffers: List<VertexBuffer>, primitive: DrawPrimitive, offset: Int = 0, indexCount: Int = indexBuffer.indexCount) {
        vertexBufferDrawer.drawVertexBuffer(context, drawStyle, primitive, indexBuffer, vertexBuffers, offset, indexCount)
    }

    fun vertexBufferInstances(vertexBuffers: List<VertexBuffer>, instanceAttributes: List<VertexBuffer>, primitive: DrawPrimitive, instanceCount: Int, offset: Int = 0, vertexCount: Int = vertexBuffers[0].vertexCount) {
        vertexBufferDrawer.drawVertexBufferInstances(context, drawStyle, primitive, vertexBuffers, instanceAttributes, offset, vertexCount, instanceCount)
    }

    fun vertexBufferInstances(indexBuffer: IndexBuffer, vertexBuffers: List<VertexBuffer>, instanceAttributes: List<VertexBuffer>, primitive: DrawPrimitive, instanceCount: Int, offset: Int = 0, indexCount: Int = indexBuffer.indexCount) {
        vertexBufferDrawer.drawVertexBufferInstances(context, drawStyle, primitive, indexBuffer, vertexBuffers, instanceAttributes, offset, indexCount, instanceCount)
    }
}

/**
 * Pushes style, view- and projection matrix, calls function and pops.
 * @param function the function that is called in the isolation
 */
fun Drawer.isolated(function: Drawer.() -> Unit) {
    pushTransforms()
    pushStyle()
    function()
    popStyle()
    popTransforms()
}

/**
 * Pushes style, view- and projection matrix, sets render target, calls function and pops,
 * @param function the function that is called in the isolation
 */
fun Drawer.isolatedWithTarget(target: RenderTarget, function: Drawer.() -> Unit) {
    target.bind()
    isolated(function)
    target.unbind()
}