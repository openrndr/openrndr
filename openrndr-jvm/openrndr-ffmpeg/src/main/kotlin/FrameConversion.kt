package org.openrndr.ffmpeg

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.Filter
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import java.nio.ByteBuffer

/** the luminance HDR video is assumed to peak at when it has no metadata for it */
private const val DEFAULT_PEAK_LUMINANCE = 1000.0

/** the luminance of SDR white in HDR video */
private const val REFERENCE_WHITE = 203.0

/**
 * Converts frames to the colors of the video player's color buffer: linear light that is tone mapped for SDR color
 * buffers and kept as it is for HDR color buffers
 */
internal class FrameConverter : AutoCloseable {
    private var filter: Filter? = null

    /** holds frames that were converted to RGB on the CPU */
    private var rgbFrame: ColorBuffer? = null

    /** the peak luminance of the video, from the last frame that had metadata for it */
    private var peakLuminance = DEFAULT_PEAK_LUMINANCE

    /**
     * Can [color] be written to [target] as it is? This is the case for SDR video and SDR targets.
     */
    fun isDirect(color: ColorDescription, target: ColorBuffer) =
        !color.isHdr && !color.bt2020 && !target.type.isFloat

    /**
     * Converts the [luma] and [chroma] planes of a YCbCr frame to [target]
     */
    fun convertPlanes(
        color: ColorDescription,
        format: PlanarFormat,
        luma: ColorBuffer,
        chroma: ColorBuffer,
        target: ColorBuffer
    ) {
        val filter = filter(color, target)
        filter.parameters["yCbCr"] = true
        filter.parameters["lumaCoefficients"] = Vector2(color.matrix.kr, color.matrix.kb)
        filter.parameters["codeScale"] = format.codeScale
        filter.parameters["codeRange"] =
            Vector4(format.lumaOffset, format.lumaRange, format.chromaOffset, format.chromaRange)
        filter.apply(arrayOf(luma, chroma), target)
    }

    /**
     * Converts a frame that was converted to 8 bit, or [rgb16] 16 bit, RGBA on the CPU to [target]
     */
    fun convertRGB(color: ColorDescription, buffer: ByteBuffer, rgb16: Boolean, target: ColorBuffer) {
        val type = if (rgb16) ColorType.UINT16 else ColorType.UINT8
        val source = rgbFrame?.takeIf { it.width == target.width && it.height == target.height && it.type == type }
            ?: colorBuffer(target.width, target.height, format = ColorFormat.RGBa, type = type).also {
                rgbFrame?.destroy()
                rgbFrame = it
            }
        source.writeBuffer(buffer)
        val filter = filter(color, target)
        filter.parameters["yCbCr"] = false
        filter.apply(source, target)
    }

    private fun filter(color: ColorDescription, target: ColorBuffer): Filter {
        val filter = filter ?: Filter(filterShaderFromCode(conversionShader, "video-frame-conversion")).also {
            filter = it
        }
        color.peakLuminance?.let { peakLuminance = it }
        filter.parameters["transfer"] = color.transfer.ordinal
        filter.parameters["bt2020"] = color.bt2020
        filter.parameters["toneMap"] = !target.type.isFloat
        filter.parameters["peak"] = peakLuminance / REFERENCE_WHITE
        // sRGB targets encode what is written, other 8 bit targets store sRGB encoded colors
        filter.parameters["encodeSRGB"] = target.type == ColorType.UINT8
        return filter
    }

    override fun close() {
        filter?.destroy()
        rgbFrame?.destroy()
    }
}

/**
 * Converts a frame to linear light, in which 1.0 is SDR white. The frame is either YCbCr, in a luma (tex0) and a
 * chroma (tex1) plane, or R'G'B' (tex0).
 */
private val conversionShader = """
    |in vec2 v_texCoord0;
    |uniform sampler2D tex0;
    |uniform sampler2D tex1;
    |uniform vec2 textureSize0;
    |uniform vec2 textureSize1;
    |uniform vec2 targetSize;
    |
    |uniform bool yCbCr;
    |// Kr and Kb of the YCbCr to RGB matrix
    |uniform vec2 lumaCoefficients;
    |// scales samples to code values
    |uniform float codeScale;
    |// offset and range of the luma and chroma code values
    |uniform vec4 codeRange;
    |
    |// 0: SDR, 1: PQ, 2: HLG
    |uniform int transfer;
    |uniform bool bt2020;
    |// map to SDR, with the peak luminance relative to SDR white
    |uniform bool toneMap;
    |uniform float peak;
    |uniform bool encodeSRGB;
    |out vec4 o_color;
    |
    |const float referenceWhite = $REFERENCE_WHITE;
    |
    |vec3 decodeSRGB(vec3 c) {
    |    return mix(c / 12.92, pow((c + 0.055) / 1.055, vec3(2.4)), step(0.04045, c));
    |}
    |
    |vec3 encodeSRGBColor(vec3 c) {
    |    return mix(c * 12.92, 1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055, step(0.0031308, c));
    |}
    |
    |// SMPTE ST 2084, to nits relative to the reference white
    |vec3 decodePQ(vec3 e) {
    |    const float m1 = 0.1593017578125;
    |    const float m2 = 78.84375;
    |    const float c1 = 0.8359375;
    |    const float c2 = 18.8515625;
    |    const float c3 = 18.6875;
    |    vec3 p = pow(e, vec3(1.0 / m2));
    |    return 10000.0 * pow(max(p - c1, 0.0) / (c2 - c3 * p), vec3(1.0 / m1)) / referenceWhite;
    |}
    |
    |// ARIB STD-B67 for a 1000 nit display, relative to the reference white
    |vec3 decodeHLG(vec3 e) {
    |    const float a = 0.17883277;
    |    const float b = 0.28466892;
    |    const float c = 0.55991073;
    |    vec3 scene = mix(e * e / 3.0, (exp((e - c) / a) + b) / 12.0, step(0.5, e));
    |    float luminance = dot(scene, vec3(0.2627, 0.6780, 0.0593));
    |    return 1000.0 * pow(max(luminance, 1e-6), 0.2) * scene / referenceWhite;
    |}
    |
    |// BT.2020 to BT.709 primaries, for linear colors
    |const mat3 fromBT2020 = mat3(
    |    1.6605, -0.1246, -0.0182,
    |    -0.5876, 1.1329, -0.1006,
    |    -0.0728, -0.0083, 1.1187
    |);
    |
    |// keeps luminance up to the knee and compresses luminance above it, so the peak luminance maps to 1.0
    |vec3 toneMapColor(vec3 color) {
    |    const float knee = 0.8;
    |    float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
    |    if (luminance <= knee || peak <= 1.0) {
    |        return color;
    |    }
    |    float e = (luminance - knee) / (1.0 - knee);
    |    float maximum = (peak - knee) / (1.0 - knee);
    |    float mapped = knee + (1.0 - knee) * e * (1.0 + e / (maximum * maximum)) / (1.0 + e);
    |    return color * (mapped / luminance);
    |}
    |
    |void main() {
    |    // the frame can be smaller than the planes that hold it, chroma has half the resolution of luma
    |    vec2 position = v_texCoord0 * targetSize;
    |    vec3 coded;
    |    if (yCbCr) {
    |        float y = (texture(tex0, position / textureSize0).r * codeScale - codeRange.x) / codeRange.y;
    |        vec2 cbcr = (texture(tex1, 0.5 * position / textureSize1).rg * codeScale - codeRange.z) / codeRange.w;
    |        float kr = lumaCoefficients.x;
    |        float kb = lumaCoefficients.y;
    |        float r = y + 2.0 * (1.0 - kr) * cbcr.y;
    |        float b = y + 2.0 * (1.0 - kb) * cbcr.x;
    |        float g = (y - kr * r - kb * b) / (1.0 - kr - kb);
    |        coded = vec3(r, g, b);
    |    } else {
    |        coded = texture(tex0, position / textureSize0).rgb;
    |    }
    |    coded = clamp(coded, 0.0, 1.0);
    |
    |    vec3 color = transfer == 1 ? decodePQ(coded) : transfer == 2 ? decodeHLG(coded) : decodeSRGB(coded);
    |    if (bt2020) {
    |        color = fromBT2020 * color;
    |    }
    |    if (toneMap) {
    |        if (transfer != 0) {
    |            color = toneMapColor(color);
    |        }
    |        color = clamp(color, 0.0, 1.0);
    |    }
    |    o_color = vec4(encodeSRGB ? encodeSRGBColor(color) : color, 1.0);
    |}
""".trimMargin()
