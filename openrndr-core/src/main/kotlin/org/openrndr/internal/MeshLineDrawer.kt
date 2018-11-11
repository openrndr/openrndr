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
        attribute("element", VertexElementType.FLOAT32)
    }, 1024 * 1024)

    fun drawLineSegments(drawContext: DrawContext, drawStyle: DrawStyle, segments: List<Vector3>) {

        val vertexCount = vertices.put {
            for (i in 0 until segments.size step 2) {

                var element = (i/2).toFloat()

                write(segments[i])
                write(segments[i])
                write(segments[i + 1])
                write(-1.0f)
                write(1.0f)
                write(Vector2.ZERO)
                write(element)

                write(segments[i])
                write(segments[i])
                write(segments[i + 1])
                write(+1.0f)
                write(1.0f)
                write(Vector2.ZERO)
                write(element)

                write(segments[i])
                write(segments[i + 1])
                write(segments[i + 1])
                write(+1.0f)
                write(1.0f)
                write(Vector2.ZERO)
                write(element)

                // --

                write(segments[i])
                write(segments[i + 1])
                write(segments[i + 1])
                write(+1.0f)
                write(1.0f)
                write(Vector2.ZERO)
                write(element)

                write(segments[i])
                write(segments[i + 1])
                write(segments[i + 1])
                write(-1.0f)
                write(1.0f)
                write(Vector2.ZERO)
                write(element)

                write(segments[i])
                write(segments[i])
                write(segments[i + 1])
                write(-1.0f)
                write(1.0f)
                write(Vector2.ZERO)
                write(element)
            }
        }

        val shader = shaderManager.shader(drawStyle.shadeStyle, vertices.vertexFormat)
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)

        Driver.instance.setState(drawStyle)
        Driver.instance.drawVertexBuffer(shader, listOf(vertices), DrawPrimitive.TRIANGLES, 0, vertexCount)
        shader.end()
    }

    fun drawLineStrips(drawContext: DrawContext,
                       drawStyle: DrawStyle,
                       strips: List<List<Vector3>>) {

        val vertexCount = vertices.put {
            for ((element, strip) in strips.withIndex()) {

                val elementF = element.toFloat()

                var previous = strip[0]
                // leading degenerate
                write(strip[0])
                write(strip[0])
                write(strip[1])
                write(-1.0f)
                write(1.0f)
                write(Vector2.ZERO)
                write(elementF)

                for ((current, next) in strip.zipWithNext()) {
                    write(previous)
                    write(current)
                    write(next)
                    write(-1.0f)
                    write(1.0f)
                    write(Vector2.ZERO)
                    write(elementF)

                    write(previous)
                    write(current)
                    write(next)
                    write(1.0f)
                    write(1.0f)
                    write(Vector2.ZERO)
                    write(elementF)
                    previous = current
                }

                // last point
                write(previous)
                write(strip.last())
                write(strip.last())
                write(-1.0f)
                write(1.0f)
                write(Vector2.ZERO)
                write(elementF)

                write(previous)
                write(strip.last())
                write(strip.last())
                write(1.0f)
                write(1.0f)
                write(Vector2.ZERO)
                write(elementF)

                // -- degenerate
                write(previous)
                write(strip.last())
                write(strip.last())
                write(1.0f)
                write(1.0f)
                write(Vector2.ZERO)
                write(elementF)
            }
        }

        val shader = shaderManager.shader(drawStyle.shadeStyle, vertices.vertexFormat)
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)

        Driver.instance.setState(drawStyle)
        Driver.instance.drawVertexBuffer(shader, listOf(vertices), DrawPrimitive.TRIANGLE_STRIP, 0, vertexCount)
        shader.end()
    }

    fun drawLineLoops(drawContext: DrawContext,
                      drawStyle: DrawStyle,
                      loops: List<List<Vector3>>) {

        val vertexCount = vertices.put {
            for ((element, loop) in loops.withIndex()) {
                val elementF = element.toFloat()

                var previous = loop.last()
                // leading degenerate
                write(loop.last())
                write(loop[0])
                write(loop[1])
                write(-1.0f)
                write(1.0f)
                write(Vector2.ZERO)
                write(elementF)

                for ((current, next) in loop.zipWithNext()) {
                    write(previous)
                    write(current)
                    write(next)
                    write(-1.0f)
                    write(1.0f)
                    write(Vector2.ZERO)
                    write(elementF)

                    write(previous)
                    write(current)
                    write(next)
                    write(1.0f)
                    write(1.0f)
                    write(Vector2.ZERO)
                    write(elementF)
                    previous = current
                }

                // last point
                write(previous)
                write(loop.last())
                write(loop[0])
                write(-1.0f)
                write(1.0f)
                write(Vector2.ZERO)
                write(elementF)

                write(previous)
                write(loop.last())
                write(loop[0])
                write(1.0f)
                write(1.0f)
                write(Vector2.ZERO)
                write(elementF)

                // first point (close)
                write(loop.last())
                write(loop[0])
                write(loop[1])
                write(-1.0f)
                write(1.0f)
                write(Vector2.ZERO)
                write(elementF)

                write(loop.last())
                write(loop[0])
                write(loop[1])
                write(1.0f)
                write(1.0f)
                write(Vector2.ZERO)
                write(elementF)

                // -- degenerate
                write(loop.last())
                write(loop[0])
                write(loop[1])
                write(1.0f)
                write(1.0f)
                write(Vector2.ZERO)
                write(elementF)
            }
        }

        val shader = shaderManager.shader(drawStyle.shadeStyle, vertices.vertexFormat)
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)

        Driver.instance.setState(drawStyle)
        Driver.instance.drawVertexBuffer(shader, listOf(vertices), DrawPrimitive.TRIANGLE_STRIP, 0, vertexCount)
        shader.end()
    }
}