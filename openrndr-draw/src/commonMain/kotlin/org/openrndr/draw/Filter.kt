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
 * @param watcher The optional `ShaderWatcher` to dynamically manage the shader during execution.
 */
open class Filter(private val shader: Shader? = null, private val watcher: ShaderWatcher? = null): AutoCloseable {

    private val filterDrawStyle = DrawStyle().apply {
        blendMode = BlendMode.REPLACE
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

    open fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle? = null) {
        if (target.isEmpty() || clip?.area == 0.0) {
            return
        }
        filterDrawStyle.clip = clip
        val renderTarget = renderTarget(target[0].width, target[0].height, target[0].contentScale) {}

        target.forEach {
            renderTarget.attach(it, ownedByRenderTarget = false)
        }

        for (i in 1 until target.size) {
            renderTarget.blendMode(i, BlendMode.REPLACE)
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

    fun apply(source: Array<ColorBuffer>, target: RenderTarget, clip: Rectangle? = null) {
        filterDrawStyle.clip = clip
        val shader = if (this.watcher != null) watcher.shader!! else this.shader!!
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
            colorBuffer.bind(index)
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
                    value.bind(textureIndex)
                    textureIndex++
                }

                is Cubemap -> {
                    shader.uniform(uniform, textureIndex)
                    value.bind(textureIndex)
                    textureIndex++
                }

                is ArrayCubemap -> {
                    shader.uniform(uniform, textureIndex)
                    value.bind(textureIndex)
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

    fun apply(source: ColorBuffer, target: Array<ColorBuffer>, clip: Rectangle? = null) = apply(arrayOf(source), target, clip)
    fun apply(source: Array<ColorBuffer>, target: ColorBuffer, clip: Rectangle? = null) = apply(source, arrayOf(target), clip)

    fun untrack() {
        shader?.let { Session.active.untrack(shader) }
    }

    open fun destroy() {
        shader?.destroy()
    }

    protected val format get() = filterQuadFormat

    override fun close() {
        destroy()
    }
}

open class Filter1to1(shader: Shader? = null, watcher: ShaderWatcher? = null) :
    Filter(shader, watcher)

open class Filter2to1(shader: Shader? = null, watcher: ShaderWatcher? = null) :
    Filter(shader, watcher) {
    fun apply(source0: ColorBuffer, source1: ColorBuffer, target: ColorBuffer, clip: Rectangle? = null) =
        apply(arrayOf(source0, source1), arrayOf(target), clip)
}

open class Filter3to1(shader: Shader? = null, watcher: ShaderWatcher? = null) :
    Filter(shader, watcher) {
    fun apply(
        source0: ColorBuffer,
        source1: ColorBuffer,
        source2: ColorBuffer,
        target: ColorBuffer,
        clip: Rectangle? = null
    ) =
        apply(arrayOf(source0, source1, source2), arrayOf(target), clip)
}

open class Filter4to1(shader: Shader? = null, watcher: ShaderWatcher? = null) :
    Filter(shader, watcher) {
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
