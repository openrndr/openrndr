import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DynamicParameterSource
import org.openrndr.draw.GlslFunction
import org.openrndr.draw.ShadeStyleFilter1to1
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.glslFunction
import org.openrndr.draw.loadImage
import org.openrndr.draw.shadeStyle
import org.openrndr.math.Vector2

/** a warp preset: a function `vec2 warp(vec2 uv)` with properties for its parameters */
abstract class WarpPreset : DynamicParameterSource {
    abstract override val dynamicParameter: GlslFunction
}

/** moves the coordinates along sines */
class Ripple : WarpPreset() {
    override val dynamicParameter = glslFunction<Vector2>(
        "vec2 uv", "uv + p_amount * sin(uv.yx * p_frequency + p_phase)"
    ) {
        parameter("amount", 0.02)
        parameter("frequency", 30.0)
        parameter("phase", 0.0)
    }
    var amount: Double by dynamicParameter.Parameter("amount")
    var phase: Double by dynamicParameter.Parameter("phase")
}

/** rotates the coordinates around the center, more near the center */
class Swirl : WarpPreset() {
    override val dynamicParameter = glslFunction<Vector2>(
        "vec2 uv", """
        vec2 d = uv - 0.5;
        float a = p_angle * exp(-dot(d, d) * 8.0);
        return 0.5 + mat2(cos(a), sin(a), -sin(a), cos(a)) * d;
        """.trimIndent()
    ) {
        parameter("angle", 2.0)
    }
    var angle: Double by dynamicParameter.Parameter("angle")
}

/** a preset made of two presets: warps with [first], then with [second] */
class Chain(first: WarpPreset, second: WarpPreset) : WarpPreset() {
    override val dynamicParameter = glslFunction<Vector2>("vec2 uv", "p_second(p_first(uv))") {
        parameter("first", first)
        parameter("second", second)
    }
}

/** a filter that reads its input at the coordinates its [warp] returns */
class Warp : ShadeStyleFilter1to1(shadeStyle {
    fragmentTransform = "x_fill = texture(p_input0, p_warp(c_textureCoordinate));"
}) {
    var warp: WarpPreset = Ripple()
        set(value) {
            // the contract of the slot is checked when a preset is assigned
            require(value.dynamicParameter.hasContract("vec2", "vec2")) { "a warp is a function vec2(vec2)" }
            field = value
            parameter("warp", value)
        }

    init {
        parameter("warp", warp)
    }
}

/**
 * Presets are dynamic parameter sources: objects with properties that provide a GLSL function. A filter takes a
 * preset for its warp, presets take other presets. Press a key to switch between presets, presets with the same
 * structure share shaders, values change without new shaders.
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        val image = loadImage("data/images/rgb-8_8_8.jpg")
        val warped = colorBuffer(image.width, image.height)

        val ripple = Ripple()
        val swirl = Swirl()
        val presets = listOf(ripple, swirl, Chain(swirl, ripple))
        var index = 0

        val filter = Warp()
        filter.warp = presets[index]

        keyboard.keyDown.listen {
            index = (index + 1) % presets.size
            filter.warp = presets[index]
        }

        extend {
            ripple.phase = seconds * 4.0
            swirl.angle = 3.0 * kotlin.math.sin(seconds)
            filter.apply(image, warped)

            drawer.clear(ColorRGBa.BLACK)
            drawer.image(warped, 0.0, 0.0, width.toDouble(), height.toDouble())
        }
    }
}
