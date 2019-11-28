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

private var filterVertex: VertexBuffer? = null

fun filterShaderFromUrl(url: String): Shader {
    return filterShaderFromCode(URL(url).readText())
}

fun filterWatcherFromUrl(url: String, geometry: FilterGeometry = FilterGeometry.TRIANGLE): ShaderWatcher {
    var vertexCode = Filter.filterTriangleVertexCode

    if (geometry == FilterGeometry.QUAD) {
        vertexCode = Filter.filterQuadVertexCode
    }

    return shaderWatcher {
        vertexShaderCode = vertexCode
        fragmentShaderUrl = url
    }
}

fun filterShaderFromCode(fragmentShaderCode: String, geometry: FilterGeometry = FilterGeometry.TRIANGLE): Shader {
    var vertexCode = Filter.filterTriangleVertexCode

    if (geometry == FilterGeometry.QUAD) {
        vertexCode = Filter.filterQuadVertexCode
    }

    return Shader.createFromCode(vertexCode, fragmentShaderCode)
}

enum class FilterGeometry {
    QUAD, TRIANGLE
}

open class Filter(private val shader: Shader? = null, private val watcher: ShaderWatcher? = null, private val geometry: FilterGeometry = FilterGeometry.TRIANGLE) {

    val parameters = mutableMapOf<String, Any>()
    var padding = 0

    companion object {
        val filterQuadVertexCode: String get() = Driver.instance.internalShaderResource("filter-quad.vert")
        val filterTriangleVertexCode: String get() = Driver.instance.internalShaderResource("filter-triangle.vert")
    }

    fun apply(source: RenderTarget, target: RenderTarget) {
        apply(source.colorBuffers.toTypedArray(), target.colorBuffers.toTypedArray())
    }

    open fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {

        if (target.isEmpty()) {
            return
        }
        val shader = if (this.watcher != null) watcher.shader!! else this.shader!!
        val renderTarget = renderTarget(target[0].width, target[0].height, target[0].contentScale) {}

        target.forEach {
            renderTarget.attach(it)
        }

        renderTarget.bind()

        if (filterVertex == null) {
            if (geometry == FilterGeometry.TRIANGLE) {
                val ft = VertexBuffer.createDynamic(VertexFormat().apply {
                    position(2)
                }, 3)

                ft.shadow.writer().apply {
                    write(Vector2(-1.0, -1.0))
                    write(Vector2(-1.0, 3.0))
                    write(Vector2(3.0, -1.0))
                }

                ft.shadow.upload()
                ft.shadow.destroy()

                filterVertex = ft
            } else if (geometry == FilterGeometry.QUAD) {
                val fq = VertexBuffer.createDynamic(VertexFormat().apply {
                    position(2)
                    textureCoordinate(2)
                }, 6)

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

                filterVertex = fq
            }
        }

        shader.begin()

        source.forEachIndexed { index, colorBuffer ->
            colorBuffer.bind(index)
            shader.uniform("tex$index", index)
        }

        Driver.instance.setState(filterDrawStyle)

        if (geometry == FilterGeometry.QUAD) {
            shader.uniform("projectionMatrix", ortho(0.0, target[0].width.toDouble(), target[0].height.toDouble(), 0.0, -1.0, 1.0))
            shader.uniform("targetSize", Vector2(target[0].width.toDouble(), target[0].height.toDouble()))
            shader.uniform("padding", Vector2(padding.toDouble(), padding.toDouble()))
        }

        var textureIndex = source.size
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
                is ColorRGBa -> shader.uniform(uniform, value)
                is Int -> shader.uniform(uniform, value)
                is Matrix55 -> shader.uniform(uniform, value.floatArray)
                is FloatArray -> shader.uniform(uniform, value)

                // EJ: this is not so nice but I have no other ideas for this
                is Array<*> -> if (value.size > 0) when (value[0]) {
                    is Vector2 -> shader.uniform(uniform, value as Array<Vector2>)
                    is Vector3 -> shader.uniform(uniform, value as Array<Vector3>)
                    is Vector4 -> shader.uniform(uniform, value as Array<Vector4>)
                    else -> throw IllegalArgumentException("unsupported array value: ${value[0]!!::class.java}")
                    //is ColorRGBa -> shader.uniform(uniform, value as Array<ColorRGBa>)
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

                is ArrayTexture -> {
                    shader.uniform("$uniform", textureIndex)
                    value.bind(textureIndex)
                    textureIndex++
                }
            }
        }

        Driver.instance.drawVertexBuffer(shader, listOf(filterVertex!!), DrawPrimitive.TRIANGLES, 0, filterVertex!!.vertexCount)
        shader.end()
        renderTarget.unbind()
        renderTarget.detachColorBuffers()
        renderTarget.destroy()
    }

    fun apply(source: ColorBuffer, target: ColorBuffer) = apply(arrayOf(source), arrayOf(target))
    fun apply(source: ColorBuffer, target: Array<ColorBuffer>) = apply(arrayOf(source), target)
    fun apply(source: Array<ColorBuffer>, target: ColorBuffer) = apply(source, arrayOf(target))

    fun untrack() {
        shader?.let { Session.active.untrack(shader) }
    }
}