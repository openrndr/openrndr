@file:Suppress("unused")

package org.openrndr.draw

import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.internal.*
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.YPolarity
import org.openrndr.math.transforms.rotate
import org.openrndr.math.transforms.rotateZ
import org.openrndr.math.transforms.scale
import org.openrndr.math.transforms.translate
import org.openrndr.shape.*
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.util.*
import org.openrndr.math.transforms.lookAt as _lookAt
import org.openrndr.math.transforms.ortho as _ortho
import org.openrndr.math.transforms.perspective as _perspective

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

/**
 * A render target that wraps around the back-buffer
 */
interface ProgramRenderTarget : RenderTarget {
    val program: Program
    override val width get() = program.width
    override val height get() = program.height
}

enum class DrawQuality {
    QUALITY,
    PERFORMANCE
}

/**
 * The Drawer
 */
@Suppress("MemberVisibilityCanPrivate", "unused")
class Drawer(val driver: Driver) {

    /**
     * The bounds of the drawable area as a [Rectangle]
     */
    val bounds: Rectangle
        get() = Rectangle(Vector2(0.0, 0.0), width * 1.0, height * 1.0)

    private val drawStyles = Stack<DrawStyle>().apply {
        push(DrawStyle())
    }
    private var rectangleDrawer = RectangleDrawer()
    private var vertexBufferDrawer = VertexBufferDrawer()
    private var circleDrawer = CircleDrawer()
    private var pointDrawer = PointDrawer()
    private var imageDrawer = ImageDrawer()
    private var fastLineDrawer = PerformanceLineDrawer()
    private val meshLineDrawer by lazy { MeshLineDrawer() }
    private var qualityLineDrawer = QualityLineDrawer()
    private var qualityPolygonDrawer = QualityPolygonDrawer()
    internal val fontImageMapDrawer = FontImageMapDrawer()

    private val modelStack = Stack<Matrix44>()
    private val viewStack = Stack<Matrix44>()
    private val projectionStack = Stack<Matrix44>()

    var width: Int = 0
    var height: Int = 0

    var model: Matrix44 = Matrix44.IDENTITY /** The active model matrix */
    var view: Matrix44 = Matrix44.IDENTITY /** The active view matrix */
    var projection: Matrix44 = Matrix44.IDENTITY /** The active projection matrix */

    /**
     * The draw context holds references to model, view, projection matrices, width, height and content-scale
     */
    val context: DrawContext
        get() = DrawContext(model, view, projection, width, height, RenderTarget.active.contentScale)

    var drawStyle = DrawStyle() /** The active draw style */

    /**
     * @see isolatedWithTarget
     * @see isolated
     */
    fun withTarget(target: RenderTarget, action: Drawer.() -> Unit) {
        target.bind()
        this.action()
        target.unbind()
    }

    /**
     *  Resets state stacks and load default values for draw style and transformations.
     *  This destroys the state stacks, consider using defaults() instead of reset()
     *  @see defaults
     */
    @Deprecated("reset is considered harmful, use defaults()")
    fun reset() {
        viewStack.clear()
        modelStack.clear()
        projectionStack.clear()
        drawStyles.clear()
        defaults()
    }

    /**
     * Loads default values for draw style and transformations
     */
    fun defaults() {
        drawStyle = DrawStyle()
        ortho()
        view = Matrix44.IDENTITY
        model = Matrix44.IDENTITY
    }

    /**
     * Sets the [projection] matrix to orthogonal using the sizes of a [RenderTarget]
     * @param renderTarget the render target to take the sizes from
     */
    fun ortho(renderTarget: RenderTarget) {
        ortho(0.0, renderTarget.width.toDouble(), renderTarget.height.toDouble(), 0.0, -1.0, 1.0)
    }

    /**
     * Sets the [projection] matrix to orthogonal using the drawer's current size
     */
    fun ortho() {
        ortho(0.0, width.toDouble(), height.toDouble(), 0.0, -1.0, 1.0)
    }

    /**
     * Sets the [projection] matrix to orthogonal using [left], [right], [bottom], [top], [near], [far]
     * @param left left value
     * @param right right value
     * @param bottom bottom value
     * @param top top value
     * @param near near value
     * @param far far value
     * @see perspective
     */
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
     *  @see ortho
     */
    fun perspective(fovY: Double, aspectRatio: Double, zNear: Double, zFar: Double) {
        projection = _perspective(fovY, aspectRatio, zNear, zFar)
    }

    fun lookAt(from: Vector3, to: Vector3, up: Vector3 = Vector3.UNIT_Y) {
        view *= _lookAt(from, to, up)
    }

    /**
     * Apply a uniform scale to the model matrix
     * @param s the scaling factor
     */
    fun scale(s: Double) {
        model *= Matrix44.scale(s, s, s)
    }

    /**
     * Applies non-uniform scale to the model matrix
     * @param x the scaling factor for the x-axis
     * @param y the scaling factor for the y-axis
     */
    fun scale(x: Double, y: Double) {
        model *= Matrix44.scale(x, y, 1.0)
    }

    /**
     * Applies non-uniform scale to the model matrix
     * @param x the scaling factor for the x-axis
     * @param y the scaling factor for the y-axis
     * @param z the scaling factor for the y-axis
     * @see translate
     * @see scale
     */
    fun scale(x: Double, y: Double, z: Double) {
        model *= Matrix44.scale(x, y, z)
    }

    /**
     * Applies a two-dimensional translation to the model matrix
     */
    fun translate(t: Vector2) {
        model *= Matrix44.translate(t.vector3())
    }

    /**
     * Applies three-dimensional translation to the model matrix
     */
    fun translate(t: Vector3) {
        model *= Matrix44.translate(t)
    }

    /**
     * Applies a two-dimensional translation to the model matrix
     */
    fun translate(x: Double, y: Double) {
        translate(x, y, 0.0)
    }

    /**
     * Applies a three-dimensional translation to the model matrix
     */
    fun translate(x: Double, y: Double, z: Double) {
        model *= Matrix44.translate(Vector3(x, y, z))
    }

    /**
     * Applies a rotation over the z-axis to the model matrix
     * @param rotationInDegrees the rotation in degrees
     */
    fun rotate(rotationInDegrees: Double) {
        model *= Matrix44.rotateZ(rotationInDegrees)
    }

    /**
     * Applies a rotation over an arbitrary axis to the model matrix
     * @param axis the axis to rotate over, will be normalized
     * @param rotationInDegrees the rotation in degrees
     */
    fun rotate(axis: Vector3, rotationInDegrees: Double) {
        model *= Matrix44.rotate(axis, rotationInDegrees)
    }

    fun background(r: Double, g: Double, b: Double, a: Double) {
        driver.clear(r, g, b, a)
    }

    fun background(color: ColorRGBa) {
        driver.clear(color)
    }

    /**
     * Push the active draw style on the draw style stack
     * @see drawStyle
     * @see popStyle
     */
    fun pushStyle(): DrawStyle = drawStyles.push(drawStyle.copy())

    /**
     * Pop the draw style from the draw style stack
     * @see drawStyle
     * @see popStyle
     */
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

    /**
     * Push the active projection matrix on the projection state stack
     */
    fun pushProjection(): Matrix44 = projectionStack.push(projection)

    /**
     * Pop the active projection matrix from the projection state stack
     */
    fun popProjection() {
        projection = projectionStack.pop()
    }

    /**
     * Push the active model, view and projection matrices on their according stacks
     * @see pushTransforms
     * @see popTransforms
     * @see isolated
     * @see isolatedWithTarget
     */
    fun pushTransforms() {
        pushModel()
        pushView()
        pushProjection()
    }

    /**
     * Pop the active the model, view and projection matrices from their according stacks
     * @see pushTransforms
     * @see popTransforms
     * @see isolated
     * @see isolatedWithTarget
     */
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

    /**
     * The active fill color
     * @see stroke
     */
    var fill: ColorRGBa?
        set(value) {
            drawStyle.fill = value
        }
        get() = drawStyle.fill

    /**
     * The active stroke color
     * @see fill
     * @see strokeWeight
     */
    var stroke: ColorRGBa?
        set(value) {
            drawStyle.stroke = value
        }
        get() = drawStyle.stroke

    /**
     * The active stroke weight
     * @see stroke
     * @see lineCap
     * @see lineJoin
     */
    var strokeWeight: Double
        set(value) {
            drawStyle.strokeWeight = value
        }
        get() = drawStyle.strokeWeight

    /**
     * The active line cap method
     * @see strokeWeight
     * @see stroke
     * @see lineJoin
     */
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

    /**
     * The active fontmap, default is null
     */
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

    /**
     * Draw a single point
     * @see points
     * @see circle
     */
    fun point(x: Double, y: Double, z: Double = 0.0) {
        pointDrawer.drawPoint(context, drawStyle, x, y, z)
    }

    /**
     * Draw a single point
     * @see points
     * @see circle
     */
    fun point(vector: Vector2) {
        pointDrawer.drawPoint(context, drawStyle, vector.x, vector.y, 0.0)
    }

    /**
     * Draw a single point
     * @see points
     * @see circle
     */
    fun point(vector: Vector3) {
        pointDrawer.drawPoint(context, drawStyle, vector.x, vector.y, vector.z)
    }

    /**
     * Draw a list of 2D points
     * @see point
     * @see circle
     */
    @JvmName("points2D")
    fun points(points: List<Vector2>) {
        pointDrawer.drawPoints(context, drawStyle, points)
    }

    /**
     * Draw a list of 3D points
     * @see point
     * @see circle
     * @see circles
     */
    @JvmName("points3D")
    fun points(points: List<Vector3>) {
        pointDrawer.drawPoints(context, drawStyle, points)
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

    fun circles(vertexBuffer: VertexBuffer, count: Int = vertexBuffer.vertexCount) {
        circleDrawer.drawCircles(context, drawStyle, vertexBuffer, count)
    }

    /**
     * Draws a single [Shape] using [fill], [stroke] and [strokeWeight] settings
     * @see contour
     * @see shapes
     * @see contours
     * @see composition
     */
    fun shape(shape: Shape) {
        if (RenderTarget.active.hasDepthBuffer) {
            when (shape.topology) {
                ShapeTopology.CLOSED -> qualityPolygonDrawer.drawPolygon(context, drawStyle,
                        shape.contours.map { it.adaptivePositions() })
                ShapeTopology.OPEN -> qualityLineDrawer.drawLineStrips(context, drawStyle, shape.contours.map { it.adaptivePositions() })
                ShapeTopology.MIXED -> {
                    qualityPolygonDrawer.drawPolygon(context, drawStyle, shape.closedContours.map { it.adaptivePositions() })
                    qualityLineDrawer.drawLineStrips(context, drawStyle, shape.openContours.map { it.adaptivePositions() })
                }
            }
        } else {
            throw RuntimeException("drawing shapes requires a render target with a depth buffer attachment")
        }
    }

    /**
     * Draws shapes using [fill], [stroke] and [strokeWeight] settings
     * @see shape
     * @see contour
     * @see contours
     */
    fun shapes(shapes: List<Shape>) {
        shapes.forEach {
            shape(it)
        }
    }

    /**
     * Draw a single segment
     * @see contour
     */
    fun segment(segment: Segment) {
        contour(ShapeContour(listOf(segment), false, YPolarity.CW_NEGATIVE_Y))
    }

    /**
     * Draws a single [ShapeContour] using [fill], [stroke] and [strokeWeight] settings
     */
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

    /**
     * Draws contours using [fill], [stroke] and [strokeWeight] settings
     */
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

    /**
     * Draws a line segment from [start] to [end] using 2d coordinates
     */
    fun lineSegment(start: Vector2, end: Vector2) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineSegments(context, drawStyle, listOf(start, end))
            DrawQuality.QUALITY -> qualityLineDrawer.drawLineStrips(context, drawStyle, listOf(listOf(start, end)))
        }
    }

    /**
     * Draws a line segment from [start] to [end] using 3d coordinates
     */
    fun lineSegment(start: Vector3, end: Vector3) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineSegments(context, drawStyle, listOf(start, end))
            DrawQuality.QUALITY -> meshLineDrawer.drawLineSegments(context, drawStyle, listOf(start, end))
        }
    }

    fun lineSegments(segments: List<Vector2>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineSegments(context, drawStyle, segments)
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
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineSegments(context, drawStyle, segments)
            DrawQuality.QUALITY -> {
                val pairs = (0 until segments.size / 2).map {
                    listOf(segments[it * 2], segments[it * 2 + 1])
                }
                qualityLineDrawer.drawLineStrips(context, drawStyle, pairs, weights)
            }
        }
    }

    @JvmName("lineSegments3d")
    fun lineSegments(segments: List<Vector3>) {
        when (drawStyle.quality) {
            DrawQuality.QUALITY -> meshLineDrawer.drawLineSegments(context, drawStyle, segments)
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineSegments(context, drawStyle, segments)
        }
    }

    @JvmName("lineSegments3d")
    fun lineSegments(segments: List<Vector3>, weights: List<Double>) {
        when (drawStyle.quality) {
            DrawQuality.QUALITY -> meshLineDrawer.drawLineSegments(context, drawStyle, segments, weights)
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineSegments(context, drawStyle, segments)
        }
    }

    @JvmName("lineSegments3d")
    fun lineSegments(segments: List<Vector3>, weights: List<Double>, colors: List<ColorRGBa>) {
        when (drawStyle.quality) {
            DrawQuality.QUALITY -> meshLineDrawer.drawLineSegments(context, drawStyle, segments, weights, colors)
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineSegments(context, drawStyle, segments)
        }
    }

    fun lineLoop(points: List<Vector2>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, listOf(points))
            DrawQuality.QUALITY -> qualityLineDrawer.drawLineLoops(context, drawStyle, listOf(points))
        }
    }

    @JvmName("lineLoop3d")
    fun lineLoop(points: List<Vector3>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, listOf(points))
            DrawQuality.QUALITY -> meshLineDrawer.drawLineLoops(context, drawStyle, listOf(points))
        }
    }

    fun lineLoops(loops: List<List<Vector2>>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, loops)
            DrawQuality.QUALITY -> qualityLineDrawer.drawLineLoops(context, drawStyle, loops)
        }
    }

    @JvmName("lineLoops3d")
    fun lineLoops(loops: List<List<Vector3>>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, loops)
            DrawQuality.QUALITY -> meshLineDrawer.drawLineLoops(context, drawStyle, loops)
        }
    }

    fun lineLoops(loops: List<List<Vector2>>, weights: List<Double>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, loops)
            DrawQuality.QUALITY -> qualityLineDrawer.drawLineLoops(context, drawStyle, loops, weights)
        }
    }

    @JvmName("lineLoops3d)")
    fun lineLoops(loops: List<List<Vector3>>, weights: List<Double>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, loops)
            DrawQuality.QUALITY -> meshLineDrawer.drawLineLoops(context, drawStyle, loops, weights)
        }
    }

    /**
     * Draws a line strip with 2d coordinates
     */
    fun lineStrip(points: List<Vector2>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, listOf(points))
            DrawQuality.QUALITY -> qualityLineDrawer.drawLineStrips(context, drawStyle, listOf(points))
        }
    }

    /**
     * Draws a line strip with 3d coordinates
     */
    @JvmName("lineStrip3d")
    fun lineStrip(points: List<Vector3>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, listOf(points))
            DrawQuality.QUALITY -> meshLineDrawer.drawLineStrips(context, drawStyle, listOf(points))
        }
    }

    /**
     * Draws line strips with 3d coordinates
     */
    fun lineStrips(strips: List<List<Vector2>>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, strips)
            DrawQuality.QUALITY -> qualityLineDrawer.drawLineStrips(context, drawStyle, strips)
        }
    }

    /**
     * Draws line strips with 3d coordinates
     */
    @JvmName("lineStrips3d")
    fun lineStrips(strips: List<List<Vector3>>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, strips)
            DrawQuality.QUALITY -> meshLineDrawer.drawLineStrips(context, drawStyle, strips)
        }
    }

    /**
     * Draws line strips with 2d coordinates and stroke weights per strip
     */
    fun lineStrips(strips: List<List<Vector2>>, weights: List<Double>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, strips)
            DrawQuality.QUALITY -> qualityLineDrawer.drawLineStrips(context, drawStyle, strips, weights)
        }
    }

    /**
     * Draws line strips with 3d coordinates and stroke weights per strip
     */
    @JvmName("lineStrips3d")
    fun lineStrips(strips: List<List<Vector3>>, weights: List<Double>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, strips)
            DrawQuality.QUALITY -> meshLineDrawer.drawLineStrips(context, drawStyle, strips, weights)
        }
    }

    /**
     * Draws line strips with 3d coordinates and stroke weights per strip
     */
    @JvmName("lineStrips3d")
    fun lineStrips(strips: List<List<Vector3>>, weights: List<Double>, colors: List<ColorRGBa>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, strips)
            DrawQuality.QUALITY -> meshLineDrawer.drawLineStrips(context, drawStyle, strips, weights, colors)
        }
    }

    /**
     * Draws a single 3D segment
     */
    fun segment(segment: Segment3D) {
        lineStrip(segment.sampleAdaptive())
    }

    /**
     * Draws a list of 3D segments
     */
    fun segments(segments: List<Segment3D>) {
        lineStrips(segments.map { it.sampleAdaptive() })
    }

    /**
     * Draws a list of 3D segments, each with their weight and color
     */
    fun segments(segments: List<Segment3D>, weights: List<Double>, colors: List<ColorRGBa>) {
        lineStrips(segments.map { it.sampleAdaptive() }, weights, colors)
    }

    /**
     * Draws a single 3D path
     * @param path the path to draw
     */
    fun path(path: Path3D) {
        lineStrip(path.adaptivePositions(0.03))
    }

    /**
     * Draws a [Composition]
     * @param composition The composition to draw
     * @see contour
     * @see contours
     * @see shape
     * @see shapes
     */
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

                    compositionNode.strokeWeight.let {
                        if (it is StrokeWeight) {
                            strokeWeight = it.weight
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

    /**
     * Draws a [source] area of an image ([ColorBuffer]) into a [target] area
     */
    fun image(colorBuffer: ColorBuffer, source: Rectangle, target: Rectangle) {
        imageDrawer.drawImage(context, drawStyle, colorBuffer, listOf(source to target))
    }

    /**
     * Draws an image with its top-left corner at ([x], [y]) and dimensions ([width], [height])
     */
    fun image(colorBuffer: ColorBuffer, x: Double, y: Double, width: Double = colorBuffer.width.toDouble(), height: Double = colorBuffer.height.toDouble()) {
        imageDrawer.drawImage(context, drawStyle, colorBuffer, x, y, width, height)
    }

    /**
     * Draws an image with its top-left corner at ([position]) and dimensions ([width], [height])
     */
    fun image(colorBuffer: ColorBuffer, position: Vector2, width: Double = colorBuffer.width.toDouble(), height: Double = colorBuffer.height.toDouble()) {
        imageDrawer.drawImage(context, drawStyle, colorBuffer, position.x, position.y, width, height)
    }

    /**
     * Draws an image with its top-left corner at (0,0)
     */
    fun image(colorBuffer: ColorBuffer) = image(colorBuffer, 0.0, 0.0)

    fun image(colorBuffer: ColorBuffer, rectangles: List<Pair<Rectangle, Rectangle>>) {
        imageDrawer.drawImage(context, drawStyle, colorBuffer, rectangles)
    }


    /**
     * Draws an image using an ArrayTexture as source
     */
    fun image(arrayTexture: ArrayTexture, layer: Int = 0, x: Double = 0.0, y: Double = 0.0,
              width: Double = arrayTexture.width.toDouble(), height: Double = arrayTexture.height.toDouble()) {
        imageDrawer.drawImage(context, drawStyle, arrayTexture, layer, x, y, width, height)
    }

    fun image(arrayTexture: ArrayTexture, layer: Int = 0, source: Rectangle, target: Rectangle) {
        imageDrawer.drawImage(context, drawStyle, arrayTexture, listOf(layer), listOf(source to target))
    }

    fun image(arrayTexture: ArrayTexture, layers: List<Int>, rectangles: List<Pair<Rectangle, Rectangle>>) {
        imageDrawer.drawImage(context, drawStyle, arrayTexture, layers, rectangles)
    }

    /**
     * Draws [text] at ([position])
     */
    fun text(text: String, position: Vector2) {
        if (fontMap is FontImageMap) {
            fontImageMapDrawer.drawText(context, drawStyle, text, position.x, position.y)
        }
    }

    /**
     * Draws [text] at ([x], [y])
     */
    fun text(text: String, x: Double = 0.0, y: Double = 0.0) {
        if (fontMap is FontImageMap) {
            fontImageMapDrawer.drawText(context, drawStyle, text, x, y)
        }
    }

    /**
     * Draws [texts] at [positions])
     */
    fun texts(texts: List<String>, positions: List<Vector2>) {
        if (fontMap is FontImageMap) {
            fontImageMapDrawer.drawTexts(context, drawStyle, texts, positions)
        }
    }

    fun size(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    /**
     * Draws a [VertexBuffer] using [primitive]
     */
    fun vertexBuffer(
            vertexBuffer: VertexBuffer,
            primitive: DrawPrimitive,
            vertexOffset: Int = 0,
            vertexCount: Int = vertexBuffer.vertexCount
    ) {
        vertexBuffer(listOf(vertexBuffer), primitive, vertexOffset, vertexCount)
    }

    fun vertexBuffer(
            vertexBuffers: List<VertexBuffer>,
            primitive: DrawPrimitive,
            offset: Int = 0,
            vertexCount: Int = vertexBuffers[0].vertexCount
    ) {
        vertexBufferDrawer.drawVertexBuffer(context, drawStyle, primitive, vertexBuffers, offset, vertexCount)
    }

    fun vertexBuffer(
            indexBuffer: IndexBuffer,
            vertexBuffers: List<VertexBuffer>,
            primitive: DrawPrimitive,
            offset: Int = 0,
            indexCount: Int = indexBuffer.indexCount
    ) {
        vertexBufferDrawer.drawVertexBuffer(context, drawStyle, primitive, indexBuffer, vertexBuffers, offset, indexCount)
    }

    fun vertexBufferInstances(
            vertexBuffers: List<VertexBuffer>,
            instanceAttributes: List<VertexBuffer>,
            primitive: DrawPrimitive,
            instanceCount: Int,
            offset: Int = 0,
            vertexCount: Int = vertexBuffers[0].vertexCount
    ) {
        vertexBufferDrawer.drawVertexBufferInstances(context, drawStyle, primitive, vertexBuffers, instanceAttributes, offset, vertexCount, instanceCount)
    }

    fun vertexBufferInstances(
            indexBuffer: IndexBuffer,
            vertexBuffers: List<VertexBuffer>,
            instanceAttributes: List<VertexBuffer>,
            primitive: DrawPrimitive,
            instanceCount: Int,
            offset: Int = 0,
            indexCount: Int = indexBuffer.indexCount
    ) {
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
