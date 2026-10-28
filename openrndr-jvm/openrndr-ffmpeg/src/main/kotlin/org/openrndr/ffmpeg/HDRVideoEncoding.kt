package org.openrndr.ffmpeg

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.Filter
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.filterShaderFromCode

/**
 * Transfer functions that HDR video is encoded with
 */
enum class HDRTransfer {
    /** SMPTE ST 2084, the transfer function of HDR10 */
    PQ,

    /** ARIB STD-B67 */
    HLG
}

/**
 * Encodes linear frames, in which 1.0 is SDR white, as BT.2020 Y'CbCr with an HDR transfer function. The encoded
 * frame holds packed AYUV with 16 bits per component, limited range 10 bit code values in the most significant bits.
 */
internal class HDRFrameEncoder(width: Int, height: Int, private val transfer: HDRTransfer) : AutoCloseable {
    private val filter = Filter(filterShaderFromCode(encodingShader, "hdr-frame-encoding"))

    /** the encoded frame, AYUV in the RGBA components */
    val encoded: ColorBuffer = colorBuffer(width, height, format = ColorFormat.RGBa, type = ColorType.UINT16)

    fun encode(frame: ColorBuffer): ColorBuffer {
        filter.parameters["transfer"] = transfer.ordinal
        filter.apply(frame, encoded)
        return encoded
    }

    override fun close() {
        filter.destroy()
        encoded.destroy()
    }
}

/**
 * Encodes linear BT.709 colors, in which 1.0 is SDR white (203 nits), as BT.2020 Y'CbCr with PQ (0) or HLG (1)
 */
private val encodingShader = """
    |in vec2 v_texCoord0;
    |uniform sampler2D tex0;
    |uniform int transfer;
    |out vec4 o_color;
    |
    |const float referenceWhite = 203.0;
    |
    |// BT.709 to BT.2020 primaries, for linear colors
    |const mat3 toBT2020 = mat3(
    |    0.6274, 0.0691, 0.0164,
    |    0.3293, 0.9195, 0.0880,
    |    0.0433, 0.0114, 0.8956
    |);
    |
    |// SMPTE ST 2084, from nits
    |vec3 encodePQ(vec3 nits) {
    |    const float m1 = 0.1593017578125;
    |    const float m2 = 78.84375;
    |    const float c1 = 0.8359375;
    |    const float c2 = 18.8515625;
    |    const float c3 = 18.6875;
    |    vec3 y = pow(clamp(nits / 10000.0, 0.0, 1.0), vec3(m1));
    |    return pow((c1 + c2 * y) / (1.0 + c3 * y), vec3(m2));
    |}
    |
    |// ARIB STD-B67, from nits on a 1000 nit display
    |vec3 encodeHLG(vec3 nits) {
    |    const float a = 0.17883277;
    |    const float b = 0.28466892;
    |    const float c = 0.55991073;
    |    const float gamma = 1.2;
    |    vec3 display = clamp(nits / 1000.0, 0.0, 1.0);
    |    // the inverse of the system gamma of the display
    |    float luminance = dot(display, vec3(0.2627, 0.6780, 0.0593));
    |    vec3 scene = luminance > 0.0 ? display * pow(luminance, (1.0 - gamma) / gamma) : vec3(0.0);
    |    return mix(sqrt(3.0 * scene), a * log(max(12.0 * scene - b, 1e-6)) + c, step(1.0 / 12.0, scene));
    |}
    |
    |void main() {
    |    // colors outside of the BT.2020 gamut are clipped
    |    vec3 color = max(toBT2020 * texture(tex0, v_texCoord0).rgb, 0.0) * referenceWhite;
    |    vec3 coded = transfer == 0 ? encodePQ(color) : encodeHLG(color);
    |
    |    // BT.2020 non-constant luminance, limited range 10 bit code values
    |    const float kr = 0.2627;
    |    const float kb = 0.0593;
    |    float y = dot(coded, vec3(kr, 1.0 - kr - kb, kb));
    |    float cb = (coded.b - y) / (2.0 * (1.0 - kb));
    |    float cr = (coded.r - y) / (2.0 * (1.0 - kr));
    |    vec3 codes = vec3(64.0 + 876.0 * y, 512.0 + 896.0 * cb, 512.0 + 896.0 * cr);
    |
    |    // A, Y, Cb, Cr with the code values in the most significant bits of 16 bits
    |    o_color = vec4(1.0, codes * 64.0 / 65535.0);
    |}
""".trimMargin()
