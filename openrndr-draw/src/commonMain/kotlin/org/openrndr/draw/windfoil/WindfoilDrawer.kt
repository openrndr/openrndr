package org.openrndr.draw.windfoil

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.draw.slug.SlugCommand
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.shape.FillRule
import org.openrndr.shape.Rectangle

/**
 * Prepared windfoil instances, drawn with [WindfoilDrawer.draw]
 */
class WindfoilDrawCommand(val instances: VertexBuffer, val count: Int)

/**
 * Draws shapes from a [WindfoilMap] with the windfoil algorithm. The coverage of a pixel is the box filtered
 * coverage of the shape over the pixel, computed from the winding integral of its curves. Strokes are drawn from the
 * distance to the curves within reach of the stroke.
 *
 * Commands are [SlugCommand]s, of which [SlugCommand.slugIndex] is the index of a shape in the [WindfoilMap].
 */
class WindfoilDrawer : AutoCloseable {
    private val vb = vertexBuffer(vertexFormat {
        position(3)
        textureCoordinate(2)
    }, 6)

    init {
        val b = Rectangle(0.0, 0.0, 1.0, 1.0)
        vb.put {
            for (corner in listOf(Vector2(0.0, 0.0), Vector2(1.0, 0.0), Vector2(1.0, 1.0), Vector2(1.0, 1.0), Vector2(0.0, 1.0), Vector2(0.0, 0.0))) {
                write(b.position(corner).xy0)
                write(corner)
            }
        }
    }

    /**
     * How much the coverage of edges in dark colors is increased, see [org.openrndr.draw.slug.SlugDrawer.contrast]
     */
    var contrast = 1.5

    /**
     * Shapes that are at most this many pixels in width and height are drawn from the winding densities of their
     * bands instead of from their curves, which is faster but approximate. Up to 1.5 times this size the two are
     * blended. Shapes with a stroke are always drawn from their curves. 0.0 draws all shapes from their curves.
     */
    var minificationThreshold = 2.0

    /**
     * When larger than 0, coverage is the fraction of exactSamples x exactSamples points in the pixel that are inside
     * the shape according to its fill rule, instead of the winding integral. Slow, meant as a reference.
     */
    var exactSamples = 0

    fun createInstanceBuffer(size: Int) = vertexBuffer(instanceFormat, size)

    val instances = createInstanceBuffer(10_000)
    private var internalCommand = WindfoilDrawCommand(instances, 0)

    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators(
        "windfoil",
        vsGenerator = Driver.instance.shaderGenerators::windfoilVertexShader,
        fsGenerator = Driver.instance.shaderGenerators::windfoilFragmentShader
    )

    fun prepare(map: WindfoilMap, commands: List<SlugCommand>, instances: VertexBuffer = this.instances): WindfoilDrawCommand {
        instances.put {
            for (command in commands) {
                val shape = map.shapes[command.slugIndex]
                write(command.transform)
                val bounds = shape.bounds
                write(Vector4(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height))
                // colors are drawn in linear light, like the fill and stroke of the drawer
                write(command.fill?.toLinear() ?: ColorRGBa.TRANSPARENT)
                write(command.stroke?.toLinear() ?: ColorRGBa.TRANSPARENT)
                write(command.strokeWeight.toFloat())
                write(command.strokeMode)
                write(shape.rowBase)
                write(shape.bandCount)
                write(Vector2(shape.bandHeight, shape.inverseBandHeight))
                write(if (shape.fillRule == FillRule.EVEN_ODD) 1 else 0)
            }
        }
        val drawCommand = WindfoilDrawCommand(instances, commands.size)
        if (instances === this.instances) {
            internalCommand = drawCommand
        }
        return drawCommand
    }

    fun draw(drawer: Drawer, map: WindfoilMap, command: WindfoilDrawCommand = internalCommand) {
        if (command.count == 0) return

        val drawStyle = drawer.drawStyle
        val shader = shaderManager.shader(
            drawStyle.shadeStyle,
            listOf(vb.vertexFormat),
            listOf(command.instances.vertexFormat)
        )
        // texture units 0 and 1 are reserved for built-in textures, shade style textures start at 2
        shader.textureBindings[0] = map.curves
        shader.textureBindings[1] = map.rows
        shader.begin()
        drawer.context.applyToShader(shader)
        drawStyle.applyToShader(shader)
        shader.uniform("u_windfoilCurves", 0)
        shader.uniform("u_windfoilRows", 1)
        shader.uniform("u_windfoilContrast", contrast)
        shader.uniform("u_windfoilMinification", minificationThreshold)
        shader.uniform("u_windfoilExactSamples", exactSamples)
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
        internalCommand = WindfoilDrawCommand(instances, 0)
    }

    companion object {
        val instanceFormat = vertexFormat {
            attribute("transform", VertexElementType.MATRIX44_FLOAT32)
            attribute("windfoilBounds", VertexElementType.VECTOR4_FLOAT32)
            attribute("fill", VertexElementType.VECTOR4_FLOAT32)
            attribute("stroke", VertexElementType.VECTOR4_FLOAT32)
            attribute("strokeWeight", VertexElementType.FLOAT32)
            attribute("strokeMode", VertexElementType.INT32)
            attribute("rowBase", VertexElementType.INT32)
            attribute("bandCount", VertexElementType.INT32)
            attribute("bandScale", VertexElementType.VECTOR2_FLOAT32)
            attribute("fillRule", VertexElementType.INT32)
        }
    }
}
