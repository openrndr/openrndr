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
/**
 * VertexBuffer Layout describes how data is organized in the VertexBuffer
 */
class VertexFormat {

    var items: MutableList<VertexElement> = mutableListOf()
    private var vertexSize = 0

    val size get() = vertexSize

    /**
     * Appends a position component to the layout
     * @param dimensions
     */
    fun position(dimensions: Int): VertexFormat = attribute("position", floatTypeFromDimensions(dimensions))

    private fun floatTypeFromDimensions(dimensions: Int): VertexElementType {
        return when (dimensions) {
            1 -> VertexElementType.FLOAT32
            2 -> VertexElementType.VECTOR2_FLOAT32
            3 -> VertexElementType.VECTOR3_FLOAT32
            4 -> VertexElementType.VECTOR4_FLOAT32
            else -> throw IllegalArgumentException("dimensions can only be 1, 2, 3 or 4 (got $dimensions)")
        }
    }

    /**
     * Appends a normal component to the layout
     * @param dimensions the number of dimensions of the normal vector
     */
    fun normal(dimensions: Int): VertexFormat = attribute("normal", floatTypeFromDimensions(dimensions))

    /**
     * Appends a color attribute to the layout
     * @param dimensions
     */
    fun color(dimensions: Int): VertexFormat = attribute("color", floatTypeFromDimensions(dimensions))

    fun textureCoordinate(dimensions: Int = 2, index: Int = 0): VertexFormat = attribute("texCoord$index", floatTypeFromDimensions(dimensions))


    /**
     * Adds a custom attribute to the layout
     * @param name the name of the attribute
     * *
     * @param dimensions the dimensionality of the attribute
     * *
     * @param type the type of the attribute.
     * *
     * @return
     */
    fun attribute(name: String, type: VertexElementType, arraySize: Int = 1): VertexFormat {
        val offset = items.sumBy { it.arraySize * it.type.sizeInBytes }
        val item = VertexElement(name, offset, type, arraySize)
        items.add(item)
        vertexSize += type.sizeInBytes * arraySize
        return this
    }


    override fun toString(): String {
        return "VertexFormat{" +
                "items=" + items +
                ", vertexSize=" + vertexSize +
                '}'
    }

}

fun vertexFormat(builder: VertexFormat.() -> Unit): VertexFormat {
    return VertexFormat().apply { builder() }
}


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


interface VertexBufferShadow {
    val vertexBuffer: VertexBuffer

    fun upload(offset: Int = 0, size: Int = vertexBuffer.vertexCount * vertexBuffer.vertexFormat.size)
    fun uploadElements(elementOffset: Int = 0, elementCount: Int = vertexBuffer.vertexCount) {
        upload(elementOffset * vertexBuffer.vertexFormat.size, elementCount * vertexBuffer.vertexFormat.size)
    }

    fun download()
    fun destroy()
    fun writer(): BufferWriter
}

interface VertexBuffer {
    companion object {
        fun createDynamic(format: VertexFormat, vertexCount: Int): VertexBuffer = Driver.instance.createDynamicVertexBuffer(format, vertexCount)
        //fun createStatic(format: VertexFormat, buffer:Buffer):VertexBuffer
    }

    val vertexFormat: VertexFormat
    val vertexCount: Int

    val shadow: VertexBufferShadow

    fun write(data: ByteBuffer, offset: Int = 0)
    fun read(data: ByteBuffer, offset: Int = 0)

    fun destroy()

    fun put(putter: BufferWriter.() -> Unit): Int {
        val w = shadow.writer()
        w.rewind()
        w.putter()
        if (w.position % vertexFormat.size != 0) {
            throw RuntimeException("incomplete vertices written. likely violating the specified vertex format")
        }
        val count = w.positionElements
        shadow.upload()
        w.rewind()
        return count
    }
}

fun vertexBuffer(vertexFormat: VertexFormat, vertexCount: Int): VertexBuffer {
    return VertexBuffer.createDynamic(vertexFormat, vertexCount)
}

/**
 * Texture wrapping mode
 */
enum class WrapMode {
    CLAMP_TO_EDGE,
    REPEAT,
    MIRRORED_REPEAT
}

/**
 * Texture filters used for minification
 */
enum class MinifyingFilter {
    NEAREST,
    LINEAR,
    NEAREST_MIPMAP_NEAREST,
    LINEAR_MIPMAP_NEAREST,
    NEAREST_MIPMAP_LINEAR,
    LINEAR_MIPMAP_LINEAR
}

/**
 * Texture filters for magnification
 */
enum class MagnifyingFilter {
    NEAREST,
    LINEAR
}

/**
 * File format used while saving to file
 */
enum class FileFormat {
    JPG,
    PNG,
}

interface BufferWriter {
    fun write(vararg v: Vector3) {
        v.forEach { write(it) }
    }

    fun write(v: Vector3)
    fun write(v: Vector2)
    fun write(v: Vector4)
    fun write(v: Matrix44)
    fun write(v: Float)
    fun write(x: Float, y: Float)
    fun write(x: Float, y: Float, z: Float)
    fun write(x: Float, y: Float, z: Float, w: Float)
    fun write(v: ColorRGBa)
    fun write(a: FloatArray, offset: Int = 0, size: Int = a.size)

    fun rewind()
    var position: Int //(position: Int)
    var positionElements: Int//(element: Int)
}


interface ColorBufferShadow {
    val colorBuffer: ColorBuffer
    val buffer: ByteBuffer

    fun upload()
    fun download()
    fun destroy()

    fun writer(): BufferWriter
    fun write(x: Int, y: Int, r: Double, g: Double, b: Double, a: Double)
    fun write(x: Int, y: Int, color: ColorRGBa) {
        write(x, y, color.r, color.g, color.b, color.a)
    }
    fun write(x: Int, y: Int, r: Float, g: Float, b: Float, a: Float) {
        write(x, y, r.toDouble(), g.toDouble(), b.toDouble(), a.toDouble())
    }

    fun read(x: Int, y: Int): ColorRGBa
}


interface ColorBuffer {
    val width: Int
    val height: Int
    val contentScale: Double
    val format: ColorFormat
    val type: ColorType

    val bounds: Rectangle get() = Rectangle(Vector2.ZERO, width * 1.0, height * 1.0)

    val effectiveWidth: Int get() = (width * contentScale).toInt()
    val effectiveHeight: Int get() = (height * contentScale).toInt()


    fun saveToFile(file: File, fileFormat: FileFormat = FileFormat.PNG)
    fun destroy()
    fun bind(unit: Int)

    fun write(buffer: ByteBuffer)
    fun read(buffer: ByteBuffer)
    fun generateMipmaps()

    var wrapU: WrapMode
    var wrapV: WrapMode

    var filterMin: MinifyingFilter
    var filterMag: MagnifyingFilter

    val shadow: ColorBufferShadow
    var flipV: Boolean


    fun filter(filterMin: MinifyingFilter, filterMag: MagnifyingFilter) {
        this.filterMin = filterMin
        this.filterMag = filterMag
    }

    companion object {
        fun create(width: Int, height: Int, contentScale: Double = 1.0, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8): ColorBuffer {
            return Driver.instance.createColorBuffer(width, height, contentScale, format, type)
        }

        fun fromUrl(url: String): ColorBuffer {
            return Driver.instance.createColorBufferFromUrl(url)
        }

        fun fromFile(filename: String): ColorBuffer {
            return Driver.instance.createColorBufferFromFile(filename)
        }
    }
}

fun colorBuffer(width: Int, height: Int, contentScale: Double = 1.0, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8): ColorBuffer {
    return ColorBuffer.create(width, height, contentScale, format, type)
}

interface BufferTextureShadow {
    val bufferTexture: BufferTexture

    fun upload(offset: Int, size: Int)
    fun download()
    fun destroy()

    fun writer(): BufferWriter
}

interface BufferTexture {
    val shadow: BufferTextureShadow

    val format: ColorFormat
    val type: ColorType

    val elementCount: Int

    fun destroy()
    fun bind(unit: Int)

    companion object {
        fun create(elementCount: Int, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.FLOAT32): BufferTexture {
            return Driver.instance.createBufferTexture(elementCount, format, type)
        }
    }

    fun put(putter: BufferWriter.() -> Unit): Int {
        val w = shadow.writer()
        w.rewind()
        w.putter()
        val count = w.positionElements
        shadow.upload(0, w.position)
        w.rewind()
        return count
    }
}


interface DepthBuffer {

    val width: Int
    val height: Int
    val format: DepthFormat

    val hasStencil: Boolean
        get() = format == DepthFormat.DEPTH32F_STENCIL8 || format == DepthFormat.DEPTH24_STENCIL8

    companion object {
        fun create(width: Int, height: Int, format: DepthFormat = DepthFormat.DEPTH24_STENCIL8): DepthBuffer = Driver.instance.createDepthBuffer(width, height, format)
    }

    fun destroy()
    fun bind(textureUnit: Int)
}

interface ProgramRenderTarget : RenderTarget {
    val program: Program
    override val width get() = program.width
    override val height get() = program.height
}

@Suppress("unused")
class RenderTargetBuilder(private val renderTarget: RenderTarget) {
    fun colorBuffer(colorBuffer: ColorBuffer) {
        renderTarget.attach(colorBuffer)
    }

    fun colorBuffer(name: String, colorBuffer: ColorBuffer) {
        renderTarget.attach(name, colorBuffer)
    }

    fun colorBuffer(name: String, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8) {
        val cb = ColorBuffer.create(renderTarget.width, renderTarget.height, renderTarget.contentScale, format, type)
        renderTarget.attach(name, cb)
    }

    fun colorBuffer(format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8) {
        val cb = ColorBuffer.create(renderTarget.width, renderTarget.height, renderTarget.contentScale, format, type)
        renderTarget.attach(cb)
    }

    fun depthBuffer(format: DepthFormat = DepthFormat.DEPTH24_STENCIL8) {
        renderTarget.attach(DepthBuffer.create(renderTarget.effectiveWidth, renderTarget.effectiveHeight, format))
    }

    fun depthBuffer(depthBuffer: DepthBuffer) {
        renderTarget.attach(depthBuffer)
    }
}

interface RenderTarget {
    val width: Int
    val height: Int
    val contentScale: Double

    val effectiveWidth: Int get() = (width * contentScale).toInt()
    val effectiveHeight: Int get() = (height * contentScale).toInt()

    val colorBuffers: List<ColorBuffer>
    val depthBuffer: DepthBuffer?

    companion object {
        fun create(width: Int, height: Int, contentScale: Double): RenderTarget = Driver.instance.createRenderTarget(width, height, contentScale)
        val active: RenderTarget
            get() = Driver.instance.activeRenderTarget
    }

    fun attach(name: String, colorBuffer: ColorBuffer)

    fun attach(colorBuffer: ColorBuffer)
    fun attach(depthBuffer: DepthBuffer)
    fun detachColorBuffers()
    fun detachDepthBuffer()
    fun destroy()

    fun colorBuffer(index: Int): ColorBuffer
    fun colorBuffer(name: String): ColorBuffer
    fun colorBufferIndex(name: String): Int

    fun bind()
    fun unbind()

    val hasDepthBuffer: Boolean
    val hasColorBuffer: Boolean
}

fun renderTarget(width: Int, height: Int, contentScale: Double = 1.0, builder: RenderTargetBuilder.() -> Unit): RenderTarget {
    val renderTarget = RenderTarget.create(width, height, contentScale)
    RenderTargetBuilder(renderTarget).builder()
    return renderTarget
}

enum class DrawQuality {
    QUALITY,
    PERFORMANCE
}

class StencilStyle {
    var stencilFailOperation = StencilOperation.KEEP
    var depthFailOperation = StencilOperation.KEEP
    var depthPassOperation = StencilOperation.KEEP
    var stencilTestMask = 0xff
    var stencilTestReference = 0
    var stencilWriteMask = 0xff
    var stencilTest = StencilTest.DISABLED

    fun stencilFunc(stencilTest: StencilTest, ref: Int, mask: Int) {
        this.stencilTest = stencilTest
        this.stencilTestReference = ref
        this.stencilWriteMask = mask
    }

    fun stencilOp(stencilFail: StencilOperation, depthTestFail: StencilOperation, depthTestPass: StencilOperation) {
        stencilFailOperation = stencilFail
        depthFailOperation = depthTestFail
        depthPassOperation = depthTestPass
    }
}

private var lastModel = Matrix44.IDENTITY
private var lastModelNormal = Matrix44.IDENTITY
private var lastView = Matrix44.IDENTITY
private var lastViewNormal = Matrix44.IDENTITY

private var contextBlock: UniformBlock? = null
private var useContextBlock = true

@Suppress("MemberVisibilityCanPrivate")
data class DrawContext(val model: Matrix44, val view: Matrix44, val projection: Matrix44, val width: Int, val height: Int, val contentScale: Double) {
    fun applyToShader(shader: Shader) {

        if (contextBlock == null) {
            contextBlock = shader.createBlock("ContextBlock")
        }

        if (!useContextBlock) {
            if (shader.hasUniform("u_viewMatrix")) {
                shader.uniform("u_viewMatrix", view)
            }
            if (shader.hasUniform("u_modelMatrix")) {
                shader.uniform("u_modelMatrix", model)
            }
            if (shader.hasUniform("u_projectionMatrix")) {
                shader.uniform("u_projectionMatrix", projection)
            }
            if (shader.hasUniform("u_viewDimensions")) {
                shader.uniform("u_viewDimensions", Vector2(width.toDouble(), height.toDouble()))
            }
            if (shader.hasUniform("u_modelNormalMatrix")) {
                val normalMatrix = if (model === lastModel) lastModelNormal else {
                    lastModelNormal = if (model !== Matrix44.IDENTITY) normalMatrix(model) else Matrix44.IDENTITY
                    lastModel = model
                    lastModelNormal
                }
                shader.uniform("u_modelNormalMatrix", normalMatrix)
            }
            if (shader.hasUniform("u_viewNormalMatrix")) {
                val normalMatrix = if (view === lastView) lastViewNormal else {
                    lastViewNormal = if (view !== Matrix44.IDENTITY) normalMatrix(view) else Matrix44.IDENTITY
                    lastView = view
                    lastViewNormal
                }
                shader.uniform("u_viewNormalMatrix", normalMatrix)
            }
            if (shader.hasUniform("u_contentScale")) {
                shader.uniform("u_contentScale", contentScale)
            }
        } else {
            contextBlock?.apply {
                uniform("u_viewMatrix", view)
                uniform("u_modelMatrix", model)
                uniform("u_projectionMatrix", projection)
                uniform("u_viewDimensions", Vector2(width.toDouble(), height.toDouble()))
                run {
                    val normalMatrix = if (model === lastModel) lastModelNormal else {
                        lastModelNormal = if (model !== Matrix44.IDENTITY) normalMatrix(model) else Matrix44.IDENTITY
                        lastModel = model
                        lastModelNormal
                    }
                    shader.uniform("u_modelNormalMatrix", normalMatrix)
                }
                run {
                    val normalMatrix = if (view === lastView) lastViewNormal else {
                        lastViewNormal = if (view !== Matrix44.IDENTITY) normalMatrix(view) else Matrix44.IDENTITY
                        lastView = view
                        lastViewNormal
                    }
                    shader.uniform("u_viewNormalMatrix", normalMatrix)
                }
                shader.uniform("u_contentScale", contentScale)
                if (dirty) {
                    upload()
                }
                shader.block("ContextBlock", this)
            }
        }
    }
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

    fun rectangles(positions: List<Vector2>, width: Double, height: Double) {
        rectangleDrawer.drawRectangles(context, drawStyle, positions, width, height)
    }

    fun rectangles(positions: List<Vector2>, dimensions: List<Vector2>) {
        rectangleDrawer.drawRectangles(context, drawStyle, positions, dimensions)
    }

    fun rectangles(rectangles: List<Rectangle>) {
        rectangleDrawer.drawRectangles(context, drawStyle, rectangles)
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
                        true -> qualityLineDrawer.drawLineLoops(context, drawStyle, listOf(contour.adaptivePositions().let { it.subList(0, it.size - 1) }))
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

    fun image(colorBuffer: ColorBuffer, x: Double, y: Double) {
        imageDrawer.drawImage(context, drawStyle, colorBuffer, x, y, colorBuffer.width * 1.0, colorBuffer.height * 1.0)
    }

    fun image(colorBuffer: ColorBuffer) = image(colorBuffer, 0.0, 0.0)

    fun image(colorBuffer: ColorBuffer, rectangles: List<Pair<Rectangle, Rectangle>>) {
        imageDrawer.drawImage(context, drawStyle, colorBuffer, rectangles)
    }

    fun text(text: String, x: Double, y: Double) {
        if (fontMap is FontImageMap) {
            fontImageMapDrawer.drawText(context, drawStyle, text, x, y)
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

    fun vertexBuffer(indexBuffer:IndexBuffer, vertexBuffers: List<VertexBuffer>, primitive: DrawPrimitive, offset: Int = 0, indexCount: Int = indexBuffer.indexCount) {
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