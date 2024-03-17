@file:Suppress("Duplicates")

package org.openrndr.internal

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

class MeshLineDrawer {
    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators(
        "mesh-line",
        vsGenerator = Driver.instance.shaderGenerators::meshLineVertexShader,
        fsGenerator = Driver.instance.shaderGenerators::meshLineFragmentShader
    )

    private val vertexFormat = VertexFormat().apply {
        attribute("previous", VertexElementType.VECTOR3_FLOAT32)
        position(3)
        attribute("next", VertexElementType.VECTOR3_FLOAT32)
        attribute("side", VertexElementType.FLOAT32)
        attribute("width", VertexElementType.FLOAT32)
        attribute("uv", VertexElementType.VECTOR2_FLOAT32)
        attribute("element", VertexElementType.FLOAT32)
        color(4)
    }

    private val manyVertices = vertexBuffer(vertexFormat, 1024 * 1024, Session.root)
    private val fewVerticesCount = 1024
    private val fewVertices = List(DrawerConfiguration.vertexBufferMultiBufferCount) {
        vertexBuffer(
            vertexFormat,
            fewVerticesCount,
            Session.root
        )
    }
    private var counter = 0

    private fun vertices(count: Int): VertexBuffer {
        return if (count < fewVerticesCount) {
            counter++
            fewVertices[counter.mod(fewVertices.size)]
        } else {
            manyVertices
        }
    }

    fun drawLineSegments(
        drawContext: DrawContext, drawStyle: DrawStyle, segments: List<Vector3>,
        weights: List<Double> = emptyList(),
        colors: List<ColorRGBa> = emptyList()
    ) {
        val vertices = vertices(segments.size * 6)

        val colorCount = colors.size
        val defaultColor = colors.lastOrNull() ?: drawStyle.stroke ?: ColorRGBa.TRANSPARENT

        val vertexCount = vertices.put {
            for (i in 0 until segments.size step 2) {
                val width = weights.getOrElse(i) { drawStyle.strokeWeight }.toFloat()
                val element = (i / 2).toFloat()

                val color = if (i < colorCount) colors[i] else defaultColor

                write(segments[i])
                write(segments[i])
                write(segments[i + 1])
                write(-1.0f)
                write(width)
                write(Vector2.ZERO)
                write(element)
                write(color)

                write(segments[i])
                write(segments[i])
                write(segments[i + 1])
                write(+1.0f)
                write(width)
                write(Vector2.ZERO)
                write(element)
                write(color)

                write(segments[i])
                write(segments[i + 1])
                write(segments[i + 1])
                write(+1.0f)
                write(width)
                write(Vector2.ZERO)
                write(element)
                write(color)

                // --
                write(segments[i])
                write(segments[i + 1])
                write(segments[i + 1])
                write(+1.0f)
                write(width)
                write(Vector2.ZERO)
                write(element)
                write(color)

                write(segments[i])
                write(segments[i + 1])
                write(segments[i + 1])
                write(-1.0f)
                write(width)
                write(Vector2.ZERO)
                write(element)
                write(color)

                write(segments[i])
                write(segments[i])
                write(segments[i + 1])
                write(-1.0f)
                write(width)
                write(Vector2.ZERO)
                write(element)
                write(color)
            }
        }

        val shader = shaderManager.shader(drawStyle.shadeStyle, vertices.vertexFormat)
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)

        Driver.instance.setState(drawStyle)
        Driver.instance.drawVertexBuffer(
            shader,
            listOf(vertices),
            DrawPrimitive.TRIANGLES,
            0,
            vertexCount,
            verticesPerPatch = 0
        )
        shader.end()
    }

    fun drawLineStrips(
        drawContext: DrawContext,
        drawStyle: DrawStyle,
        strips: List<List<Vector3>>,
        weights: List<Double> = emptyList(),
        colors: List<ColorRGBa> = emptyList(),
        closed: List<Boolean> = emptyList()
    ) {
        val vertices = vertices(strips.sumOf { it.size * 4 + 4 })

        val colorCount = colors.size
        val defaultColor = colors.lastOrNull() ?: drawStyle.stroke ?: ColorRGBa.TRANSPARENT
        val vertexCount = vertices.put {
            for ((element, strip) in strips.withIndex()) {
                val stripClosed = closed.getOrNull(element) ?: false
                val color = if (element < colorCount) colors[element] else defaultColor

                if (strip.size >= 2) {
                    val width = weights.getOrElse(element) { drawStyle.strokeWeight }.toFloat()
                    val elementF = element.toFloat()

                    var previous = if (stripClosed) strip.last() else strip[0]
                    // leading degenerate
                    write(strip[0])
                    write(strip[0])
                    write(strip[1])
                    write(-1.0f)
                    write(width)
                    write(Vector2.ZERO)
                    write(elementF)
                    write(color)

                    for ((current, next) in strip.zipWithNext()) {
                        write(previous)
                        write(current)
                        write(next)
                        write(-1.0f)
                        write(width)
                        write(Vector2.ZERO)
                        write(elementF)
                        write(color)

                        write(previous)
                        write(current)
                        write(next)
                        write(1.0f)
                        write(width)
                        write(Vector2.ZERO)
                        write(elementF)
                        write(color)
                        previous = current
                    }

                    // last point
                    write(previous)
                    write(strip.last())
                    write(strip.last())
                    write(-1.0f)
                    write(width)
                    write(Vector2.ZERO)
                    write(elementF)
                    write(color)

                    write(previous)
                    write(strip.last())
                    write(strip.last())
                    write(1.0f)
                    write(width)
                    write(Vector2.ZERO)
                    write(elementF)
                    write(color)

                    // -- degenerate
                    write(previous)
                    write(strip.last())
                    write(strip.last())
                    write(1.0f)
                    write(width)
                    write(Vector2.ZERO)
                    write(elementF)
                    write(color)
                }
            }
        }

        val shader = shaderManager.shader(drawStyle.shadeStyle, vertices.vertexFormat)
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)

        Driver.instance.setState(drawStyle)
        Driver.instance.drawVertexBuffer(
            shader,
            listOf(vertices),
            DrawPrimitive.TRIANGLE_STRIP,
            0,
            vertexCount,
            verticesPerPatch = 0
        )
        shader.end()
    }
}