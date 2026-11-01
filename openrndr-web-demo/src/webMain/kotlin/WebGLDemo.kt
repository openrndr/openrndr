import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.MagnifyingFilter
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolated
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.draw.shadeStyle
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.ShapeContour
import org.openrndr.utils.buffer.MPPBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A long page of text with ten programs, each drawing in its own canvas: openrndr-canvas-0 to openrndr-canvas-9
 */
fun main() {
    val programs: List<Program.() -> Unit> = listOf(
        Program::rotatingSquares, Program::mouseCircle, Program::bouncingCircles, Program::lissajous, Program::text,
        Program::lines, Program::shadeStyleGradient, Program::generatedImage, Program::feedback, Program::stars
    )
    for ((index, program) in programs.withIndex()) {
        application {
            configure {
                canvasId = "openrndr-canvas-$index"
            }
            program {
                program()
            }
        }
    }
}

/** a grid of squares rotating out of phase */
fun Program.rotatingSquares() {
    extend {
        drawer.clear(ColorRGBa.fromHex("#1d3557"))
        drawer.fill = ColorRGBa.fromHex("#f1faee")
        drawer.stroke = null
        val columns = 12
        val size = width / columns.toDouble()
        for (y in 0 until (height / size).toInt() + 1) for (x in 0 until columns) {
            drawer.isolated {
                translate((x + 0.5) * size, (y + 0.5) * size)
                rotate(seconds * 90.0 + (x + y) * 15.0)
                rectangle(-size * 0.3, -size * 0.3, size * 0.6, size * 0.6)
            }
        }
    }
}

/** a circle that follows the mouse */
fun Program.mouseCircle() {
    extend {
        drawer.clear(ColorRGBa.fromHex("#e63946"))
        drawer.fill = ColorRGBa.WHITE
        drawer.stroke = ColorRGBa.BLACK
        drawer.strokeWeight = 4.0
        drawer.circle(mouse.position, 60.0 + 20.0 * sin(seconds * 3.0))
    }
}

/** circles bouncing off the edges, drawn in one batch */
fun Program.bouncingCircles() {
    class Ball(var position: Vector2, var velocity: Vector2)

    val balls = List(60) { i ->
        Ball(Vector2(width * ((i * 37) % 100) / 100.0, height * ((i * 61) % 100) / 100.0), Polar(i * 47.0, 2.0 + i % 5).cartesian)
    }
    extend {
        for (ball in balls) {
            ball.position += ball.velocity
            if (ball.position.x !in 0.0..width.toDouble()) ball.velocity = ball.velocity.copy(x = -ball.velocity.x)
            if (ball.position.y !in 0.0..height.toDouble()) ball.velocity = ball.velocity.copy(y = -ball.velocity.y)
        }
        drawer.clear(ColorRGBa.fromHex("#2a9d8f"))
        drawer.fill = ColorRGBa.fromHex("#e9c46a")
        drawer.stroke = null
        drawer.circles(balls.map { it.position }, 12.0)
    }
}

/** a slowly changing Lissajous figure */
fun Program.lissajous() {
    extend {
        val center = Vector2(width / 2.0, height / 2.0)
        val radius = minOf(width, height) * 0.4
        val points = List(400) {
            val t = it / 400.0 * 2.0 * PI
            center + Vector2(sin(3.0 * t + seconds * 0.5), sin(4.0 * t)) * radius
        }
        drawer.clear(ColorRGBa.fromHex("#264653"))
        drawer.fill = null
        drawer.stroke = ColorRGBa.fromHex("#f4a261")
        drawer.strokeWeight = 3.0
        drawer.contour(ShapeContour.fromPoints(points, closed = true))
    }
}

/** text in the default font */
fun Program.text() {
    extend {
        drawer.clear(ColorRGBa.fromHex("#fefae0"))
        drawer.fill = ColorRGBa.fromHex("#283618")
        for (i in 0 until 8) {
            val x = 20.0 + 40.0 * sin(seconds + i * 0.5)
            drawer.text("OPENRNDR in the browser, line ${i + 1}", x, 40.0 + i * 40.0)
        }
    }
}

/** radiating line segments */
fun Program.lines() {
    extend {
        val center = Vector2(width / 2.0, height / 2.0)
        drawer.clear(ColorRGBa.BLACK)
        drawer.stroke = ColorRGBa.WHITE.opacify(0.8)
        drawer.strokeWeight = 1.5
        drawer.lineSegments(List(180) {
            val angle = it * 2.0 + seconds * 20.0
            val length = 100.0 + 80.0 * sin(it * 0.3 + seconds * 2.0)
            listOf(center, center + Polar(angle, length).cartesian)
        }.flatten())
    }
}

/** an animated gradient made with a shade style */
fun Program.shadeStyleGradient() {
    val gradient = shadeStyle {
        fragmentTransform = """
            vec2 uv = c_boundsPosition.xy;
            x_fill.rgb = 0.5 + 0.5 * cos(p_time + uv.xyx * 6.0 + vec3(0.0, 2.0, 4.0));
        """.trimIndent()
    }
    extend {
        drawer.clear(ColorRGBa.BLACK)
        gradient.parameter("time", seconds)
        drawer.shadeStyle = gradient
        drawer.stroke = null
        drawer.rectangle(drawer.bounds.offsetEdges(-20.0))
    }
}

/** an image generated in a color buffer, drawn tiled and moving */
fun Program.generatedImage() {
    val size = 64
    val image = colorBuffer(size, size, 1.0, ColorFormat.RGBa, ColorType.UINT8)
    val pixels = MPPBuffer.allocate(size * size * 4)
    for (y in 0 until size) for (x in 0 until size) {
        val checker = ((x / 8) + (y / 8)) % 2 == 0
        pixels.put((x * 4).toByte())
        pixels.put((y * 4).toByte())
        pixels.put(if (checker) 255.toByte() else 64.toByte())
        pixels.put(255.toByte())
    }
    pixels.rewind()
    image.write(pixels, ColorFormat.RGBa, ColorType.UINT8, 0, 0, size, size, 0)
    // a scaled copy, which is drawn
    val tile = colorBuffer(size / 2, size / 2, 1.0, ColorFormat.RGBa, ColorType.UINT8)
    image.copyTo(tile, 0, 0, IntRectangle(0, 0, size, size), IntRectangle(0, 0, size / 2, size / 2), MagnifyingFilter.NEAREST)
    extend {
        drawer.clear(ColorRGBa.fromHex("#6d597a"))
        val offset = (seconds * 40.0) % (size * 2.0)
        for (y in -1 until height / (size * 2) + 1) for (x in -1 until width / (size * 2) + 1) {
            drawer.image(image, x * size * 2.0 + offset, y * size * 2.0 + offset / 2.0)
            drawer.image(tile, x * size * 2.0 + offset + size, y * size * 2.0 + offset / 2.0 + size)
        }
    }
}

/** circles drawn into a render target that fades, a feedback trail */
fun Program.feedback() {
    val target = renderTarget(width, height) {
        colorBuffer()
    }
    drawer.isolatedWithTarget(target) { clear(ColorRGBa.BLACK) }
    // a preview at a quarter of the size, copied every frame
    val preview = colorBuffer(width / 4, height / 4)
    extend {
        drawer.isolatedWithTarget(target) {
            ortho(target)
            fill = ColorRGBa.BLACK.opacify(0.05)
            stroke = null
            rectangle(0.0, 0.0, width.toDouble(), height.toDouble())
            fill = ColorRGBa.fromHex("#ffb703")
            val t = seconds * 2.0
            circle(width / 2.0 + cos(t) * width * 0.35, height / 2.0 + sin(t * 1.3) * height * 0.35, 20.0)
        }
        drawer.image(target.colorBuffer(0))
        target.colorBuffer(0).copyTo(
            preview, 0, 0,
            IntRectangle(0, 0, target.colorBuffer(0).effectiveWidth, target.colorBuffer(0).effectiveHeight),
            IntRectangle(0, 0, preview.effectiveWidth, preview.effectiveHeight),
            MagnifyingFilter.LINEAR
        )
        drawer.stroke = ColorRGBa.WHITE
        drawer.fill = null
        drawer.image(preview, width - preview.width - 10.0, 10.0)
        drawer.rectangle(width - preview.width - 10.0, 10.0, preview.width.toDouble(), preview.height.toDouble())
    }
}

/** spinning stars as filled contours */
fun Program.stars() {
    fun star(center: Vector2, outer: Double, inner: Double, rotation: Double) = ShapeContour.fromPoints(
        List(10) { Polar(rotation + it * 36.0, if (it % 2 == 0) outer else inner).cartesian + center }, closed = true
    )
    extend {
        drawer.clear(ColorRGBa.fromHex("#03045e"))
        drawer.fill = ColorRGBa.fromHex("#ffd60a")
        drawer.stroke = ColorRGBa.fromHex("#ffc300")
        drawer.strokeWeight = 2.0
        for (i in 0 until 7) {
            val center = Vector2(width * (i + 0.5) / 7.0, height / 2.0 + sin(seconds + i) * height * 0.25)
            drawer.contour(star(center, 40.0, 16.0, seconds * 60.0 * if (i % 2 == 0) 1 else -1))
        }
        drawer.fill = null
        drawer.stroke = ColorRGBa.WHITE.opacify(0.3)
        drawer.circle(Circle(width / 2.0, height / 2.0, minOf(width, height) * 0.45))
        // a second canvas with text, every canvas has its own font map
        drawer.fill = ColorRGBa.WHITE
        drawer.text("stars, frame ${frameCount}", 20.0, height - 20.0)
    }
}
