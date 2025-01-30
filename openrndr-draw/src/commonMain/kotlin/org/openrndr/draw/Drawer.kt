@file:Suppress("unused")

package org.openrndr.draw

import org.openrndr.shape.Circle
import org.openrndr.shape.Path3D
import org.openrndr.shape.Segment3D
import org.openrndr.shape.ShapeTopology
import org.openrndr.collections.pop
import org.openrndr.collections.push
import org.openrndr.color.ColorRGBa
import org.openrndr.internal.*
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.rotate
import org.openrndr.math.transforms.rotateZ
import org.openrndr.math.transforms.scale
import org.openrndr.math.transforms.translate
import org.openrndr.shape.*
import kotlin.jvm.JvmName
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.abs
import kotlin.math.log2
import kotlin.reflect.KMutableProperty0
import org.openrndr.math.transforms.lookAt as _lookAt
import org.openrndr.math.transforms.ortho as _ortho
import org.openrndr.math.transforms.perspective as _perspective


/**
 * Represents the target of a transformation within a graphical or computational system.
 *
 * The enumeration contains the following values:
 * - MODEL: Targeting the model transformation, which typically applies to object-space transformations.
 * - VIEW: Targeting the view transformation, which determines the position and orientation of the camera or observer.
 * - PROJECTION: Targeting the projection transformation, which handles the conversion of 3D space to a 2D representation.
 */
@Suppress("MemberVisibilityCanPrivate")

enum class TransformTarget {
    MODEL,
    VIEW,
    PROJECTION
}



/**
 * The `Drawer` class is a utility that provides various drawing operations such as drawing shapes,
 * transformations, and managing drawing states in a graphical rendering context.
 * It serves as the primary interface for rendering graphics and provides control
 * for styling and transformations.
 */
@Suppress("MemberVisibilityCanPrivate", "unused")
class Drawer(val driver: Driver) {

    /**
     * The bounds of the drawable area as a [Rectangle]
     */
    val bounds: Rectangle
        get() = Rectangle(Vector2(0.0, 0.0), width * 1.0, height * 1.0)

    private val drawStyles = ArrayDeque<DrawStyle>().apply {
        addLast(DrawStyle())
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
    val fontImageMapDrawer = FontImageMapDrawer()

    private val modelStack = ArrayDeque<Matrix44>()
    private val viewStack = ArrayDeque<Matrix44>()
    private val projectionStack = ArrayDeque<Matrix44>()

    val width: Int get() = RenderTarget.active.width
    val height: Int get() = RenderTarget.active.height

    private var modelViewScalingFactor = 1.0
    private val modelViewScaling: Double
        get() {
            return modelViewScalingFactor * RenderTarget.active.contentScale
        }


    /**
     * Represents the transformation matrix of a model.
     *
     * This variable holds a 4x4 matrix (`Matrix44`) that defines the current
     * transformation state of the model, such as translation, rotation,
     * and scaling. It defaults to the identity matrix (`Matrix44.IDENTITY`).
     *
     * When this variable is updated, it automatically recalculates the
     * `modelViewScalingFactor` using the product of the view matrix and
     * the updated model matrix. The scaling factor is derived from the
     * length of the transformed unit vector, providing a measure of
     * scaling applied along all axes.
     */
    var model: Matrix44 = Matrix44.IDENTITY
        set(value) {
            field = value
            modelViewScalingFactor = ((view * model).matrix33 * Vector3.UNIT_XYZ).length
        }


    /**
     * Represents the transformation matrix for the view in a 3D rendering context.
     *
     * This matrix is used to define the position, orientation, and scaling of the view.
     * When updated, it recalculates the `modelViewScalingFactor` based on the current
     * view and model matrices to account for changes in scaling.
     */
    var view: Matrix44 = Matrix44.IDENTITY
        set(value) {
            field = value
            modelViewScalingFactor = ((view * model).matrix33 * Vector3.UNIT_XYZ).length
        }



    /**
     * Represents a 4x4 transformation matrix used for projection in graphics rendering.
     * Typically utilized in 3D rendering to define views, perspectives, or transformations.
     * Defaults to the identity matrix, meaning no transformation is applied.
     */
    var projection: Matrix44 = Matrix44.IDENTITY


    /**
     * Provides the current drawing context for rendering operations. This includes information
     * about the model matrix, view matrix, projection matrix, the active render target's dimensions,
     * content scale, and the model-view scaling factor. The provided context is dynamically
     * derived based on the active rendering state.
     */
    val context: DrawContext
        get() = DrawContext(
            model,
            view,
            projection,
            RenderTarget.active.width,
            RenderTarget.active.height,
            RenderTarget.active.contentScale,
            modelViewScalingFactor
        )

    /**
     * Represents the style settings used for drawing operations.
     * This variable specifies how shapes or paths should be rendered,
     * such as stroke, fill, or other stylistic configurations.
     */
    var drawStyle = DrawStyle()


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

    /**
     * Rotates a transform to look at a given target point in 3D space.
     *
     * @param from The starting point of the look-at operation as a Vector3.
     * @param to The target point to look at as a Vector3.
     * @param up The "up" direction as a Vector3, defaulting to UNIT_Y.
     * @param target The transformation target, defaulting to TransformTarget.VIEW.
     */
    fun lookAt(
        from: Vector3,
        to: Vector3,
        up: Vector3 = Vector3.UNIT_Y,
        target: TransformTarget = TransformTarget.VIEW
    ) {
        transform(target) *= _lookAt(from, to, up)
    }

    /**
     * Scales the target transformation uniformly along all axes by the given scalar value.
     *
     * @param s The scalar value by which to scale the transformation.
     * @param target The target transformation to be scaled. Defaults to the MODEL transformation.
     */
    fun scale(s: Double, target: TransformTarget = TransformTarget.MODEL) {
        transform(target) *= Matrix44.scale(s, s, s)
    }

    /**
     * Applies non-uniform scale to the model matrix
     * @param x the scaling factor for the x-axis
     * @param y the scaling factor for the y-axis
     */
    fun scale(x: Double, y: Double, target: TransformTarget = TransformTarget.MODEL) {
        transform(target) *= Matrix44.scale(x, y, 1.0)
    }

    /**
     * Scales a transformation matrix by the specified factors along the x, y, and z axes.
     *
     * @param x The scale factor along the x-axis.
     * @param y The scale factor along the y-axis.
     * @param z The scale factor along the z-axis.
     * @param target The transformation target to which the scaling is applied. Defaults to TransformTarget.MODEL.
     */
    fun scale(x: Double, y: Double, z: Double, target: TransformTarget = TransformTarget.MODEL) {
        transform(target) *= Matrix44.scale(x, y, z)
    }

    /**
     * Applies a translation transformation to the specified target.
     *
     * @param t The translation vector to apply.
     * @param target The transformation target to which the translation is applied. Defaults to TransformTarget.MODEL.
     */
    fun translate(t: Vector2, target: TransformTarget = TransformTarget.MODEL) {
        transform(target) *= Matrix44.translate(t.vector3())
    }


    /**
     * Applies a translation transformation to the specified target.
     *
     * @param t The translation vector that specifies the direction and magnitude of the transformation.
     * @param target The transformation target to which the translation is applied. Defaults to TransformTarget.MODEL.
     */
    fun translate(t: Vector3, target: TransformTarget = TransformTarget.MODEL) {
        transform(target) *= Matrix44.translate(t)
    }

    /**
     * Translates a point or object by the given x and y offsets.
     *
     * @param x The x-axis offset by which the object is translated.
     * @param y The y-axis offset by which the object is translated.
     * @param target The transformation target that determines the coordinate system to apply the translation in (default is TransformTarget.MODEL).
     */
    fun translate(x: Double, y: Double, target: TransformTarget = TransformTarget.MODEL) {
        translate(x, y, 0.0, target)
    }
  
    /**
     * Translates the target by the specified x, y, and z values.
     *
     * @param x The translation value along the X-axis.
     * @param y The translation value along the Y-axis.
     * @param z The translation value along the Z-axis.
     * @param target The target of the transformation. Defaults to TransformTarget.MODEL.
     */
    fun translate(x: Double, y: Double, z: Double, target: TransformTarget = TransformTarget.MODEL) {
        transform(target) *= Matrix44.translate(Vector3(x, y, z))
    }

 
    /**
     * Rotates the target by the specified angle in degrees around the Z-axis.
     *
     * @param rotationInDegrees The angle in degrees by which to rotate.
     * @param target The target object to apply the rotation to. Defaults to TransformTarget.MODEL.
     */
    fun rotate(rotationInDegrees: Double, target: TransformTarget = TransformTarget.MODEL) {
        transform(target) *= Matrix44.rotateZ(rotationInDegrees)
    }

 
    /**
     * Rotates a transformation using the specified axis and angle in degrees,
     * applying the rotation to the given transformation target.
     *
     * @param axis the axis of rotation represented as a 3D vector.
     * @param rotationInDegrees the angle of rotation in degrees.
     * @param target the transformation target to which the rotation is applied;
     * defaults to TransformTarget.MODEL.
     */
    fun rotate(axis: Vector3, rotationInDegrees: Double, target: TransformTarget = TransformTarget.MODEL) {
        transform(target) *= Matrix44.rotate(axis, rotationInDegrees)
    }


    /**
     * Clears the current drawing buffer with the specified color.
     *
     * @param color The color used to clear the buffer. It is an instance of ColorRGBa.
     */
    fun clear(color: ColorRGBa) {
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

    /**
     * Pushes the current view onto the view stack and returns the top entry
     * of the stack after the push operation.
     *
     * @return The top entry of the view stack after the view has been pushed.
     */
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
     * Push the active model, view and projection matrices on their respective stacks
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
     * Pop the active the model, view and projection matrices from their respective stacks
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


    /**
     * The active line join method
     * @see strokeWeight
     * @see stroke
     * @see lineCap
     */
    var lineJoin: LineJoin
        set(value) {
            drawStyle.lineJoin = value
        }
        get() = drawStyle.lineJoin

    /**
     * When two line segments meet at a sharp angle and miter joins have been specified for [lineJoin],
     * it is possible for the miter to extend far beyond the thickness of the line stroking the path.
     * The miterlimit imposes a limit on the ratio of the miter length to the [strokeWeight].
     */
    var miterLimit: Double
        set(value) {
            drawStyle.miterLimit = value
        }
        get() = drawStyle.miterLimit

    /**
     * The active fontmap, default is null
     */
    var fontMap: FontMap?
        set(value) {
            drawStyle.fontMap = value
        }
        get() {
            if (drawStyle.fontMap == null) {
                drawStyle.fontMap = defaultFontMap
            }
            return drawStyle.fontMap
        }

    /**
     * Draws a rectangle based on the specified properties.
     *
     * @param rectangle The Rectangle object containing the x and y coordinates, width, and height of the rectangle.
     */
    fun rectangle(rectangle: Rectangle) {
        rectangleDrawer.drawRectangle(context, drawStyle, rectangle.x, rectangle.y, rectangle.width, rectangle.height)
    }

    /**
     * Draws a rectangle on the canvas based on the provided position and dimensions.
     *
     * @param x The x-coordinate of the top-left corner of the rectangle.
     * @param y The y-coordinate of the top-left corner of the rectangle.
     * @param width The width of the rectangle.
     * @param height The height of the rectangle. Defaults to the value of width if not specified.
     */
    fun rectangle(x: Double, y: Double, width: Double, height: Double = width) {
        rectangleDrawer.drawRectangle(context, drawStyle, x, y, width, height)
    }

    /**
     * Draws a rectangle on the canvas using the specified parameters.
     *
     * @param corner The top-left corner of the rectangle represented as a Vector2 object.
     * @param width The width of the rectangle.
     * @param height The height of the rectangle. If not specified, it defaults to the width, creating a square.
     */
    fun rectangle(corner: Vector2, width: Double, height: Double = width) {
        rectangleDrawer.drawRectangle(context, drawStyle, corner.x, corner.y, width, height)
    }

    /**
     * Draws a set of rectangles based on the given positions and dimensions.
     *
     * @param positions A list of Vector2 objects representing the positions where rectangles will be drawn.
     * @param width The width of each rectangle.
     * @param height The height of each rectangle. Defaults to the value of the width parameter.
     */
    fun rectangles(positions: List<Vector2>, width: Double, height: Double = width) {
        rectangleDrawer.drawRectangles(context, drawStyle, positions, width, height)
    }

    /**
     * Draws multiple rectangles based on the specified positions and dimensions.
     *
     * @param positions A list of Vector2 representing the positions of the rectangles.
     * @param dimensions A list of Vector2 representing the dimensions (width and height) of the rectangles.
     */
    fun rectangles(positions: List<Vector2>, dimensions: List<Vector2>) {
        rectangleDrawer.drawRectangles(context, drawStyle, positions, dimensions)
    }

    /**
     * Draws a list of rectangles on the specified context using the given draw style.
     *
     * @param rectangles A list of Rectangle objects to be drawn.
     */
    fun rectangles(rectangles: List<Rectangle>) {
        rectangleDrawer.drawRectangles(context, drawStyle, rectangles)
    }

    /**
     * Draws a specified number of rectangles from the given rectangle batch.
     *
     * @param batch The batch of rectangles to be drawn.
     * @param count The number of rectangles to draw from the batch. Defaults to the size of the batch.
     */
    fun rectangles(batch: RectangleBatch, count: Int = batch.size) {
        rectangleDrawer.drawRectangles(context, drawStyle, batch, count)
    }

    /**
     * Facilitates the construction and rendering of rectangles in a batch process.
     *
     * @param build A lambda function with receiver of type `RectangleBatchBuilder` to define
     * the attributes and properties of the rectangles to be drawn.
     */
    fun rectangles(build: RectangleBatchBuilder.() -> Unit) {
        val batchBuilder = RectangleBatchBuilder(this)
        batchBuilder.build()
        rectangleDrawer.ensureBatchSize(batchBuilder.entries.size)
        batchBuilder.batch(rectangleDrawer.batch)
        rectangleDrawer.drawRectangles(context, drawStyle, rectangleDrawer.batch, batchBuilder.entries.size)
    }

    /**
     * Draws a point in a 2D or 3D space.
     *
     * @param x The x-coordinate of the point.
     * @param y The y-coordinate of the point.
     * @param z The z-coordinate of the point. Defaults to 0.0 for 2D points.
     */
    fun point(x: Double, y: Double, z: Double = 0.0) {
        pointDrawer.drawPoint(context, drawStyle, x, y, z)
    }

    /**
     * Draws a point on the canvas at the specified vector's coordinates.
     *
     * @param vector The 2D vector containing the x and y coordinates where the point will be drawn.
     */
    fun point(vector: Vector2) {
        pointDrawer.drawPoint(context, drawStyle, vector.x, vector.y, 0.0)
    }

    /**
     * Draws a point in 3D space using the specified vector.
     *
     * @param vector The 3D vector containing the coordinates (x, y, z) of the point to be drawn.
     */
    fun point(vector: Vector3) {
        pointDrawer.drawPoint(context, drawStyle, vector.x, vector.y, vector.z)
    }

    /**
     * Draws a list of 2D points using the specified context and draw style.
     *
     * @param points The list of 2D points to be drawn, represented as Vector2 objects.
     */
    @JvmName("points2D")
    fun points(points: List<Vector2>) {
        pointDrawer.drawPoints(context, drawStyle, points)
    }

    /**
     * Draws a list of 3D points using the specified drawing style and context.
     *
     * @param points The list of 3D points to be drawn.
     */
    @JvmName("points3D")
    fun points(points: List<Vector3>) {
        pointDrawer.drawPoints(context, drawStyle, points)
    }

    /**
     * Draws a batch of points using the provided configuration builder.
     *
     * @param build A lambda with receiver of type PointBatchBuilder that is used
     *              to configure the batch of points to be drawn.
     */
    fun points(build: PointBatchBuilder.() -> Unit) {
        val batchBuilder = PointBatchBuilder(this)
        batchBuilder.build()
        pointDrawer.ensureBatchSize(batchBuilder.entries.size)
        batchBuilder.batch(pointDrawer.batch)
        pointDrawer.drawPoints(context, drawStyle, pointDrawer.batch, batchBuilder.entries.size)
    }

    /**
     * Draws a specified number of points from the given point batch.
     *
     * @param batch The batch of points to be drawn.
     * @param count The number of points to draw from the batch. If not specified, it defaults to the size of the batch.
     */
    fun points(batch: PointBatch, count: Int = batch.size) {
        pointDrawer.drawPoints(context, drawStyle, batch, count)
    }

    /**
     * Draws a circle with the specified parameters.
     *
     * @param x The x-coordinate of the center of the circle.
     * @param y The y-coordinate of the center of the circle.
     * @param radius The radius of the circle.
     */
    fun circle(x: Double, y: Double, radius: Double) {
        circleDrawer.drawCircle(context, drawStyle, x, y, radius)
    }

    /**
     * Draws a circle at the specified position with the given radius.
     *
     * @param position The center position of the circle as a Vector2 object.
     * @param radius The radius of the circle as a Double.
     */
    fun circle(position: Vector2, radius: Double) {
        circleDrawer.drawCircle(context, drawStyle, position.x, position.y, radius)
    }

    /**
     * Draws a circle on the given context using the specified draw style and circle properties.
     *
     * @param circle The Circle object containing the center coordinates and radius to define the circle to be drawn.
     */
    fun circle(circle: Circle) {
        circleDrawer.drawCircle(context, drawStyle, circle.center.x, circle.center.y, circle.radius)
    }

    /**
     * Draws multiple circles at specified positions with a given radius.
     *
     * @param positions A list of positions where each circle will be drawn. Each position is represented as a Vector2 object.
     * @param radius The radius of the circles to be drawn.
     */
    fun circles(positions: List<Vector2>, radius: Double) {
        circleDrawer.drawCircles(context, drawStyle, positions, radius)
    }

    /**
     * Draws multiple circles on a given context using specified positions and radii.
     *
     * @param positions A list of Vector2 objects representing the positions of the circle centers.
     * @param radii A list of Double values representing the radii of the circles, corresponding to each position.
     */
    fun circles(positions: List<Vector2>, radii: List<Double>) {
        circleDrawer.drawCircles(context, drawStyle, positions, radii)
    }

    /**
     * Draws the given list of circles using the specified drawing context and style.
     *
     * @param circles A list of Circle objects to be drawn.
     */
    fun circles(circles: List<Circle>) {
        circleDrawer.drawCircles(context, drawStyle, circles)
    }

    /**
     * Draws multiple circles in a batch using the specified drawing context and style.
     *
     * @param batch The batch of circles to be drawn.
     * @param count The number of circles to draw from the batch. Defaults to the size of the batch.
     */
    fun circles(batch: CircleBatch, count: Int = batch.size) {
        circleDrawer.drawCircles(context, drawStyle, batch, count)
    }

    /**
     * Draws a batch of circles on a canvas using the provided building instructions.
     *
     * @param build A lambda with receiver that defines the properties and parameters for
     * creating a batch of circles using the CircleBatchBuilder.
     */
    fun circles(build: CircleBatchBuilder.() -> Unit) {
        val batchBuilder = CircleBatchBuilder(this)
        batchBuilder.build()
        circleDrawer.ensureBatchSize(batchBuilder.entries.size)
        batchBuilder.batch(circleDrawer.batch)
        circleDrawer.drawCircles(context, drawStyle, circleDrawer.batch, batchBuilder.entries.size)
    }


    /**
     * Renders a given shape on the current render target. Supports shapes with different topology types: CLOSED, OPEN, and MIXED.
     *
     * @param shape The shape to be rendered. It contains the geometric data and topology information for rendering.
     */
    fun shape(shape: Shape) {
        val distanceTolerance = 0.5 / (modelViewScaling * log2(strokeWeight).coerceAtLeast(1.0))
        val fringeWidth = 1.0 / modelViewScaling
        if (abs(modelViewScaling) < 1E-6) {
            return
        }

        if (RenderTarget.active.hasStencilBuffer) {
            when (shape.topology) {
                ShapeTopology.CLOSED -> {
                    val closedPC = shape.contours.map { it.adaptivePositionsAndCorners(distanceTolerance) }
                    val closedP = closedPC.map { it.first }
                    val closedC = closedPC.map { it.second }
                    qualityPolygonDrawer.drawPolygon(
                        context, drawStyle,
                        closedP, closedC, fringeWidth
                    )
                    qualityLineDrawer.drawLineLoops(context, drawStyle, closedP, closedC, fringeWidth)
                }

                ShapeTopology.OPEN -> {
                    val openPC = shape.openContours.map { it.adaptivePositionsAndCorners(distanceTolerance) }
                    val openP = openPC.map { it.first }
                    val openC = openPC.map { it.second }
                    qualityLineDrawer.drawLineStrips(context, drawStyle, openP, openC, fringeWidth)
                }

                ShapeTopology.MIXED -> {
                    val closedPC = shape.closedContours.map { it.adaptivePositionsAndCorners(distanceTolerance) }
                    val closedP = closedPC.map { it.first }
                    val closedC = closedPC.map { it.second }
                    qualityPolygonDrawer.drawPolygon(context, drawStyle, closedP, closedC, fringeWidth)
                    val openPC = shape.openContours.map { it.adaptivePositionsAndCorners(distanceTolerance) }
                    val openP = openPC.map { it.first }
                    val openC = openPC.map { it.second }
                    qualityLineDrawer.drawLineStrips(context, drawStyle, openP, openC, fringeWidth)
                }
            }
        } else {
            error("Drawing shapes requires a render target with a stencil attachment.")
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
     * Draws a single [ShapeContour] using [fill], [stroke] and [strokeWeight] settings
     */
    fun contour(contour: ShapeContour) {
        val distanceTolerance = 0.5 / (modelViewScaling * log2(strokeWeight).coerceAtLeast(1.0))
        val fringeWidth = 1.0 / modelViewScaling
        if (abs(modelViewScaling) < 1E-6) {
            return
        }

        if (RenderTarget.active.hasStencilBuffer) {
            if (drawStyle.fill != null && contour.closed) {
                val apc = contour.adaptivePositionsAndCorners(distanceTolerance)
                val ap = listOf(apc.first)
                val ac = listOf(apc.second)

                qualityPolygonDrawer.drawPolygon(context, drawStyle, ap, ac, fringeWidth)
            }

            if (drawStyle.stroke != null && drawStyle.strokeWeight > 1E-4) {
                when (drawStyle.quality) {
                    DrawQuality.PERFORMANCE -> when (contour.closed) {
                        true -> fastLineDrawer.drawLineLoops(
                            context,
                            drawStyle,
                            listOf(contour.adaptivePositions(distanceTolerance))
                        )

                        false -> fastLineDrawer.drawLineLoops(
                            context,
                            drawStyle,
                            listOf(contour.adaptivePositions(distanceTolerance))
                        )
                    }

                    DrawQuality.QUALITY -> {
                        val apc = contour.adaptivePositionsAndCorners(distanceTolerance)
                        val ap = listOf(apc.first)
                        val ac = listOf(apc.second)
                        when (contour.closed) {
                            true -> qualityLineDrawer.drawLineLoops(context, drawStyle, ap, ac, fringeWidth)
                            false -> qualityLineDrawer.drawLineStrips(context, drawStyle, ap, ac, fringeWidth)
                        }
                    }
                }
            }
        } else {
            error("Drawing contours requires a render target with a stencil attachment")
        }
    }

    /**
     * Draws org.openrndr.shape.contours using [fill], [stroke] and [strokeWeight] settings
     */
    fun contours(contours: List<ShapeContour>) {
        for (contour in contours) {
            contour(contour)
        }
        /*
        if (drawStyle.fill != null) {
            qualityPolygonDrawer.drawPolygons(context, drawStyle, org.openrndr.shape.contours.filter { it.closed }.map { listOf(it.adaptivePositions()) })
        }

        if (drawStyle.stroke != null) {
            qualityLineDrawer.drawLineStrips(context, drawStyle, org.openrndr.shape.contours.map { it.adaptivePositions() })
        }
        */

    }

    /**
     * Draws a line segment between two points in a 2D space.
     *
     * @param x0 The x-coordinate of the starting point of the line segment.
     * @param y0 The y-coordinate of the starting point of the line segment.
     * @param x1 The x-coordinate of the ending point of the line segment.
     * @param y1 The y-coordinate of the ending point of the line segment.
     */
    fun lineSegment(x0: Double, y0: Double, x1: Double, y1: Double) {
        lineSegment(Vector2(x0, y0), Vector2(x1, y1))
    }

    /**
     * Draws a line segment using the specified `LineSegment`.
     *
     * @param lineSegment The `LineSegment` object containing the start and end points of the line segment.
     */
    fun lineSegment(lineSegment: LineSegment) {
        lineSegment(lineSegment.start, lineSegment.end)
    }

    /**
     * Draws a line segment from [start] to [end] using 2d coordinates
     */
    fun lineSegment(start: Vector2, end: Vector2) {
        val fringeWidth = 1.0 / modelViewScaling
        if (abs(modelViewScaling) < 1E-6) {
            return
        }

        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineSegments(context, drawStyle, listOf(start, end))
            DrawQuality.QUALITY -> qualityLineDrawer.drawLineStrips(
                context,
                drawStyle,
                listOf(listOf(start, end)),
                listOf(listOf(true, true)),
                fringeWidth
            )
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
        val fringeWidth = 0.5 / modelViewScaling
        if (abs(modelViewScaling) < 1E-6) {
            return
        }

        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineSegments(context, drawStyle, segments)
            DrawQuality.QUALITY -> {

                val pairs = (0 until segments.size / 2).map {
                    listOf(segments[it * 2], segments[it * 2 + 1])
                }
                val corners = pairs.map { it.map { true } }
                qualityLineDrawer.drawLineStrips(context, drawStyle, pairs, corners, fringeWidth)
            }
        }
    }

    /**
     * Draws a series of line segments with specified weights
     *
     * @param segments A list of `Vector2` instances representing the points of the line segments.
     *                 Each consecutive pair of points defines a single line segment.
     * @param weights  A list of `Double` values representing the weights (thickness) of each line segment.
     */
    fun lineSegments(segments: List<Vector2>, weights: List<Double>) {
        val fringeWidth = 0.5 / modelViewScaling
        if (abs(modelViewScaling) < 1E-6) {
            return
        }

        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineSegments(context, drawStyle, segments)
            DrawQuality.QUALITY -> {
                val pairs = (0 until segments.size / 2).map {
                    listOf(segments[it * 2], segments[it * 2 + 1])
                }
                val corners = pairs.map { it.map { true } }
                qualityLineDrawer.drawLineStrips(context, drawStyle, pairs, corners, weights, fringeWidth)
            }
        }
    }

    /**
     * Draws line segments using the specified list of 3D vectors.
     *
     * @param segments A list of [Vector3] objects representing the line segments to be drawn.
     */
    @JvmName("lineSegments3d")
    fun lineSegments(segments: List<Vector3>) {
        when (drawStyle.quality) {
            DrawQuality.QUALITY -> meshLineDrawer.drawLineSegments(context, drawStyle, segments)
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineSegments(context, drawStyle, segments)
        }
    }

    /**
     * Draws a series of line segments based on the provided 3D vectors and weights.
     *
     * @param segments A list of Vector3 objects representing the points that define the line segments.
     * @param weights A list of Double values representing the weights for each line segment, influencing their appearance.
     */
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

    @JvmName("lineSegmentsFromLineSegmentList")
    fun lineSegments(segments: List<LineSegment>) {
        val fringeWidth = 1.0 / modelViewScaling
        if (abs(modelViewScaling) < 1E-6) {
            return
        }

        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> {
                // TODO: a faster version would pass `segments` to
                // fastLineDrawer as is to avoid iterating over the points twice
                val points = segments.map { it.start } + segments.last().end
                fastLineDrawer.drawLineSegments(context, drawStyle, points)
            }

            DrawQuality.QUALITY -> {
                val pairs = segments.map {
                    listOf(it.start, it.end)
                }
                qualityLineDrawer.drawLineStrips(context, drawStyle, pairs, pairs.map { it.map { true } }, fringeWidth)
            }
        }
    }

    /**
     * Renders a closed loop of connected line segments given a list of points.
     *
     * @param points A list of 2D vectors representing the points to create the line loop.
     * Each point in the list is connected sequentially, with the last point connecting back to the first.
     */
    fun lineLoop(points: List<Vector2>) {
        val fringeWidth = 1.0 / modelViewScaling
        if (abs(modelViewScaling) < 1E-6) {
            return
        }

        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, listOf(points))
            DrawQuality.QUALITY -> qualityLineDrawer.drawLineLoops(
                context,
                drawStyle,
                listOf(points),
                listOf(points.map { true }),
                fringeWidth
            )
        }
    }

    /**
     * Draws a closed loop connecting given 3D points.
     *
     * @param points A list of `Vector3` objects representing the 3D points to form the closed loop.
     */
    @JvmName("lineLoop3d")
    fun lineLoop(points: List<Vector3>) {
        if (abs(modelViewScaling) < 1E-6) {
            return
        }
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, listOf(points))
            DrawQuality.QUALITY -> meshLineDrawer.drawLineStrips(
                context,
                drawStyle,
                listOf(points),
                closed = listOf(true)
            )
        }
    }

    /**
     * Renders a list of line loops based on the current drawing style and quality settings.
     *
     * @param loops A list of line loops, where each loop is represented as a list of Vector2 points.
     */
    fun lineLoops(loops: List<List<Vector2>>) {
        val fringeWidth = 1.0 / modelViewScaling
        if (abs(modelViewScaling) < 1E-6) {
            return
        }
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, loops)
            DrawQuality.QUALITY -> qualityLineDrawer.drawLineLoops(
                context,
                drawStyle,
                loops,
                loops.map { it.map { true } },
                fringeWidth
            )
        }
    }

    /**
     * Draws a series of line loops based on the provided list of loops.
     *
     * @param loops A list of line loops where each loop is represented as a list of `Vector3` points.
     */
    @JvmName("lineLoops3d")
    fun lineLoops(loops: List<List<Vector3>>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, loops)
            DrawQuality.QUALITY -> meshLineDrawer.drawLineStrips(
                context,
                drawStyle,
                loops,
                closed = List(loops.size) { true })
        }
    }

    /**
     * Renders a series of line loops with specified weights.
     *
     * @param loops A list of line loops, where each loop is represented as a list of 2D vectors.
     * @param weights A list of weights corresponding to each line in the loops, determining their thickness.
     */
    fun lineLoops(loops: List<List<Vector2>>, weights: List<Double>) {
        val fringeWidth = 1.0 / modelViewScaling
        if (abs(modelViewScaling) < 1E-6) {
            return
        }
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, loops)
            DrawQuality.QUALITY -> qualityLineDrawer.drawLineLoops(
                context,
                drawStyle,
                loops,
                loops.map { it.map { true } },
                weights,
                fringeWidth
            )
        }
    }

    /**
     * Draws line loops in either performance or quality mode based on the current draw style.
     *
     * @param loops A list of lists representing the points for each line loop, where each inner list defines a single loop using Vector3 points.
     * @param weights A list of weights corresponding to each line loop, used in the quality drawing mode.
     */
    @JvmName("lineLoops3d)")
    fun lineLoops(loops: List<List<Vector3>>, weights: List<Double>) {
        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, loops)
            DrawQuality.QUALITY -> meshLineDrawer.drawLineStrips(
                context,
                drawStyle,
                loops,
                weights,
                closed = List(loops.size) { true })
        }
    }

    /**
     * Draws a line strip with 2d coordinates
     */
    fun lineStrip(points: List<Vector2>) {
        val fringeWidth = 1.0 / modelViewScaling
        if (abs(modelViewScaling) < 1E-6) {
            return
        }

        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, listOf(points))
            DrawQuality.QUALITY -> qualityLineDrawer.drawLineStrips(
                context,
                drawStyle,
                listOf(points),
                listOf(points.map { true }),
                fringeWidth
            )
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
        val fringeWidth = 1.0 / modelViewScaling
        if (abs(modelViewScaling) < 1E-6) {
            return
        }

        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, strips)
            DrawQuality.QUALITY -> qualityLineDrawer.drawLineStrips(
                context,
                drawStyle,
                strips,
                strips.map { it.map { true } },
                fringeWidth
            )
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
        val fringeWidth = 1.0 / modelViewScaling
        if (abs(modelViewScaling) < 1E-6) {
            return
        }

        when (drawStyle.quality) {
            DrawQuality.PERFORMANCE -> fastLineDrawer.drawLineLoops(context, drawStyle, strips)
            DrawQuality.QUALITY -> qualityLineDrawer.drawLineStrips(
                context,
                drawStyle,
                strips,
                strips.map { it.map { true } },
                weights,
                fringeWidth
            )
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
     * Draw a single segment
     * @see contour
     */
    fun segment(segment: Segment2D) {
        val distanceTolerance = 0.5 / (modelViewScaling * log2(strokeWeight).coerceAtLeast(1.0))
        if (abs(modelViewScaling) < 1E-6) {
            return
        }
        lineStrip(segment.adaptivePositions(distanceTolerance))
    }

    /**
     * Draws a single 3D segment
     */
    fun segment(segment: Segment3D) {
        lineStrip(segment.adaptivePositions())
    }

    /**
     * Draws a list of 2D segments
     */
    @JvmName("segments2d")
    fun segments(segments: List<Segment2D>) {
        val distanceTolerance = 0.5 / (modelViewScaling * log2(strokeWeight).coerceAtLeast(1.0))
        if (abs(modelViewScaling) < 1E-6) {
            return
        }
        lineStrips(segments.map { it.adaptivePositions(distanceTolerance) })
    }

    /**
     * Draws a list of 3D segments
     */
    fun segments(segments: List<Segment3D>) {
        lineStrips(segments.map { it.adaptivePositions() })
    }

    /**
     * Draws a list of 3D segments, each with their weight and color
     */
    fun segments(segments: List<Segment3D>, weights: List<Double>, colors: List<ColorRGBa>) {
        lineStrips(segments.map { it.adaptivePositions() }, weights, colors)
    }

    /**
     * Draws a single 3D path
     * @param path the path to draw
     */
    fun path(path: Path3D) {
        if (path.closed) {
            lineLoop(path.adaptivePositions(0.03).dropLast(1))
        } else {
            lineStrip(path.adaptivePositions(0.03))
        }
    }

    /**
     * Draw a list of [Path3D]
     * @param paths the paths to draw
     * @param weights a list of weights
     * @param colors a list of colors
     */
    fun paths(paths: List<Path3D>, weights: List<Double> = emptyList(), colors: List<ColorRGBa> = emptyList()) {
        meshLineDrawer.drawLineStrips(
            context,
            drawStyle,
            paths.map { p -> p.adaptivePositions(0.03).let { if (p.closed) it.dropLast(1) else it } },
            weights,
            colors,
            closed = paths.map { it.closed })
    }


    /**
     * Draws a [source] area of an image ([ColorBuffer]) into a [target] area
     */
    fun image(colorBuffer: ColorBuffer, source: Rectangle, target: Rectangle) {
        imageDrawer.drawImage(context, drawStyle, colorBuffer, listOf(source to target))
    }

    /**
     * Draws an image ([ColorBuffer]) into a [target] area
     */
    fun image(colorBuffer: ColorBuffer, target: Rectangle) {
        imageDrawer.drawImage(context, drawStyle, colorBuffer, target.x, target.y, target.width, target.height)
    }

    /**
     * Draws an image with its top-left corner at ([x], [y]) and dimensions ([width], [height])
     */
    fun image(
        colorBuffer: ColorBuffer,
        x: Double,
        y: Double,
        width: Double = colorBuffer.width.toDouble(),
        height: Double = colorBuffer.height.toDouble()
    ) {
        imageDrawer.drawImage(context, drawStyle, colorBuffer, x, y, width, height)
    }

    /**
     * Draws an image with its top-left corner at ([position]) and dimensions ([width], [height])
     */
    fun image(
        colorBuffer: ColorBuffer,
        position: Vector2,
        width: Double = colorBuffer.width.toDouble(),
        height: Double = colorBuffer.height.toDouble()
    ) {
        imageDrawer.drawImage(context, drawStyle, colorBuffer, position.x, position.y, width, height)
    }

    /**
     * Draws an image with its top-left corner at (0,0)
     */
    fun image(colorBuffer: ColorBuffer) = image(colorBuffer, 0.0, 0.0)

    /**
     * Draws an image onto the specified context using the provided color buffer and rectangles.
     *
     * @param colorBuffer The color buffer containing the image to be drawn.
     * @param rectangles A list of pairs of rectangles where the first rectangle in each pair represents
     * the source region in the color buffer, and the second rectangle represents the target region
     * in the context where the portion of the image will be drawn.
     */
    fun image(colorBuffer: ColorBuffer, rectangles: List<Pair<Rectangle, Rectangle>>) {
        imageDrawer.drawImage(context, drawStyle, colorBuffer, rectangles)
    }

    /**
     * Draws an image using an ArrayTexture as source
     */
    fun image(
        arrayTexture: ArrayTexture, layer: Int = 0, x: Double = 0.0, y: Double = 0.0,
        width: Double = arrayTexture.width.toDouble(), height: Double = arrayTexture.height.toDouble()
    ) {
        imageDrawer.drawImage(context, drawStyle, arrayTexture, layer, x, y, width, height)
    }

    /**
     * Draws an image from a specified layer of an ArrayTexture onto a target rectangle.
     *
     * @param arrayTexture the ArrayTexture that contains the image layers.
     * @param layer the layer of the texture to use, defaults to 0 if not specified.
     * @param source the rectangle defining the region of the texture to draw.
     * @param target the rectangle defining the region on the target to draw the texture onto.
     */
    fun image(arrayTexture: ArrayTexture, layer: Int = 0, source: Rectangle, target: Rectangle) {
        imageDrawer.drawImage(context, drawStyle, arrayTexture, listOf(layer), listOf(source to target))
    }

    /**
     * Draws an image using the given array texture, layers, and mapping of rectangles.
     *
     * @param arrayTexture The texture containing the image data to be drawn.
     * @param layers The list of layer indices to be used from the array texture.
     * @param rectangles A list of pairs where each pair maps a rectangle in the array texture
     * to a corresponding rectangle in the target space.
     */
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

    /**
     * Draws the specified vertex buffers using the given draw primitive and parameters.
     *
     * @param vertexBuffers A list of vertex buffers to be drawn.
     * @param primitive The type of drawing primitive to use for rendering (e.g., triangles, lines).
     * @param offset The starting index in the vertex buffer from which to begin drawing. Default is 0.
     * @param vertexCount The number of vertices to be drawn from the vertex buffer. Default is the vertex count of the first buffer in the list.
     */
    fun vertexBuffer(
        vertexBuffers: List<VertexBuffer>,
        primitive: DrawPrimitive,
        offset: Int = 0,
        vertexCount: Int = vertexBuffers[0].vertexCount
    ) {
        vertexBufferDrawer.drawVertexBuffer(context, drawStyle, primitive, vertexBuffers, offset, vertexCount)
    }

    /**
     * Draws a vertex buffer using the specified parameters.
     *
     * @param indexBuffer The index buffer that contains indices for drawing vertices.
     * @param vertexBuffers A list of vertex buffers containing vertex data.
     * @param primitive The primitive type used for drawing (e.g., triangle, line).
     * @param offset The starting offset in the index buffer to begin drawing. Defaults to 0.
     * @param indexCount The number of indices to use for drawing. Defaults to the total index count in the index buffer.
     */
    fun vertexBuffer(
        indexBuffer: IndexBuffer,
        vertexBuffers: List<VertexBuffer>,
        primitive: DrawPrimitive,
        offset: Int = 0,
        indexCount: Int = indexBuffer.indexCount
    ) {
        vertexBufferDrawer.drawVertexBuffer(
            context,
            drawStyle,
            primitive,
            indexBuffer,
            vertexBuffers,
            offset,
            indexCount
        )
    }

    /**
     * Issues a draw call for rendering geometry using multiple vertex buffers with instancing.
     *
     * @param vertexBuffers A list of vertex buffers containing per-vertex attributes for the geometry.
     * @param instanceAttributes A list of vertex buffers containing per-instance attributes for instanced rendering.
     * @param primitive The type of geometric primitive to be drawn (e.g., triangles, lines).
     * @param instanceCount The number of instances of the geometry to render.
     * @param offset The starting position in the vertex buffer from which to begin drawing.
     * @param vertexCount The number of vertices to be rendered. Defaults to the vertex count of the first vertex buffer.
     */
    fun vertexBufferInstances(
        vertexBuffers: List<VertexBuffer>,
        instanceAttributes: List<VertexBuffer>,
        primitive: DrawPrimitive,
        instanceCount: Int,
        offset: Int = 0,
        vertexCount: Int = vertexBuffers[0].vertexCount
    ) {
        vertexBufferDrawer.drawVertexBufferInstances(
            context,
            drawStyle,
            primitive,
            vertexBuffers,
            instanceAttributes,
            offset,
            vertexCount,
            instanceCount
        )
    }

    /**
     * Draws instances of a vertex buffer with the specified parameters.
     *
     * @param indexBuffer The index buffer containing indices to define the vertices to be drawn.
     * @param vertexBuffers A list of vertex buffers that define the vertex data for rendering.
     * @param instanceAttributes A list of vertex buffers that define attributes for each instance.
     * @param primitive The primitive type used for rendering (e.g., triangles, lines).
     * @param instanceCount The number of instances to render.
     * @param offset The starting index in the index buffer for drawing, defaults to 0.
     * @param indexCount The number of indices to use for drawing, defaults to the total index count in the index buffer.
     */
    fun vertexBufferInstances(
        indexBuffer: IndexBuffer,
        vertexBuffers: List<VertexBuffer>,
        instanceAttributes: List<VertexBuffer>,
        primitive: DrawPrimitive,
        instanceCount: Int,
        offset: Int = 0,
        indexCount: Int = indexBuffer.indexCount
    ) {
        vertexBufferDrawer.drawVertexBufferInstances(
            context,
            drawStyle,
            primitive,
            indexBuffer,
            vertexBuffers,
            instanceAttributes,
            offset,
            indexCount,
            instanceCount
        )
    }

    /**
     * Returns a mutable property reference to a specific transformation matrix
     * based on the provided transform target.
     *
     * @param transform the target transformation type to retrieve the matrix for;
     *                  can be projection, model, or view.
     * @return a mutable property reference to the corresponding transformation matrix.
     */
    private fun transform(transform: TransformTarget): KMutableProperty0<Matrix44> {
        return when (transform) {
            TransformTarget.PROJECTION -> this::projection
            TransformTarget.MODEL -> this::model
            TransformTarget.VIEW -> this::view
        }
    }
}


/**
 * Executes a given function within an isolated context where transforms and styles
 * are managed. Ensures that the applied transforms and styles are reverted after
 * the function execution, maintaining isolation for the block of code.
 *
 * @param function A lambda function that operates within the isolated drawing context.
 * @see Drawer.isolatedWithTarget
 */
@OptIn(ExperimentalContracts::class)
fun Drawer.isolated(function: Drawer.() -> Unit) {
    contract {
        callsInPlace(function, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    pushTransforms()
    pushStyle()
    try {
        function()
    } finally {
        popStyle()
        popTransforms()
    }
}


/**
 * Executes the given function within an isolated drawing context bound to the specified render target.
 *
 * This method binds the provided render target, executes the function within an isolated context,
 * and then ensures that the render target is unbound after execution.
 *
 * @param target The render target to bind during the execution of the provided function.
 * @param function The drawing function to execute within the isolated context.
 * @see Drawer.isolated
 */
@OptIn(ExperimentalContracts::class)
fun Drawer.isolatedWithTarget(target: RenderTarget, function: Drawer.() -> Unit) {
    contract {
        callsInPlace(function, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    target.bind()
    try {
        isolated(function)
    } finally {
        target.unbind()
    }
}


private operator fun KMutableProperty0<Matrix44>.timesAssign(matrix: Matrix44) = set(get() * matrix)
