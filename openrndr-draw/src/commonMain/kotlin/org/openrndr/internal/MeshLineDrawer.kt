@file:Suppress("Duplicates")

package org.openrndr.internal

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

/**
 * A utility class for rendering mesh-based lines in graphical contexts.
 * This class provides methods for drawing line segments and strips with
 * customizable width, color, and other visual properties using shaders.
 *
 * The implementation leverages a shader-driven approach for efficient rendering.
 * It maintains internal management of vertex buffers and shaders to handle
 * the drawing of complex line geometries.
 *
 */
class MeshLineDrawer {
    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators(
        "mesh-line",
        vsGenerator = Driver.instance.shaderGenerators::meshLineVertexShader,
        fsGenerator = Driver.instance.shaderGenerators::meshLineFragmentShader
    )

    private val vertexFormat = vertexFormat {
        attribute("previous", VertexElementType.VECTOR3_FLOAT32)
        position(3)
        attribute("next", VertexElementType.VECTOR3_FLOAT32)
        attribute("side", VertexElementType.FLOAT32)
        attribute("width", VertexElementType.FLOAT32)
        textureCoordinate(2)
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

                val stripLength = strip.asSequence().windowed(2, 1).sumOf { it[0].distanceTo(it[1]) }
                var offset = 0.0

                val stripClosed = closed.getOrNull(element) ?: false
                val color = if (element < colorCount) colors[element] else defaultColor

                if (strip.size >= 2) {
                    val width = weights.getOrElse(element) { drawStyle.strokeWeight }.toFloat()
                    val elementF = element.toFloat()
                    var previous = if (stripClosed) strip.last() else strip[0]
                    val edgeCount = strip.size + if (stripClosed) 1 else 0

                    for (i in 0 until edgeCount) {
                        val current = strip[i.mod(strip.size)]
                        val next =
                            if (stripClosed) strip[(i + 1).mod(strip.size)] else strip[(i + 1).coerceIn(strip.indices)]

                        val segmentLength = current.distanceTo(next)

                        if (i == 0) {
                            write(previous)
                            write(Float.NaN)
                            write(Float.NaN)
                            write(Float.NaN)
                            write(next)
                            write(-1.0f)
                            write(width)
                            write(Vector2(0.0, offset / stripLength))
                            write(elementF)
                            write(color)
                        }
                        for (r in 0 until if (i == 0) 1 else 1) {
                            write(previous)
                            write(current)
                            write(next)
                            write(-1.0f)
                            write(width)
                            write(Vector2(0.0, offset / stripLength))
                            write(elementF)
                            write(color)
                        }

                        for (r in 0 until if (i == edgeCount - 1) 1 else 1) {
                            write(previous)
                            write(current)
                            write(next)
                            write(1.0f)
                            write(width)
                            write(Vector2(1.0, offset / stripLength))
                            write(elementF)
                            write(color)
                            previous = current
                        }

                        if (i == edgeCount - 1) {
                            write(previous)
                            write(Float.NaN)
                            write(Float.NaN)
                            write(Float.NaN)
                            write(next)
                            write(1.0f)
                            write(width)
                            write(Vector2(1.0, offset / stripLength))
                            write(elementF)
                            write(color)
                            previous = current
                        }

                        offset += segmentLength
                    }
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