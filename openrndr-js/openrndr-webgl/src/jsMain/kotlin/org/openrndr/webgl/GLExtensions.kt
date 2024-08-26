package org.openrndr.webgl


external interface OESStandardDerivatives
const val FRAGMENT_SHADER_DERIVATIVE_HINT_OES = 0x8b8b

external interface ANGLEinstancedArrays {
    fun vertexAttribDivisorANGLE(index: Int, divisor: Int)
    fun drawArraysInstancedANGLE(mode: Int, first: Int, count: Int, primcount: Int)
    fun drawElementsInstancedANGLE(mode: Int, count: Int, type: Int, offset: Int, primcount: Int)
}



external interface OESTextureFloat
external interface OESTextureHalfFloat

external interface OESTextureFloatLinear
external interface OESTextureHalfFloatLinear

@Suppress("ClassName")
external interface EXT_color_buffer_float

external interface EXTColorBufferHalfFloat
external interface EXTColorBufferFloat
const val RGBA16F_EXT = 0x881A
const val RGB16F_EXT = 0x881B


const val HALF_FLOAT_OES: Int = 0x8D61

external interface EXTFloatBend

external interface WEBGLColorBufferFloat

const val RGBA32F_EXT = 0x8814
const val FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE_EXT = 0x8211
const val UNSIGNED_NORMALIZED_EXT = 0x8C17


external interface WEBGLDrawBuffers


const val COLOR_ATTACHMENT0_WEBGL     = 0x8CE0
const val COLOR_ATTACHMENT1_WEBGL     = 0x8CE1
const val COLOR_ATTACHMENT2_WEBGL     = 0x8CE2
const val COLOR_ATTACHMENT3_WEBGL     = 0x8CE3
const val COLOR_ATTACHMENT4_WEBGL     = 0x8CE4
const val COLOR_ATTACHMENT5_WEBGL     = 0x8CE5
const val COLOR_ATTACHMENT6_WEBGL     = 0x8CE6
const val COLOR_ATTACHMENT7_WEBGL     = 0x8CE7
const val COLOR_ATTACHMENT8_WEBGL     = 0x8CE8
const val COLOR_ATTACHMENT9_WEBGL     = 0x8CE9
const val COLOR_ATTACHMENT10_WEBGL    = 0x8CEA
const val COLOR_ATTACHMENT11_WEBGL    = 0x8CEB
const val COLOR_ATTACHMENT12_WEBGL    = 0x8CEC
const val COLOR_ATTACHMENT13_WEBGL    = 0x8CED
const val COLOR_ATTACHMENT14_WEBGL    = 0x8CEE
const val COLOR_ATTACHMENT15_WEBGL    = 0x8CEF

const val DRAW_BUFFER0_WEBGL          = 0x8825
const val DRAW_BUFFER1_WEBGL          = 0x8826
const val DRAW_BUFFER2_WEBGL          = 0x8827
const val DRAW_BUFFER3_WEBGL          = 0x8828
const val DRAW_BUFFER4_WEBGL          = 0x8829
const val DRAW_BUFFER5_WEBGL          = 0x882A
const val DRAW_BUFFER6_WEBGL          = 0x882B
const val DRAW_BUFFER7_WEBGL          = 0x882C
const val DRAW_BUFFER8_WEBGL          = 0x882D
const val DRAW_BUFFER9_WEBGL          = 0x882E
const val DRAW_BUFFER10_WEBGL         = 0x882F
const val DRAW_BUFFER11_WEBGL         = 0x8830
const val DRAW_BUFFER12_WEBGL         = 0x8831
const val DRAW_BUFFER13_WEBGL         = 0x8832
const val DRAW_BUFFER14_WEBGL         = 0x8833
const val DRAW_BUFFER15_WEBGL         = 0x8834

const val MAX_COLOR_ATTACHMENTS_WEBGL = 0x8CDF
const val MAX_DRAW_BUFFERS_WEBGL      = 0x8824

external interface WEBGLCompressedTextureS3TC

const val COMPRESSED_RGB_S3TC_DXT1_EXT        = 0x83F0
const val COMPRESSED_RGBA_S3TC_DXT1_EXT       = 0x83F1
const val COMPRESSED_RGBA_S3TC_DXT3_EXT       = 0x83F2
const val COMPRESSED_RGBA_S3TC_DXT5_EXT       = 0x83F3

external interface WEBGLCompressedTextureEtc1

const val COMPRESSED_RGB_ETC1_WEBGL = 0x8D64

//external interface WEBGLCompressedTextureETC

//const val COMPRESSED_R11_EAC                        = 0x9270
//const val COMPRESSED_SIGNED_R11_EAC                 = 0x9271
//const val COMPRESSED_RG11_EAC                       = 0x9272
//const val COMPRESSED_SIGNED_RG11_EAC                = 0x9273
//const val COMPRESSED_RGB8_ETC2                      = 0x9274
//const val COMPRESSED_SRGB8_ETC2                     = 0x9275
//const val COMPRESSED_RGB8_PUNCHTHROUGH_ALPHA1_ETC2  = 0x9276
//const val COMPRESSED_SRGB8_PUNCHTHROUGH_ALPHA1_ETC2 = 0x9277
//const val COMPRESSED_RGBA8_ETC2_EAC                 = 0x9278
//const val COMPRESSED_SRGB8_ALPHA8_ETC2_EAC          = 0x9279


external interface WEBGLCompressedTextureS3TCSrgb

const val COMPRESSED_SRGB_S3TC_DXT1_EXT        = 0x8C4C
const val COMPRESSED_SRGB_ALPHA_S3TC_DXT1_EXT  = 0x8C4D
const val COMPRESSED_SRGB_ALPHA_S3TC_DXT3_EXT  = 0x8C4E
const val COMPRESSED_SRGB_ALPHA_S3TC_DXT5_EXT  = 0x8C4F


const val COMPRESSED_RGBA_BPTC_UNORM_EXT = 0x8E8C
const val COMPRESSED_SRGB_ALPHA_BPTC_UNORM_EXT = 0x8E8D
const val COMPRESSED_RGB_BPTC_SIGNED_FLOAT_EXT = 0x8E8E
const val COMPRESSED_RGB_BPTC_UNSIGNED_FLOAT_EXT = 0x8E8F


external interface WEBGLDepthTexture
const val UNSIGNED_INT_24_8_WEBGL = 0x84FA
