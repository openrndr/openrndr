package org.openrndr.draw

import mu.KotlinLogging
import org.openrndr.color.ColorRGBa
import org.openrndr.internal.Driver
import org.openrndr.math.Matrix55
import org.openrndr.shape.Rectangle

private val logger = KotlinLogging.logger {}

/**
 * Line join enumeration
 */
enum class LineJoin {
    MITER,
    BEVEL,
    ROUND
}

/**
 * Line cap enumeration
 */
enum class LineCap {
    ROUND,
    BUTT,
    SQUARE
}

/**
 * Vertex element type enumeration
 */
enum class VertexElementType(val componentCount: Int, val sizeInBytes: Int) {
    /** 32 bit float, or single precision float scalar */
    FLOAT32(1, 4),
    /** 32 bit float, or single precision float 2-component vector */
    VECTOR2_FLOAT32(2, 8),
    /** 32 bit float, or single precision float 3-component vector */
    VECTOR3_FLOAT32(3, 12),
    /** 32 bit float, or single precision float 4-component vector */
    VECTOR4_FLOAT32(4, 16),
    /** 32 bit float, or single precision float 2x2 matrix */
    MATRIX22_FLOAT32(4, 4 * 4),
    /** 32 bit float, or single precision float 3x3 matrix */
    MATRIX33_FLOAT32(9, 9 * 4),
    /** 32 bit float, or single precision float 4x4 matrix */
    MATRIX44_FLOAT32(16, 16 * 4),
}

/**
 * Draw primitive type enumeration
 */
enum class DrawPrimitive {
    TRIANGLES,
    TRIANGLE_STRIP,
    TRIANGLE_FAN,
    POINTS,
    LINES,
    LINE_STRIP
}

enum class StencilTest {
    NEVER,
    LESS,
    LESS_OR_EQUAL,
    GREATER,
    GREATER_OR_EQUAL,
    EQUAL,
    NOT_EQUAL,
    ALWAYS,
    DISABLED,
}

enum class StencilOperation {
    KEEP,
    ZERO,
    REPLACE,
    INCREASE,
    INCREASE_WRAP,
    DECREASE,
    DECREASE_WRAP,
    INVERT
}

/**
 * Depth format enumeration
 */
enum class DepthFormat {
    /** 16 bit integer depth */
    DEPTH16,
    /** 24 bit integer depth */
    DEPTH24,
    /** 32 bit floating point depth */
    DEPTH32F,
    /** 24 bit integer depth plus 8 bit integer stencil */
    DEPTH24_STENCIL8,
    /** 32 bit float depth plus 8 bit integer stencil */
    DEPTH32F_STENCIL8
}

class StencilStyle {
    var stencilFailOperation = StencilOperation.KEEP
    var depthFailOperation = StencilOperation.KEEP
    var depthPassOperation = StencilOperation.KEEP
    var stencilTestMask = 0xff
    var stencilTestReference = 0
    var stencilWriteMask = 0xff
    var stencilTest = StencilTest.DISABLED

    fun stencilFunc(stencilTest: StencilTest, ref: Int, mask: Int) {
        this.stencilTest = stencilTest
        this.stencilTestReference = ref
        this.stencilWriteMask = mask
    }

    fun stencilOp(stencilFail: StencilOperation, depthTestFail: StencilOperation, depthTestPass: StencilOperation) {
        stencilFailOperation = stencilFail
        depthFailOperation = depthTestFail
        depthPassOperation = depthTestPass
    }
}

/**
 * Color format enumeration
 */
@Suppress("EnumEntryName")
enum class ColorFormat {

    /** Format with a single component (red)*/
    R,
    /** Format with two components (red, green)*/
    RG,
    /** Format with three components (red, green, blue)*/
    RGB,
    /** Format with three components in reverse order (blue, green, red)*/
    BGR,
    /** Format with four components (red, green, blue, alpha)*/
    RGBa,
    /** Format with four components in reverse order (blue, green, red, alpha)*/
    BGRa,
    /** Format with three components (red, green, blue) in sRGB space*/
    sRGB,
    /** Format with four components (red, green, blue, alpha) in sRGB space*/
    sRGBa;

    /**
     * The number of (color) components in the format
     */
    val componentCount: Int
        get() {
            return when (this) {
                R -> 1
                RG -> 2
                BGR, RGB, sRGB -> 3
                BGRa, RGBa, sRGBa -> 4
            }
        }
}

/**
 * Color sampling enumeration
 */
enum class ColorSampling {
    /** Normalized between 0 and 1 */
    NORMALIZED,
    UNSIGNED_INTEGER,
    SIGNED_INTEGER
}

/**
 * Color type enumeration
 */
enum class ColorType {
    /** unsigned 8 bit integer type */
    UINT8,
    /** unsigned 16 bit integer type */
    UINT16,
    /** unsigned 8 bit integer type, with integer sampler */
    UINT8_INT,
    /** unsigned 16 bit integer type, with integer sampler */
    UINT16_INT,
    /** signed 8 bit integer type, with integer sampler */
    SINT8_INT,
    /** signed 16 bit integer type, with integer sampler */
    SINT16_INT,
    /** 16 bit float type, or half precision float type */
    FLOAT16,
    /** 32 bit float type, or single precision float type */
    FLOAT32,
    /** Compressed in DXT1 format */
    DXT1,
    /** Compressed in DXT3 format */
    DXT3,
    /** Compressed in DXT5 format */
    DXT5,
    /** Compressed in unsigned normalized BPTC format */
    BPTC_UNORM,
    /** Compressed in floating point BPTC format */
    BPTC_FLOAT,
    /** Compressed in unsigned floating point BPTC format */
    BPTC_UFLOAT;

    /**
     * The type of color sampler to use for this color type
     */
    val colorSampling: ColorSampling
        get() {
            return when (this) {
                UINT8_INT, UINT16_INT -> ColorSampling.UNSIGNED_INTEGER
                else -> ColorSampling.NORMALIZED
            }
        }

    /**
     * The size (in bytes) for this color type
     */
    val componentSize: Int
        get() {
            return when (this) {
                UINT8, UINT8_INT, SINT8_INT -> 1
                UINT16, UINT16_INT, SINT16_INT, FLOAT16 -> 2
                FLOAT32 -> 4
                DXT1, DXT3, DXT5,
                BPTC_UNORM, BPTC_FLOAT, BPTC_UFLOAT -> throw RuntimeException("component size of compressed types cannot be queried")
            }
        }

    /**
     * Specifies if this is a compressed format
     */
    val compressed: Boolean
        get() {
            return when (this) {
                DXT1, DXT3, DXT5, BPTC_UNORM, BPTC_FLOAT, BPTC_UFLOAT -> true
                else -> false
            }
        }
}

/**
 * Cull test pass condition enumeration
 */
enum class CullTestPass {
    /** Cull test should always pass */
    ALWAYS,
    FRONT,
    BACK,
    NEVER
}

/**
 * Depth test pass condition enumeration
 */
enum class DepthTestPass {
    /** Depth test should always pass */
    ALWAYS,
    /** Depth test will only pass when the test value is less than the target value */
    LESS,
    /** Depth test will only pass when the test value is less than or equal to the target value */
    LESS_OR_EQUAL,
    /** Depth test will only pass when the test value is equal to the target value */
    EQUAL,
    /** Depth test will only pass when the test value is greater than the target value */
    GREATER,
    /** Depth test will only pass when the test value is greater than or equal to the the target value */
    GREATER_OR_EQUAL,
    /** Depth test will never pass, thus always fail */
    NEVER
}

enum class BlendMode {
    OVER,
    ADD,
    SUBTRACT,
    MULTIPLY,
    REPLACE
}

@Suppress("unused")
class ChannelMask(val red: Boolean, val green: Boolean, val blue: Boolean, val alpha: Boolean) {
    companion object {
        const val RED = 1
        const val GREEN = 2
        const val BLUE = 4
        const val ALPHA = 8
        val NONE = ChannelMask(false, false, false, false)
        val ALL = ChannelMask(true, true, true, true)
    }
}

private var styleBlocks = mutableMapOf<Long, UniformBlock>()
private var useStyleBlock = true

enum class KernMode {
    DISABLED,
    METRIC
}

data class DrawStyle(
        /** Clipping rectangle, set to null for no clipping */
        var clip: Rectangle? = null,

        /** Fill color, set to null for no fill */
        var fill: ColorRGBa? = ColorRGBa.WHITE,

        /** Stroke color, set to null for no stroke */
        var stroke: ColorRGBa? = ColorRGBa.BLACK,

        var lineCap: LineCap = LineCap.BUTT,
        var lineJoin: LineJoin = LineJoin.MITER,

        var strokeWeight: Double = 1.0,
        var smooth: Boolean = true,

        var quality: DrawQuality = DrawQuality.QUALITY,

        var depthTestPass: DepthTestPass = DepthTestPass.ALWAYS,
        var depthWrite: Boolean = false,
        var blendMode: BlendMode = BlendMode.OVER,
        var cullTestPass: CullTestPass = CullTestPass.ALWAYS,
        var channelWriteMask: ChannelMask = ChannelMask(red = true, green = true, blue = true, alpha = true),

        /** Use alpha to coverage in rendering, used in multi-sampling modes */
        var alphaToCoverage: Boolean = false,

        var shadeStyle: ShadeStyle? = null,
        var fontMap: FontMap? = null,
        var kerning: KernMode = KernMode.METRIC,

        var stencil: StencilStyle = StencilStyle(),
        var frontStencil: StencilStyle = stencil,
        var backStencil: StencilStyle = stencil,

        var colorMatrix: Matrix55 = Matrix55.IDENTITY
) {

    fun applyToShader(shader: Shader) {
        if (!useStyleBlock) {
            if (shader.hasUniform("u_fill")) {
                shader.uniform("u_fill", fill ?: ColorRGBa.TRANSPARENT)
            }

            if (shader.hasUniform("u_stroke")) {
                shader.uniform("u_stroke", stroke ?: ColorRGBa.TRANSPARENT)
            }

            if (shader.hasUniform("u_strokeWeight")) {
                shader.uniform("u_strokeWeight", if (stroke != null) strokeWeight else 0.0)
            }

            if (shader.hasUniform("u_colorMatrix")) {
                shader.uniform("u_colorMatrix", colorMatrix.floatArray)
            }
        } else {
            val styleBlock = styleBlocks.getOrPut(Driver.driver.contextID) {
                logger.debug { "creating styleblock UBO for ${Driver.driver.contextID}" }
                shader.createBlock("StyleBlock")
            }

            styleBlock.apply {
                uniform("u_fill", fill ?: ColorRGBa.TRANSPARENT)
                uniform("u_stroke", stroke ?: ColorRGBa.TRANSPARENT)
                uniform("u_strokeWeight", strokeWeight.toFloat())
                uniform("u_colorMatrix", colorMatrix)
                shader.block("StyleBlock", this)
                if (dirty) {
                    logger.trace { "styleblock UBO for ${Driver.driver.contextID} is dirty -> upload" }
                    upload()
                }
            }
        }
    }
}
