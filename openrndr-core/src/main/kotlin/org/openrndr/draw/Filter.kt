package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.internal.Driver
import org.openrndr.math.*
import org.openrndr.math.transforms.ortho
import java.net.URL

private val filterDrawStyle = DrawStyle().apply {
    blendMode = BlendMode.REPLACE
    depthWrite = false
    depthTestPass = DepthTestPass.ALWAYS
    stencil.stencilTest = StencilTest.DISABLED
}

private var filterQuad: VertexBuffer? = null
private var filterQuadFormat = vertexFormat {
    position(2)
    textureCoordinate(2)
}

fun filterShaderFromUrl(url: String): Shader {
    return filterShaderFromCode(URL(url).readText(), "filter-shader: $url")
}

fun filterWatcherFromUrl(url: String): ShaderWatcher {
    return shaderWatcher {
        vertexShaderCode = Filter.filterVertexCode
        fragmentShaderUrl = url
    }
}

fun filterShaderFromCode(fragmentShaderCode: String, name: String): Shader {
    return Shader.createFromCode(vsCode = Filter.filterVertexCode, fsCode = fragmentShaderCode, name = name)
}

/**
 * Filter base class. Renders "full-screen" quads.
 */
open class Filter(private val shader: Shader? = null, private val watcher: ShaderWatcher? = null) {

    /**
     * parameter map
     */
    val parameters = mutableMapOf<String, Any>()
    var padding = 0

    var depthBufferOut: DepthBuffer? = null

    companion object {
        val filterVertexCode: String get() = Driver.instance.internalShaderResource("filter.vert")
    }


//    fun apply(source: RenderTarget, target: RenderTarget) {
//        apply(source.colorBuffers.toTypedArray(), target)
//    }

    open fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        if (target.isEmpty()) {
            return
        }
        val renderTarget = renderTarget(target[0].width, target[0].height, target[0].contentScale) {}

        target.forEach {
            renderTarget.attach(it)
        }

        for (i in 1 until target.size) {
            renderTarget.blendMode(i, BlendMode.REPLACE)
        }

        apply(source, renderTarget)
        depthBufferOut?.let {
            renderTarget.attach(it)
        }

        if (depthBufferOut != null) {
            renderTarget.detachDepthBuffer()
        }
        renderTarget.detachColorAttachments()
        renderTarget.destroy()
    }

    fun apply(source: Array<ColorBuffer>, target: RenderTarget) {
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
        }

        Driver.instance.setState(filterDrawStyle)

        shader.uniform("projectionMatrix", ortho(0.0, target.width.toDouble(), target.height.toDouble(), 0.0, -1.0, 1.0))
        shader.uniform("targetSize", Vector2(target.width.toDouble(), target.height.toDouble()))
        shader.uniform("padding", Vector2(padding.toDouble(), padding.toDouble()))

        var textureIndex = source.size + 0
        parameters.forEach { (uniform, value) ->
            @Suppress("UNCHECKED_CAST")
            when (value) {
                is Boolean -> shader.uniform(uniform, value)
                is Float -> shader.uniform(uniform, value)
                is Double -> shader.uniform(uniform, value.toFloat())
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

                // EJ: this is not so nice but I have no other ideas for this
                is Array<*> -> if (value.size > 0) when (value[0]) {
                    is Vector2 -> shader.uniform(uniform, value as Array<Vector2>)
                    is Vector3 -> shader.uniform(uniform, value as Array<Vector3>)
                    is Vector4 -> shader.uniform(uniform, value as Array<Vector4>)
                    is IntVector2 -> shader.uniform(uniform, value as Array<IntVector2>)
                    is IntVector3 -> shader.uniform(uniform, value as Array<IntVector3>)
                    is IntVector4 -> shader.uniform(uniform, value as Array<IntVector4>)
                    is ColorRGBa -> shader.uniform(uniform, value as Array<ColorRGBa>)
                    else -> throw IllegalArgumentException("unsupported array value: ${value[0]!!::class.java}")
                    //is ColorRGBa -> shader.uniform(uniform, value as Array<ColorRGBa>)
                }

                is DepthBuffer -> {
                    shader.uniform("$uniform", textureIndex)
                    value.bind(textureIndex)
                    textureIndex++
                }

                is ColorBuffer -> {
                    shader.uniform("$uniform", textureIndex)
                    value.bind(textureIndex)
                    textureIndex++
                }

                is Cubemap -> {
                    shader.uniform("$uniform", textureIndex)
                    value.bind(textureIndex)
                    textureIndex++
                }

                is ArrayCubemap -> {
                    shader.uniform("$uniform", textureIndex)
                    value.bind(textureIndex)
                    textureIndex++
                }

                is BufferTexture -> {
                    shader.uniform("$uniform", textureIndex)
                    value.bind(textureIndex)
                    textureIndex++
                }
            }
        }

        Driver.instance.drawVertexBuffer(shader, listOf(filterQuad!!), DrawPrimitive.TRIANGLES, 0, 6)
        shader.end()
        target.unbind()
    }

    fun apply(source: ColorBuffer, target: ColorBuffer) = apply(arrayOf(source), arrayOf(target))
    fun apply(source: ColorBuffer, target: Array<ColorBuffer>) = apply(arrayOf(source), target)
    fun apply(source: Array<ColorBuffer>, target: ColorBuffer) = apply(source, arrayOf(target))

    fun untrack() {
        shader?.let { Session.active.untrack(shader) }
    }

    protected val format get() = filterQuadFormat
}