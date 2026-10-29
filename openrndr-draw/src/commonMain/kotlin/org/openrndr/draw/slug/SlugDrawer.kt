package org.openrndr.draw.slug

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.draw.boundary.BoundaryCommand
import org.openrndr.draw.boundary.BoundaryDrawCommand
import org.openrndr.draw.boundary.BoundaryDrawer
import org.openrndr.draw.boundary.BoundaryMap
import org.openrndr.draw.boundary.boundaryMap
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.shape.FillRule
import org.openrndr.shape.Rectangle
import kotlin.jvm.JvmRecord

typealias SlugCommand = BoundaryCommand

@JvmRecord
data class SlugDrawCommand(override val instances: VertexBuffer, override val count: Int) : BoundaryDrawCommand

class SlugDrawer : BoundaryDrawer {
    val vb = vertexBuffer(vertexFormat {
        position(3)
        textureCoordinate(2)
    }, 6)

    init {
        val b = Rectangle(0.0, 0.0, 1.0, 1.0)

        vb.put {
            write(b.position(0.0, 0.0).xy0)
            write(Vector2(0.0, 0.0))

            write(b.position(1.0, 0.0).xy0)
            write(Vector2(1.0, 0.0))

            write(b.position(1.0, 1.0).xy0)
            write(Vector2(1.0, 1.0))

            write(b.position(1.0, 1.0).xy0)
            write(Vector2(1.0, 1.0))

            write(b.position(0.0, 1.0).xy0)
            write(Vector2(0.0, 1.0))

            write(b.position(0.0, 0.0).xy0)
            write(Vector2(0.0, 0.0))
        }
    }

    /**
     * How much the coverage of edges in dark colors is increased. At 1.0 the edges of dark colors look as heavy as when
     * they are blended in sRGB, at 0.0 coverage is blended in linear light uncorrected, values above 1.0 increase the
     * coverage further. The correction makes small type and thin lines in dark colors look closer to how browsers and
     * other text renderers draw them. It fades out between dark and light colors, light colors are not corrected.
     *
     * The default of 1.5 matches the weight of small dark type in browsers. It also makes strokes in dark colors look
     * about 0.2 pixels heavier than SVG strokes in browsers, at 1.0 their widths match. Strokes drawn with the drawer are
     * not corrected and look lighter at any setting.
     */
    override var contrast = 1.5

    /**
     * Shapes that are at most this many pixels in width and height are drawn from the ink of their horizontal bands
     * instead of from their curves, which is faster and averages the ink of the shape over the pixels it covers. Up to
     * 1.5 times this size the two are blended, so shapes change smoothly when they are scaled. Shapes with a stroke are
     * always drawn from their curves. 0.0 draws all shapes from their curves.
     */
    override var minificationThreshold = 2.0

    override fun createInstanceBuffer(size: Int) = vertexBuffer(instanceFormat, size)

    override fun createMap() = boundaryMap<SlugMap>(4096, 64)

    private fun slugMap(map: BoundaryMap): SlugMap {
        require(map is SlugMap) { "a SlugDrawer draws from a SlugMap, not from a ${map::class.simpleName}, create the map with createMap()" }
        return map
    }

    override val instances = createInstanceBuffer(10_000)
    internal var internalCommand = SlugDrawCommand(instances, 0)

    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators(
        "slug",
        vsGenerator = Driver.instance.shaderGenerators::slugVertexShader,
        fsGenerator = Driver.instance.shaderGenerators::slugFragmentShader
    )

    override fun prepare(map: BoundaryMap, commands: List<BoundaryCommand>, instances: VertexBuffer): SlugDrawCommand {
        val slugMap = slugMap(map)
        instances.put {
            for (command in commands) {
                write(command.transform)

                run {
                    val bounds = slugMap.bounds[command.boundaryIndex]
                    val minmax = Vector4(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height)
                    write(minmax)
                }

                // colors are drawn in linear light, like the fill and stroke of the drawer
                write(command.fill?.toLinear() ?: ColorRGBa.TRANSPARENT)
                write(command.stroke?.toLinear() ?: ColorRGBa.TRANSPARENT)
                write(command.strokeWeight.toFloat())
                write(command.strokeMode)
                write(slugMap.bandIndices[command.boundaryIndex])
                write(slugMap.bandCounts[command.boundaryIndex])
                write(if (slugMap.fillRules[command.boundaryIndex] == FillRule.EVEN_ODD) 1 else 0)
            }
        }
        val drawCommand = SlugDrawCommand(instances, commands.size)

        if (instances === this.instances) {
            internalCommand  = drawCommand
        }

        return drawCommand
    }

    override fun draw(drawer: Drawer, map: BoundaryMap) = draw(drawer, map, internalCommand)

    override fun draw(drawer: Drawer, map: BoundaryMap, command: BoundaryDrawCommand) {
        val slugMap = slugMap(map)
        if (command.count == 0) return

        val drawStyle = drawer.drawStyle
        val shader = shaderManager.shader(
            drawStyle.shadeStyle,
            listOf(vb.vertexFormat),
            listOf(instances.vertexFormat)
        )
        // texture units 0 and 1 are reserved for built-in textures, shade style textures start at 2
        shader.textureBindings[0] = slugMap.curves
        // bands is an integer texture, LINEAR filtering would make it incomplete
        slugMap.bands.filter(MinifyingFilter.NEAREST, MagnifyingFilter.NEAREST)
        shader.textureBindings[1] = slugMap.bands
        shader.begin()
        drawer.context.applyToShader(shader)
        drawStyle.applyToShader(shader)
        shader.uniform("u_slugCurves", 0)
        shader.uniform("u_slugBands", 1)
        shader.uniform("u_slugContrast", contrast)
        shader.uniform("u_slugMinification", minificationThreshold)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawInstances(
            shader,
            listOf(vb),
            listOf(command.instances) + (drawStyle.shadeStyle?.attributes.orEmpty()),
            DrawPrimitive.TRIANGLES,
            0,
            vb.vertexCount,
            0,
            command.count,
            verticesPerPatch = 0
        )
        shader.end()
    }

    override fun close() {
        vb.close()
        instances.close()
        internalCommand = SlugDrawCommand(instances, 0)
    }

    companion object {
        val instanceFormat = vertexFormat {
            attribute("transform", VertexElementType.MATRIX44_FLOAT32)
            attribute("slugBounds", VertexElementType.VECTOR4_FLOAT32)
            attribute("fill", VertexElementType.VECTOR4_FLOAT32)
            attribute("stroke", VertexElementType.VECTOR4_FLOAT32)
            attribute("strokeWeight", VertexElementType.FLOAT32)
            attribute("strokeMode", VertexElementType.INT32)
            attribute("bandIndex", VertexElementType.VECTOR2_INT32)
            attribute("bandCount", VertexElementType.INT32)
            attribute("fillRule", VertexElementType.INT32)
        }
    }
}
