package org.openrndr.draw.slug

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.shape.Rectangle
import kotlin.jvm.JvmRecord

@JvmRecord
data class SlugCommand(val slugIndex: Int, val transform: Matrix44, val fill: ColorRGBa?, val stroke: ColorRGBa?, val strokeWeight: Double, val strokeMode : Int = 0)

@JvmRecord
data class SlugDrawCommand(val instances: VertexBuffer, val count: Int)

class SlugDrawer: AutoCloseable {
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

    fun createInstanceBuffer(size: Int) = vertexBuffer(instanceFormat, size)

    val instances = createInstanceBuffer(10_000)
    internal var internalCommand = SlugDrawCommand(instances, 0)

    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators(
        "slug",
        vsGenerator = Driver.instance.shaderGenerators::slugVertexShader,
        fsGenerator = Driver.instance.shaderGenerators::slugFragmentShader
    )

    fun prepare(slugMap: SlugMap, slugCommands: List<SlugCommand>, instances: VertexBuffer = this.instances): SlugDrawCommand {
        instances.put {
            for (command in slugCommands) {
                write(command.transform)

                run {
                    val bounds = slugMap.bounds[command.slugIndex]
                    val minmax = Vector4(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height)
                    write(minmax)
                }

                write(command.fill ?: ColorRGBa.TRANSPARENT)
                write(command.stroke ?: ColorRGBa.TRANSPARENT)
                write(command.strokeWeight.toFloat())
                write(command.strokeMode)
                write(slugMap.bandIndices[command.slugIndex])
                write(slugMap.bandCounts[command.slugIndex])
            }
        }
        val drawCommand = SlugDrawCommand(instances, slugCommands.size)

        if (instances === this.instances) {
            internalCommand  = drawCommand
        }

        return drawCommand
    }

    fun draw(drawer: Drawer, slugMap: SlugMap, command: SlugDrawCommand = internalCommand) {
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
        }
    }
}
