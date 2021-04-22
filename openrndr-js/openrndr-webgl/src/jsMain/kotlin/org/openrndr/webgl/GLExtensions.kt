package org.openrndr.webgl

import org.khronos.webgl.WebGLRenderingContext

external interface ANGLEinstancedArrays {
    fun vertexAttribDivisorANGLE(index: Int, divisor: Int)
    fun drawArraysInstancedANGLE(mode: Int, first: Int, count: Int, primcount: Int)
    fun drawElementsInstancedANGLE(mode: Int, count: Int, type: Int, offset: Int, primcount: Int)
}



external interface OESTextureFloat
external interface OESTextureHalfFloat

external interface OESTextureFloatLinear
external interface OESTextureHalfFloatLinear




external interface EXTColorBufferHalfFloat
const val RGBA16F_EXT = 0x881A
const val RGB16F_EXT = 0x881B


const val HALF_FLOAT_OES: Int = 0x8D61

external interface EXTFloatBend

external interface WEBGLColorBufferFloat

const val RGBA32F_EXT = 0x8814
const val FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE_EXT = 0x8211
const val UNSIGNED_NORMALIZED_EXT = 0x8C17


external interface WEBGLDrawBuffers


val COLOR_ATTACHMENT0_WEBGL     = 0x8CE0
val COLOR_ATTACHMENT1_WEBGL     = 0x8CE1
val COLOR_ATTACHMENT2_WEBGL     = 0x8CE2
val COLOR_ATTACHMENT3_WEBGL     = 0x8CE3
val COLOR_ATTACHMENT4_WEBGL     = 0x8CE4
val COLOR_ATTACHMENT5_WEBGL     = 0x8CE5
val COLOR_ATTACHMENT6_WEBGL     = 0x8CE6
val COLOR_ATTACHMENT7_WEBGL     = 0x8CE7
val COLOR_ATTACHMENT8_WEBGL     = 0x8CE8
val COLOR_ATTACHMENT9_WEBGL     = 0x8CE9
val COLOR_ATTACHMENT10_WEBGL    = 0x8CEA
val COLOR_ATTACHMENT11_WEBGL    = 0x8CEB
val COLOR_ATTACHMENT12_WEBGL    = 0x8CEC
val COLOR_ATTACHMENT13_WEBGL    = 0x8CED
val COLOR_ATTACHMENT14_WEBGL    = 0x8CEE
val COLOR_ATTACHMENT15_WEBGL    = 0x8CEF

val DRAW_BUFFER0_WEBGL          = 0x8825
val DRAW_BUFFER1_WEBGL          = 0x8826
val DRAW_BUFFER2_WEBGL          = 0x8827
val DRAW_BUFFER3_WEBGL          = 0x8828
val DRAW_BUFFER4_WEBGL          = 0x8829
val DRAW_BUFFER5_WEBGL          = 0x882A
val DRAW_BUFFER6_WEBGL          = 0x882B
val DRAW_BUFFER7_WEBGL          = 0x882C
val DRAW_BUFFER8_WEBGL          = 0x882D
val DRAW_BUFFER9_WEBGL          = 0x882E
val DRAW_BUFFER10_WEBGL         = 0x882F
val DRAW_BUFFER11_WEBGL         = 0x8830
val DRAW_BUFFER12_WEBGL         = 0x8831
val DRAW_BUFFER13_WEBGL         = 0x8832
val DRAW_BUFFER14_WEBGL         = 0x8833
val DRAW_BUFFER15_WEBGL         = 0x8834

val MAX_COLOR_ATTACHMENTS_WEBGL = 0x8CDF
val MAX_DRAW_BUFFERS_WEBGL      = 0x8824