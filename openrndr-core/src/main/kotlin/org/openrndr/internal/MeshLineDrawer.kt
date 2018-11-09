package org.openrndr.internal

import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

class MeshLineDrawer {
    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators(Driver.instance.shaderGenerators::meshLineVertexShader,
            Driver.instance.shaderGenerators::meshLineFragmentShader)

    private val vertices: VertexBuffer = vertexBuffer(VertexFormat().apply {
        attribute("previous", VertexElementType.VECTOR3_FLOAT32)
        position(3)
        attribute("next", VertexElementType.VECTOR3_FLOAT32)
        attribute("side", VertexElementType.FLOAT32)
        attribute("width", VertexElementType.FLOAT32)
        attribute("uv", VertexElementType.VECTOR2_FLOAT32)
        attribute("counters", VertexElementType.FLOAT32)
    }, 1024 * 1024)

    fun drawLineStrips(drawContext: DrawContext,
                       drawStyle: DrawStyle,
                       strips: List<List<Vector3>>) {
        val shader = shaderManager.shader(drawStyle.shadeStyle, vertices.vertexFormat)
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)

        val vertexCount = vertices.put {
            for (strip in strips) {
                var previous = strip[0]

                for ((current, next) in strip.zipWithNext()) {
                    write(previous)
                    write(current)
                    write(next)
                    write(-1.0f)
                    write(1.0f)
                    write(Vector2.ZERO)
                    write(0.0f)

                    write(previous)
                    write(current)
                    write(next)
                    write(1.0f)
                    write(1.0f)
                    write(Vector2.ZERO)
                    write(0.0f)
                    previous = current
                }
            }
        }
        Driver.instance.setState(drawStyle)
        Driver.instance.drawVertexBuffer(shader, listOf(vertices), DrawPrimitive.TRIANGLE_STRIP, 0, vertexCount)
        shader.end()
    }
}