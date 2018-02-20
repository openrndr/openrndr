package org.openrndr.draw

import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.internal.*
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.math.transforms.normalMatrix
import org.openrndr.math.transforms.lookAt as _lookAt
import org.openrndr.math.transforms.perspectiveDegrees
import org.openrndr.math.transforms.rotateZ
import org.openrndr.shape.*
import org.openrndr.math.transforms.ortho as _ortho
import org.openrndr.math.transforms.translate as _translate
import org.openrndr.math.transforms.rotate as _rotate
import org.openrndr.math.transforms.scale as _scale

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL

import java.nio.ByteBuffer
import java.util.*


data class VertexElement(val attribute: String, val size: Int, val offset: Int, val type: VertexElementType, val count: Int)

@Suppress("MemberVisibilityCanPrivate")
/**
 * VertexBuffer Layout describes how data is organized in the VertexBuffer
 */
class VertexFormat {

    var items: MutableList<VertexElement> = mutableListOf()
    internal var vertexSize = 0

    val size get() = vertexSize

    /**
     * Appends a position component to the layout
     * @param dimensions
     */
    fun position(dimensions: Int): VertexFormat = attribute("position", dimensions, VertexElementType.FLOAT32)

    /**
     * Appends a normal component to the layout
     * @param dimensions the number of dimensions of the normal vector
     */
    fun normal(dimensions: Int): VertexFormat = attribute("normal", dimensions, VertexElementType.FLOAT32)

    /**
     * Appends a color attribute to the layout
     * @param dimensions
     */
    fun color(dimensions: Int): VertexFormat = attribute("color", dimensions, VertexElementType.FLOAT32)

    fun textureCoordinate(index: Int, dimensions: Int): VertexFormat = attribute("texCoord" + index, dimensions, VertexElementType.FLOAT32)

    /**
     * Adds a primary texture coordinate attribute to the layout
     * @param dimensions the dimension the texture coordinates
     */
    fun textureCoordinate(dimensions: Int): VertexFormat = textureCoordinate(0, dimensions)


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
    fun attribute(name: String, dimensions: Int, type: VertexElementType): VertexFormat {
        val offset = items.sumBy { it.size }

        val item = VertexElement(name, dimensions * size(type), offset, type, dimensions)
        items.add(item)
        vertexSize += item.size
        return this
    }


    override fun toString(): String {
        return "VertexFormat{" +
                "items=" + items +
                ", vertexSize=" + vertexSize +
                '}'
    }

    companion object {
        internal fun size(type: VertexElementType): Int {
            when (type) {
                VertexElementType.FLOAT32 -> return 4
                else -> throw RuntimeException("unsupported element type: " + type)
            }
        }
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

interface Shader {

    @Suppress("unused")
    companion object {
        fun createFromUrls(vsUrl: String, fsUrl: String): Shader {
            val vsCode = codeFromURL(vsUrl)
            val fsCode = codeFromURL(fsUrl)
            return Driver.instance.createShader(vsCode, fsCode)
        }

        fun createFromCode(vsCode: String, fsCode: String): Shader {
            return Driver.instance.createShader(vsCode, fsCode)
        }
    }

    fun begin()
    fun end()

    fun uniform(name: String, value: Matrix44)
    fun uniform(name: String, value: ColorRGBa)
    fun uniform(name: String, value: Vector4)
    fun uniform(name: String, value: Vector3)
    fun uniform(name: String, value: Vector2)
    fun uniform(name: String, value: Double)
    fun uniform(name: String, value: Float)
    fun uniform(name: String, value: Int)

    fun uniform(name: String, value: Array<Vector4>)
    fun uniform(name: String, value: Array<Vector3>)
    fun uniform(name: String, value: Array<Vector2>)
    fun uniform(name: String, value: FloatArray)

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

    fun bind()
    fun unbind()

    fun put(putter:BufferWriter.()->Unit):Int {
        val w = shadow.writer()
        w.rewind()
        w.putter()
        if (w.position % vertexFormat.vertexSize != 0) {
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
}


interface ColorBuffer {
    val width: Int
    val height: Int
    val format: ColorFormat
    val type: ColorType

    val bounds:Rectangle get() = Rectangle(Vector2.ZERO, width*1.0, height*1.0)

    fun destroy()
    fun bind(unit: Int)

    fun generateMipmaps()

    var wrapU: WrapMode
    var wrapV: WrapMode

    var filterMin: MinifyingFilter
    var filterMag: MagnifyingFilter

    val shadow: ColorBufferShadow
    var flipV: Boolean


    fun filter(filterMin:MinifyingFilter, filterMag:MagnifyingFilter) {
        this.filterMin = filterMin
        this.filterMag = filterMag
    }

    companion object {
        fun create(width: Int, height: Int, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8): ColorBuffer {
            return Driver.instance.createColorBuffer(width, height, format, type)
        }

        fun fromUrl(url: String): ColorBuffer {
            return Driver.instance.createColorBufferFromUrl(url)
        }
    }
}

fun colorBuffer(width: Int, height: Int, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8):ColorBuffer {
    return ColorBuffer.create(width, height, format, type)
}

interface BufferTextureShadow {
    val bufferTexture: BufferTexture

    fun upload(offset:Int, size:Int)
    fun download()
    fun destroy()

    fun writer(): BufferWriter
}

interface BufferTexture {
    val shadow: BufferTextureShadow

    val format:ColorFormat
    val type:ColorType

    val elementCount: Int

    fun destroy()
    fun bind(unit: Int)

    companion object {
        fun create(elementCount: Int, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.FLOAT32): BufferTexture {
            return Driver.instance.createBufferTexture(elementCount, format, type)
        }
    }

    fun put(putter:BufferWriter.()->Unit):Int {
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
        val cb = ColorBuffer.create(renderTarget.width, renderTarget.height, format, type)
        renderTarget.attach(name, cb)
    }

    fun colorBuffer(format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8) {
        val cb = ColorBuffer.create(renderTarget.width, renderTarget.height, format, type)
        renderTarget.attach(cb)
    }

    fun depthBuffer(format: DepthFormat = DepthFormat.DEPTH24_STENCIL8) {
        renderTarget.attach(DepthBuffer.create(renderTarget.width, renderTarget.height, format))
    }

    fun depthBuffer(depthBuffer: DepthBuffer) {
        renderTarget.attach(depthBuffer)
    }
}

interface RenderTarget {
    val width: Int
    val height: Int

    val colorBuffers: List<ColorBuffer>

    companion object {
        fun create(width: Int, height: Int): RenderTarget = Driver.instance.createRenderTarget(width, height)
    }

    fun attach(name: String, colorBuffer: ColorBuffer)

    fun attach(colorBuffer: ColorBuffer)
    fun attach(depthBuffer: DepthBuffer)
    fun detachColorBuffers()
    fun destroy()

    fun colorBuffer(index: Int): ColorBuffer
    fun colorBuffer(name: String): ColorBuffer
    fun colorBufferIndex(name: String): Int

    fun bind()
    fun unbind()
}

fun renderTarget(width: Int, height: Int, builder: RenderTargetBuilder.() -> Unit): RenderTarget {
    val renderTarget = RenderTarget.create(width, height)
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


@Suppress("MemberVisibilityCanPrivate")
data class DrawContext(val view: Matrix44, val projection: Matrix44, val width: Int, val height: Int) {
    fun applyToShader(shader: Shader) {
        shader.uniform("u_viewMatrix", view)
        shader.uniform("u_projectionMatrix", projection)
        shader.uniform("u_viewProjectionMatrix", projection * view)
        shader.uniform("u_viewDimensions", Vector2(width.toDouble(), height.toDouble()))
        shader.uniform("u_normalMatrix", normalMatrix(view))
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

    val viewStack = Stack<Matrix44>()
    val projectionStack = Stack<Matrix44>()

    var width: Int = 0
    var height: Int = 0

    var view: Matrix44 = Matrix44.IDENTITY
    var projection: Matrix44 = Matrix44.IDENTITY

    val context: DrawContext
        get() = DrawContext(view, projection, width, height)

    var drawStyle = DrawStyle()


    fun withTarget(target: RenderTarget, action: Drawer.() -> Unit) {
        target.bind()
        this.action()
        target.unbind()
    }

    fun reset() {
        viewStack.clear()
        projectionStack.clear()
        drawStyles.clear()
        ortho()
        drawStyle = DrawStyle()
        view = Matrix44.IDENTITY
    }

    fun ortho(renderTarget: RenderTarget) {
        ortho(0.0, renderTarget.width.toDouble(), renderTarget.height.toDouble(), 0.0, -1.0, 1.0)
    }

    fun ortho(left: Double, right: Double, bottom: Double, top: Double, near: Double, far: Double) {
        projection = _ortho(left, right, bottom, top, near, far)
    }

    fun ortho() {
        ortho(0.0, width.toDouble(), height.toDouble(), 0.0, -1.0, 1.0)
    }

    fun perspective(fovInDegrees: Double, ratio: Double, near: Double, far: Double) {
        projection = perspectiveDegrees(fovInDegrees, ratio, near, far, 0.0, 0.0)
    }


    fun lookAt(from: Vector3, to: Vector3, up: Vector3 = Vector3.UNIT_Y) {
        view *= _lookAt(from, to, up)
    }


    fun scale(s: Double) {
        view *= _scale(s, s, s)
    }

    fun scale(x: Double, y: Double) {
        view *= _scale(x, y, 1.0)
    }

    fun scale(x: Double, y: Double, z: Double) {
        view *= _scale(x, y, z)
    }

    fun translate(t: Vector2) {
        view *= _translate(t.vector3())
    }

    fun translate(t: Vector3) {
        view *= _translate(t)
    }

    //
    fun translate(x: Double, y: Double) {
        translate(x, y, 0.0)
    }

    fun translate(x: Double, y: Double, z: Double) {
        view *= _translate(Vector3(x, y, z))
    }

    fun rotate(rotationInDegrees: Double) {
        view *= rotateZ(rotationInDegrees)
    }

    fun rotate(axis: Vector3, rotationInDegrees: Double) {
        view *= _rotate(axis, rotationInDegrees)
    }


    fun background(color: ColorRGBa) {
        driver.clear(color)
    }

    fun pushStyle():DrawStyle = drawStyles.push(drawStyle.copy())
    fun popStyle() {
        drawStyle = drawStyles.pop().copy()
    }

    fun pushView(): Matrix44 = viewStack.push(view)
    fun popView() {
        view = viewStack.pop()
    }


    fun pushProjection():Matrix44 = projectionStack.push(projection)
    fun popProjection() {
        projection = projectionStack.pop()
    }

    fun pushTransforms() {
        pushView()
        pushProjection()
    }

    fun popTransforms() {
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


    fun rectangle(x: Double, y: Double, width: Double, height: Double) {
        rectangleDrawer.drawRectangle(context, drawStyle, x, y, width, height)
    }

    fun circle(position: Vector2, radius: Double) {
        circleDrawer.drawCircle(context, drawStyle, position.x, position.y, radius)
    }

    fun circles(positions: List<Vector2>, radius: Double) {
        circleDrawer.drawCircles(context, drawStyle, positions, radius)
    }

    fun circles(positions: List<Vector2>, radii: List<Double>) {
        circleDrawer.drawCircles(context, drawStyle, positions, radii)
    }


    fun shape(shape: Shape) {
        if (drawStyle.fill != null) {
            qualityPolygonDrawer.drawPolygon(context, drawStyle,
                    shape.contours.map { it.adaptivePositions() })
        }
    }

    fun contour(contour: ShapeContour) {

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
    }

    fun contours(contours: List<ShapeContour>) {
        if (drawStyle.fill != null) {
            qualityPolygonDrawer.drawPolygons(context, drawStyle, contours.map { listOf(it.adaptivePositions()) })
        }

        if (drawStyle.stroke != null) {
            qualityLineDrawer.drawLineStrips(context, drawStyle, contours.map { it.adaptivePositions() })
        }
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


    fun composition(composition: Composition) {

        fun node(compositionNode: CompositionNode) {
            pushView()
            view *= compositionNode.transform

            when (compositionNode) {
                is ShapeNode -> {
                    pushStyle()
                    compositionNode.fill?.let { fill = it; }
                    stroke = compositionNode.stroke
                    shape(compositionNode.shape)
                    popStyle()
                }
                is TextNode -> TODO()
                is GroupNode -> compositionNode.children.forEach { node(it) }
            }

            popView()
        }

        node(composition.root)
    }


    fun image(colorBuffer: ColorBuffer, source:Rectangle, target:Rectangle) {
        imageDrawer.drawImage(context, drawStyle, colorBuffer, source, target)
    }

    fun image(colorBuffer: ColorBuffer, x: Double, y: Double) {
        imageDrawer.drawImage(context, drawStyle, colorBuffer, x, y, colorBuffer.width * 1.0, colorBuffer.height * 1.0)
    }

    fun image(colorBuffer: ColorBuffer) = image(colorBuffer, 0.0, 0.0)

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

    fun vertexBufferInstances(vertexBuffers: List<VertexBuffer>, instanceAttributes: List<VertexBuffer>, primitive: DrawPrimitive, instanceCount: Int, offset: Int = 0, vertexCount: Int = vertexBuffers[0].vertexCount) {
        vertexBufferDrawer.drawVertexBufferInstances(context, drawStyle, primitive, vertexBuffers, instanceAttributes, offset, vertexCount, instanceCount)
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