package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.internal.Driver
import org.openrndr.math.*
import org.openrndr.math.transforms.ortho

private val filterDrawStyle = DrawStyle().apply {
    blendMode = BlendMode.REPLACE
    depthWrite = false
    depthTestPass = DepthTestPass.ALWAYS
    stencil.stencilTest = StencilTest.DISABLED
}

private var filterQuad: VertexBuffer? = null

open class Filter(val shader: Shader) {

    val parameters = mutableMapOf<String, Any>()

    companion object {
        val filterVertexCode:String get() = Driver.instance.internalShaderResource("filter.vert")
    }


    fun apply(source:RenderTarget, target:RenderTarget) {
        apply(source.colorBuffers.toTypedArray(), target.colorBuffers.toTypedArray())
    }

    open fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {

        val renderTarget = RenderTarget.create(target[0].width, target[0].height, 1.0)

        target.forEach {
            renderTarget.attach(it)
        }

        renderTarget.bind()

        if (filterQuad == null) {
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
            filterQuad = fq
        }

        shader.begin()

        source.forEachIndexed { index, colorBuffer ->
            colorBuffer.bind(index)
            shader.uniform("tex$index", index)
        }

        Driver.instance.setState(filterDrawStyle)

        shader.uniform("projectionMatrix", ortho(0.0, target[0].width.toDouble(), target[0].height.toDouble(), 0.0, -1.0, 1.0))
        shader.uniform("targetSize", Vector2(target[0].width.toDouble(), target[0].height.toDouble()))

        parameters.forEach { (uniform, value) ->
            @Suppress("UNCHECKED_CAST")
            when (value) {
                is Float -> shader.uniform(uniform, value)
                is Double -> shader.uniform(uniform, value.toFloat())
                is Matrix44 -> shader.uniform(uniform, value)
                is Vector2 -> shader.uniform(uniform, value)
                is Vector3 -> shader.uniform(uniform, value)
                is Vector4 -> shader.uniform(uniform, value)
                is ColorRGBa -> shader.uniform(uniform, value)
                is Int -> shader.uniform(uniform, value)
                is Matrix55 -> shader.uniform(uniform, value.floatArray)

                // EJ: this is not so nice but I have no other ideas for this
                is Array<*> -> if (value.size > 0) when(value[0]) {
                    is Vector2 -> shader.uniform(uniform, value as Array<Vector2>)
                    is Vector3 -> shader.uniform(uniform, value as Array<Vector3>)
                    is Vector4 -> shader.uniform(uniform, value as Array<Vector4>)
                }

            }
        }
        Driver.instance.drawVertexBuffer(shader, listOf(filterQuad!!), DrawPrimitive.TRIANGLES, 0, 6)
        shader.end()
        renderTarget.unbind()
        renderTarget.detachColorBuffers()
        renderTarget.destroy()
    }

    fun apply(source: ColorBuffer, target: ColorBuffer) = apply(arrayOf(source), arrayOf(target))
    fun apply(source: ColorBuffer, target: Array<ColorBuffer>) = apply(arrayOf(source), target)
    fun apply(source: Array<ColorBuffer>, target: ColorBuffer) = apply(source, arrayOf(target))
}