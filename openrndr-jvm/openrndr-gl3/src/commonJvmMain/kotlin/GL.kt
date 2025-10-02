package org.openrndr.internal.gl3

import java.nio.ByteBuffer

expect inline fun glBufferStorage(buffer: Int, size: Long, flags: Int)

expect inline fun glNamedBufferSubData(buffer: Int, offset: Long, data: ByteBuffer)

expect inline fun glNamedBufferSubData(buffer: Int, offset: Long, data: IntArray)
expect inline fun glBufferSubData(buffer: Int, offset: Long, data: IntArray)

expect inline fun glGetBufferSubData(buffer: Int, offset: Long, result: IntArray)

expect inline fun glGetBufferSubData(buffer: Int, offset: Long, result: ByteBuffer)

expect inline fun glGetNamedBufferSubData(buffer: Int, size: Long, result: IntArray)

expect inline fun glGetNamedBufferSubData(buffer: Int, size: Long, result: ByteBuffer)


//expect inline fun glTexBuffer

expect inline fun glGenerateTextureMipmap(texture: Int)

expect inline fun glGetTexImage(
    target: Int,
    level: Int,
    internalType: Int,
    targetType: Int,
    pixels: ByteBuffer
)

expect inline fun glGetCompressedTexImage(target: Int, level: Int, pixels: ByteBuffer)

expect inline fun glClearTexImage(
    texture: Int,
    level: Int,
    format: Int,
    type: Int,
    data: FloatArray
)

expect inline fun glClearTexSubImage(
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
)

expect inline fun glTexBuffer(target: Int, internalFormat: Int, buffer: Int)

expect inline fun glDrawArraysInstancedBaseInstance(
    primitive: Int,
    vertexOffset: Int,
    vertexCount: Int,
    instanceCount: Int,
    instanceOffset: Int
)

expect inline fun glDrawElementsInstancedBaseInstance(
    mode: Int,
    count: Int,
    type: Int,
    indices: Long,
    primcount: Int,
    baseinstance: Int
)


expect inline fun glGetActiveAtomicCounterBufferi(program: Int, bufferIndex: Int, pname: Int): Int

expect inline fun glTextureStorage3D(
    texture: Int,
    levels: Int,
    internalFormat: Int,
    width: Int,
    height: Int,
    depth: Int
)

expect inline fun glGetTextureImage(
    texture: Int,
    level: Int,
    format: Int,
    type: Int,
    pixels: ByteBuffer
)

expect inline fun glGetTextureSubImage(
    texture: Int, level: Int,
    xoffset: Int,
    yoffset: Int,
    zoffset: Int,
    width: Int,
    height: Int,
    depth: Int,
    format: Int,
    type: Int,
    pixels: ByteBuffer
)

expect inline fun glTextureSubImage3D(
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
)

expect inline fun glClearNamedBufferData(
    buffer: Int,
    internalformat: Int,
    format: Int,
    type: Int,
    data: ByteBuffer
)

expect inline fun glClearNamedBufferData(
    buffer: Int,
    internalformat: Int,
    format: Int,
    type: Int,
    data: IntArray
)

expect inline fun glClearBufferData(
    target: Int,
    internalformat: Int,
    format: Int,
    type: Int,
    data: IntArray
)