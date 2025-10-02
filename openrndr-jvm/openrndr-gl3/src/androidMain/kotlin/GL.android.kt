@file:Suppress("NOTHING_TO_INLINE")

package org.openrndr.internal.gl3

import java.nio.ByteBuffer

inline fun notSupportedOnAndroid() {
    error("not supported on Android")
}

actual inline fun glBufferStorage(buffer: Int, size: Long, flags: Int) {
    notSupportedOnAndroid()
}

actual inline fun glNamedBufferSubData(
    buffer: Int,
    offset: Long,
    data: ByteBuffer
) {
    notSupportedOnAndroid()
}

actual inline fun glNamedBufferSubData(
    buffer: Int,
    offset: Long,
    data: IntArray
) {
    notSupportedOnAndroid()
}

actual inline fun glBufferSubData(buffer: Int, offset: Long, data: IntArray) {
    notSupportedOnAndroid()
}

actual inline fun glGetBufferSubData(
    buffer: Int,
    offset: Long,
    result: IntArray
) {
    notSupportedOnAndroid()
}

actual inline fun glGetBufferSubData(
    buffer: Int,
    offset: Long,
    result: ByteBuffer
) {
    notSupportedOnAndroid()
}

actual inline fun glGetNamedBufferSubData(
    buffer: Int,
    size: Long,
    result: IntArray
) {
    notSupportedOnAndroid()

}

actual inline fun glGetNamedBufferSubData(
    buffer: Int,
    size: Long,
    result: ByteBuffer
) {
    notSupportedOnAndroid()

}

actual inline fun glGenerateTextureMipmap(texture: Int) {
    notSupportedOnAndroid()
}

actual inline fun glGetTexImage(
    target: Int,
    level: Int,
    internalType: Int,
    targetType: Int,
    pixels: ByteBuffer
) {
    notSupportedOnAndroid()
}

actual inline fun glGetCompressedTexImage(
    target: Int,
    level: Int,
    pixels: ByteBuffer
) {
    notSupportedOnAndroid()
}

actual inline fun glClearTexImage(
    texture: Int,
    level: Int,
    format: Int,
    type: Int,
    data: FloatArray
) {
    notSupportedOnAndroid()
}

actual inline fun glClearTexSubImage(
    texture: Int,
    level: Int,
    xoffset: Int,
    yoffset: Int,
    zoffset: Int,
    width: Int,
    height: Int,
    depth: Int,
    format: Int,
    type: Int,
    data: FloatArray
) {
    notSupportedOnAndroid()
}

actual inline fun glTexBuffer(target: Int, internalFormat: Int, buffer: Int) {
    notSupportedOnAndroid()
}

actual inline fun glDrawArraysInstancedBaseInstance(
    primitive: Int,
    vertexOffset: Int,
    vertexCount: Int,
    instanceCount: Int,
    instanceOffset: Int
) {
    notSupportedOnAndroid()
}

actual inline fun glDrawElementsInstancedBaseInstance(
    mode: Int,
    count: Int,
    type: Int,
    indices: Long,
    primcount: Int,
    baseinstance: Int
) {
    notSupportedOnAndroid()
}

actual inline fun glGetActiveAtomicCounterBufferi(
    program: Int,
    bufferIndex: Int,
    pname: Int
): Int {
    error("not supported on Android")
}

actual inline fun glTextureStorage3D(
    texture: Int,
    levels: Int,
    internalFormat: Int,
    width: Int,
    height: Int,
    depth: Int
) {
    notSupportedOnAndroid()
}

actual inline fun glGetTextureImage(
    texture: Int,
    level: Int,
    format: Int,
    type: Int,
    pixels: ByteBuffer
) {
    notSupportedOnAndroid()
}

actual inline fun glGetTextureSubImage(
    texture: Int,
    level: Int,
    xoffset: Int,
    yoffset: Int,
    zoffset: Int,
    width: Int,
    height: Int,
    depth: Int,
    format: Int,
    type: Int,
    pixels: ByteBuffer
) {
    notSupportedOnAndroid()
}

actual inline fun glTextureSubImage3D(
    texture: Int,
    level: Int,
    xoffset: Int,
    yoffset: Int,
    zoffset: Int,
    width: Int,
    height: Int,
    depth: Int,
    format: Int,
    type: Int,
    pixels: ByteBuffer
) {
    notSupportedOnAndroid()
}

actual inline fun glClearNamedBufferData(
    buffer: Int,
    internalformat: Int,
    format: Int,
    type: Int,
    data: ByteBuffer
) {
    notSupportedOnAndroid()
}

actual inline fun glClearNamedBufferData(
    buffer: Int,
    internalformat: Int,
    format: Int,
    type: Int,
    data: IntArray
) {
    notSupportedOnAndroid()
}

actual inline fun glClearBufferData(
    target: Int,
    internalformat: Int,
    format: Int,
    type: Int,
    data: IntArray
) {
    notSupportedOnAndroid()
}