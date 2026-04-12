package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.internal.Driver
import org.openrndr.math.*
import org.openrndr.math.transforms.ortho
import org.openrndr.shape.Rectangle

private var filterQuad: VertexBuffer? = null
private var filterQuadFormat = vertexFormat {
    position(2)
    textureCoordinate(2)
}

fun filterShaderFromCode(fragmentShaderCode: String, name: String, includeShaderConfiguration: Boolean = true): Shader {
    val hasExistingConfiguration = fragmentShaderCode.contains("#version")

    return if (!includeShaderConfiguration || hasExistingConfiguration) {
        Shader.createFromCode(vsCode = Filter.filterVertexCode, fsCode = fragmentShaderCode, name = name)
    } else {
        Shader.createFromCode(
            vsCode = Filter.filterVertexCode,
            fsCode = "${Driver.instance.shaderConfiguration(ShaderType.FRAGMENT)}\n$fragmentShaderCode",
            name = name
        )
    }
}


/**
 * The `Filter` class represents a rendering filter used to process graphical content by applying shaders
 * and other configurations. This class provides mechanisms to apply the filter to color buffers, manage
 * shader parameters, and handle associated rendering operations such as clip rectangles and blending modes.
 *
 * The `Filter` class supports a flexible set of parameters, enabling it to accommodate different use cases
 * and rendering scenarios. It can operate on various combinations of source and target color buffers or
 * render targets, as well as optionally handle depth buffers.
 *
 * @constructor
 * Creates a new filter instance, optionally initializing it with a shader or shader watcher.
 * @param shader The optional `Shader` instance to be used by the filter.
 */
open class Filter(private val shader: Shader?): AutoCloseable {

    private val textureBindings = TextureBindings()
    private val filterDrawStyle = DrawStyle().apply {
        depthWrite = false
        depthTestPass = DepthTestPass.ALWAYS
        stencil.stencilTest = StencilTest.DISABLED
        clip = null
    }

    /**
     * parameter map
     */
    open val parameters = mutableMapOf<String, Any>()
    var padding = 0

    var depthBufferOut: DepthBuffer? = null

    companion object {
        val filterVertexCode: String
            get() {
                return Driver.instance.shaderConfiguration(ShaderType.VERTEX) + """

#ifdef OR_IN_OUT
in vec2 a_texCoord0;
in vec2 a_position;
#else
attribute vec2 a_texCoord0;
attribute vec2 a_position;
#endif

uniform vec2 targetSize;
uniform vec2 padding;
uniform mat4 projectionMatrix;

#ifdef OR_IN_OUT
out vec2 v_texCoord0;
#else
varying vec2 v_texCoord0;
#endif

void main() {
    v_texCoord0 = a_texCoord0;
    vec2 transformed = a_position * (targetSize - 2.0 * padding) + padding;
    gl_Position = projectionMatrix * vec4(transformed, 0.0, 1.0);
}"""

            }
    }

    /**
     * Applies a filter operation using the given source and target arrays of `ColorBuffer`. Optionally, a clipping rectangle
     * can be used to define the region for the filter operation.
     *
     * @param source An array of `ColorBuffer` representing the input for the filter operation.
     * @param target An array of `ColorBuffer` representing the destination where the filter result will be applied.
     * @param clip An optional `Rectangle` that defines the clipping region for the filter operation.
     */
    open fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle? = null) {
        if (target.isEmpty() || clip?.area == 0.0) {
            return
        }
        filterDrawStyle.clip = clip
        val renderTarget = renderTarget(target[0].width, target[0].height, target[0].contentScale) {}

        target.forEach {
            renderTarget.attach(it, ownedByRenderTarget = false)
        }

        for (i in 0 until target.size) {
            renderTarget.setBlendMode(i, BlendMode.REPLACE)
        }

        apply(source, renderTarget, clip)
        depthBufferOut?.let {
            renderTarget.attach(it, false)
        }

        if (depthBufferOut != null) {
            renderTarget.detachDepthBuffer()
        }
        renderTarget.detachColorAttachments()
        renderTarget.destroy()
    }

    /**
     * Applies a filter operation using the given source and target using a shader program.
     * Optionally, a clipping rectangle can be used to define the region for the filter operation.
     * Custom shader parameters are applied where applicable.
     *
     * @param source An array of `ColorBuffer` representing the input for the filter operation.
     * @param target A `RenderTarget` where the result of the filter operation will be applied.
     * @param clip An optional `Rectangle` specifying the clipping region for the filter application. Defaults to `null`.
     */
    fun apply(source: Array<ColorBuffer>, target: RenderTarget, clip: Rectangle? = null) {
        if (shader == null) { return }

        filterDrawStyle.clip = clip
        target.bind()

        if (filterQuad == null) {
            val fq = VertexBuffer.createDynamic(filterQuadFormat, 6, Session.root)

            fq.shadow.writer().apply {
                write(Vector2(0.0, 1.0)); write(Vector2(0.0, 0.0))
                write(Vector2(0.0, 0.0)); write(Vector2(0.0, 1.0))
                write(Vector2(1.0, 0.0)); write(Vector2(1.0, 1.0))

                write(Vector2(0.0, 1.0)); write(Vector2(0.0, 0.0))
                write(Vector2(1.0, 1.0)); write(Vector2(1.0, 0.0))
                write(Vector2(1.0, 0.0)); write(Vector2(1.0, 1.0))
            }
            fq.shadow.upload()
            fq.shadow.destroy()
            filterQuad = fq
        }

        shader.begin()

        source.forEachIndexed { index, colorBuffer ->
            shader.textureBindings[index] = colorBuffer
            shader.uniform("tex$index", index)
            shader.uniform(
                "textureSize$index",
                Vector2(colorBuffer.effectiveWidth.toDouble(), colorBuffer.effectiveHeight.toDouble())
            )
        }

        Driver.instance.setState(filterDrawStyle)

        shader.uniform(
            "projectionMatrix",
            ortho(0.0, target.width.toDouble(), target.height.toDouble(), 0.0, -1.0, 1.0)
        )
        shader.uniform("targetSize", Vector2(target.width.toDouble(), target.height.toDouble()))
        shader.uniform("padding", Vector2(padding.toDouble(), padding.toDouble()))

        var textureIndex = source.size + 0
        parameters.forEach { (uniform, value) ->
            @Suppress("UNCHECKED_CAST")
            when (value) {
                is Double -> shader.uniform(uniform, value)
                is Boolean -> shader.uniform(uniform, value)
                is Float -> shader.uniform(uniform, value)
                is Matrix44 -> shader.uniform(uniform, value)
                is Vector2 -> shader.uniform(uniform, value)
                is Vector3 -> shader.uniform(uniform, value)
                is Vector4 -> shader.uniform(uniform, value)
                is IntVector2 -> shader.uniform(uniform, value)
                is IntVector3 -> shader.uniform(uniform, value)
                is IntVector4 -> shader.uniform(uniform, value)
                is ColorRGBa -> shader.uniform(uniform, value)
                is Int -> shader.uniform(uniform, value)
                is Matrix55 -> shader.uniform(uniform, value.floatArray)
                is FloatArray -> shader.uniform(uniform, value)

                // EJ: this is not so nice, but I have no other ideas for this
                is Array<*> -> if (value.size > 0) when (value[0]) {
                    is Vector2 -> shader.uniform(uniform, value as Array<Vector2>)
                    is Vector3 -> shader.uniform(uniform, value as Array<Vector3>)
                    is Vector4 -> shader.uniform(uniform, value as Array<Vector4>)
                    is IntVector2 -> shader.uniform(uniform, value as Array<IntVector2>)
                    is IntVector3 -> shader.uniform(uniform, value as Array<IntVector3>)
                    is IntVector4 -> shader.uniform(uniform, value as Array<IntVector4>)
                    is ColorRGBa -> shader.uniform(uniform, value as Array<ColorRGBa>)
                    is Double -> shader.uniform(uniform, value as Array<Double>)
                    is Matrix33 -> shader.uniform(uniform, value as Array<Matrix33>)
                    is Matrix44 -> shader.uniform(uniform, value as Array<Matrix44>)
                    else -> throw IllegalArgumentException("unsupported array value: ${value[0]!!::class}")
                    //is ColorRGBa -> shader.uniform(uniform, value as Array<ColorRGBa>)
                }

                is DepthBuffer -> {
                    shader.uniform(uniform, textureIndex)
                    value.bind(textureIndex)
                    textureIndex++
                }

                is ColorBuffer -> {
                    shader.uniform(uniform, textureIndex)
                    shader.uniform(
                        "textureSize$textureIndex",
                        Vector2(value.effectiveWidth.toDouble(), value.effectiveHeight.toDouble())
                    )
                    shader.textureBindings[textureIndex] = value
                    textureIndex++
                }

                is Cubemap -> {
                    shader.uniform(uniform, textureIndex)
                    value.bind(textureIndex)
                    textureIndex++
                }

                is ArrayCubemap -> {
                    shader.uniform(uniform, textureIndex)
                    textureBindings[textureIndex] = value
//                    value.bind(textureIndex)
                    textureIndex++
                }

                is BufferTexture -> {
                    shader.uniform(uniform, textureIndex)
                    value.bind(textureIndex)
                    textureIndex++
                }

            }
        }

        Driver.instance.drawVertexBuffer(shader, listOf(filterQuad!!), DrawPrimitive.TRIANGLES, 0, 6)
        shader.end()
        target.unbind()
    }

    // TODO move to Filter1to1
    fun apply(source: ColorBuffer, target: ColorBuffer, clip: Rectangle? = null) =
        apply(arrayOf(source), arrayOf(target), clip)

    /**
     * Applies a filter operation using the given source `ColorBuffer` and target array of `ColorBuffer`.
     * Optionally, a clipping rectangle can be used to define the region for the filter operation.
     *
     * @param source A `ColorBuffer` representing the input for the filter operation.
     * @param target An array of `ColorBuffer` representing the destination where the filter result will be applied.
     * @param clip An optional `Rectangle` that defines the clipping region for the filter operation. Defaults to `null`.
     */
    fun apply(source: ColorBuffer, target: Array<ColorBuffer>, clip: Rectangle? = null) = apply(arrayOf(source), target, clip)
    /**
     * Applies a filter operation using the given source array of `ColorBuffer` and the target `ColorBuffer`.
     * Optionally, a clipping rectangle can be used to define the region for the filter operation.
     *
     * @param source An array of `ColorBuffer` representing the input for the filter operation.
     * @param target A `ColorBuffer` representing the destination where the filter result will be applied.
     * @param clip An optional `Rectangle` specifying the clipping region for the filter operation. Defaults to `null`.
     */
    fun apply(source: Array<ColorBuffer>, target: ColorBuffer, clip: Rectangle? = null) = apply(source, arrayOf(target), clip)

    fun untrack() {
        shader?.let { Session.active.untrack(shader) }
    }

    /**
     * Releases resources associated with this filter, including those allocated for the shader.
     * This method should be called when the filter is no longer needed to ensure proper cleanup
     * and avoid resource leaks.
     */
    open fun destroy() {
        shader?.destroy()
    }

    protected val format get() = filterQuadFormat

    override fun close() {
        destroy()
    }
}

/**
 * A filter that processes a single input texture and outputs a single result.
 *
 * This class is a specialized type of [Filter] that operates with a one-to-one
 * relationship between the input and output.
 *
 * @constructor Creates a Filter1to1 with the specified [shader].
 * @param shader The [Shader] instance used for applying the visual effect.
 * Can be null if no specific shader functionality is needed.
 */
open class Filter1to1(shader: Shader?) :
    Filter(shader)

/**
 * A class representing a 2-to-1 filter that applies a shader operation
 * using two input sources and a single target output.
 *
 * @constructor Creates a new instance of the Filter2to1 class.
 * @param shader An optional shader instance, defining the filter's processing logic.
 */
open class Filter2to1(shader: Shader?) :
    Filter(shader) {
    /**
     * Applies a shader-based operation to two input color buffers and writes the result to a target color buffer.
     *
     * @param source0 The first input color buffer that will be used in the operation.
     * @param source1 The second input color buffer that will be used in the operation.
     * @param target The target color buffer where the result of the operation will be written.
     * @param clip An optional rectangle defining the region of interest for the operation. If null, the entire buffer is used.
     */
    fun apply(source0: ColorBuffer, source1: ColorBuffer, target: ColorBuffer, clip: Rectangle? = null) =
        apply(arrayOf(source0, source1), arrayOf(target), clip)
}

/**
 * A filter implementation that processes three input color buffers and outputs to a single target color buffer.
 *
 * This class is a specialization of the `Filter` class designed to handle operations involving three input
 * color buffers and a single output.
 *
 * @constructor Creates an instance of `Filter3to1`
 * @param shader An optional shader used for rendering purposes.
 *
 * @see Filter
 */
open class Filter3to1(shader: Shader?) :
    Filter(shader) {
    /**
     * Applies a processing operation using three input color buffers and a single output color buffer.
     *
     * This function takes three source `ColorBuffer` objects as inputs, applies the specified filter
     * operations on their contents, and writes the result to the target `ColorBuffer`. An optional
     * clipping rectangle (`clip`) can be provided to restrict the processing to a specific region within
     * the buffers.
     *
     * @param source0 The first input `ColorBuffer` to be processed.
     * @param source1 The second input `ColorBuffer` to be processed.
     * @param source2 The third input `ColorBuffer` to be processed.
     * @param target The target `ColorBuffer` where the processed result is written.
     * @param clip An optional `Rectangle` defining the area within the buffers to process. When `null`, the entire buffer is processed.
     */
    fun apply(
        source0: ColorBuffer,
        source1: ColorBuffer,
        source2: ColorBuffer,
        target: ColorBuffer,
        clip: Rectangle? = null
    ) =
        apply(arrayOf(source0, source1, source2), arrayOf(target), clip)
}

/**
 * A specialized filter that combines four input color buffers into one output color buffer.
 *
 * This filter processes four input color buffers simultaneously and outputs the result
 * into a specified target color buffer. It optionally accepts a clipping rectangle to define
 * the area of processing.
 *
 * @constructor Creates an instance of `Filter4to1`.
 * @param shader The shader used for processing, can be null.
 */
open class Filter4to1(shader: Shader?) :
    Filter(shader) {
    /**
     * Applies a filter that processes four source color buffers and outputs the result to a target color buffer.
     * Optionally, a clipping rectangle can be provided to specify the area of processing.
     *
     * @param source0 The first input color buffer to be processed.
     * @param source1 The second input color buffer to be processed.
     * @param source2 The third input color buffer to be processed.
     * @param source3 The fourth input color buffer to be processed.
     * @param target The target color buffer where the processed output will be written.
     * @param clip An optional rectangular area to restrict processing, or null for no restriction.
     */
    fun apply(
        source0: ColorBuffer,
        source1: ColorBuffer,
        source2: ColorBuffer,
        source3: ColorBuffer,
        target: ColorBuffer,
        clip: Rectangle? = null

    ) =
        apply(arrayOf(source0, source1, source2, source3), arrayOf(target), clip)
}

/**
 * A filter class that applies a processing step using a shader and takes a single input color buffer.
 * The processing result is written to two output color buffers.
 *
 * This class extends the functionality of the base [Filter] class by supporting an input of one
 * [ColorBuffer] and producing outputs to two [ColorBuffer] instances. It allows an optional rectangular
 * clipping region to be defined for the output targets.
 *
 * @constructor Creates a Filter1to2 instance.
 * @param shader A [Shader] instance to use for the filter. Defaults to null.
 */
open class Filter1to2(shader: Shader?) :
    Filter(shader) {
    fun apply(
        source: ColorBuffer,
        target0: ColorBuffer,
        target1: ColorBuffer,
        clip: Rectangle? = null

    ) =
        apply(arrayOf(source), arrayOf(target0, target1), clip)
}

open class Filter2to2(shader: Shader?) :
/**
 * A filter extension class that applies a two-source to two-target fragment shader.
 * This class facilitates the rendering of graphical effects involving two input `ColorBuffer` sources
 * and two output `ColorBuffer` targets, using a specified `Shader` for processing.
 *
 * @constructor Creates a Filter2to2 instance with an optional [shader].
 * The [shader] defines the graphics pipeline used for rendering.
 *
 * @param shader An optional `Shader` instance that implements the rendering logic. Pass `null` to
 * rely on default behavior.
 */
Filter(shader) {
    fun apply(
        source0: ColorBuffer,
        source1: ColorBuffer,
        target0: ColorBuffer,
        target1: ColorBuffer,
        clip: Rectangle? = null
    ) =
        apply(arrayOf(source0, source1), arrayOf(target0, target1), clip)
}