@file:Suppress("NOTHING_TO_INLINE")

package org.openrndr.internal.gl3

import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL31C
import org.lwjgl.opengl.GL42C
import org.lwjgl.opengl.GL44C
import org.lwjgl.opengl.GL45C
import org.lwjgl.opengles.GLES32
import java.nio.ByteBuffer

actual inline fun glBufferStorage(buffer: Int, size: Long, flags: Int) =
    GL44C.glBufferStorage(buffer, size, flags)

actual inline fun glNamedBufferSubData(
    buffer: Int,
    offset: Long,
    data: ByteBuffer
) = GL45C.glNamedBufferSubData(buffer, offset, data)

actual inline fun glNamedBufferSubData(
    buffer: Int,
    offset: Long,
    data: IntArray
) = GL45C.glNamedBufferSubData(buffer, offset, data)

actual inline fun glBufferSubData(buffer: Int, offset: Long, data: IntArray) =
    GL15C.glBufferSubData(buffer, offset, data)

actual inline fun glGetBufferSubData(
    buffer: Int,
    offset: Long,
    result: IntArray
) = GL15C.glGetBufferSubData(buffer, offset, result)

actual inline fun glGetBufferSubData(
    buffer: Int,
    offset: Long,
    result: ByteBuffer
) = GL15C.glGetBufferSubData(buffer, offset, result)

actual inline fun glGetNamedBufferSubData(
    buffer: Int,
    size: Long,
    result: IntArray
) = GL45C.glGetNamedBufferSubData(buffer, size, result)

actual inline fun glGetNamedBufferSubData(
    buffer: Int,
    size: Long,
    result: ByteBuffer
) = GL45C.glGetNamedBufferSubData(buffer, size, result)

actual inline fun glGenerateTextureMipmap(texture: Int) = GL45C.glGenerateTextureMipmap(texture)

actual inline fun glGetTexImage(
    target: Int,
    level: Int,
    internalType: Int,
    targetType: Int,
    pixels: ByteBuffer
) = GL42C.glGetTexImage(target, level, internalType, targetType, pixels)

actual inline fun glGetCompressedTexImage(
    target: Int,
    level: Int,
    pixels: ByteBuffer
) = GL42C.glGetCompressedTexImage(target, level, pixels)

actual inline fun glClearTexImage(
    texture: Int,
    level: Int,
    format: Int,
    type: Int,
    data: FloatArray
) {
    GL44C.glClearTexImage(texture, level, format, type, data)
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
    GL44C.glClearTexSubImage(
        texture,
        level,
        xoffset,
        yoffset,
        zoffset,
        width,
        height,
        depth,
        format,
        type,
        data
    )
}

actual inline fun glTexBuffer(target: Int, internalFormat: Int, buffer: Int) =
    GL31C.glTexBuffer(target, internalFormat, buffer)

actual inline fun glDrawArraysInstancedBaseInstance(
    primitive: Int,
    vertexOffset: Int,
    vertexCount: Int,
    instanceCount: Int,
    instanceOffset: Int
) = GL42C.glDrawArraysInstancedBaseInstance(
    primitive,
    vertexOffset,
    vertexCount,
    instanceCount,
    instanceOffset
)

actual inline fun glDrawElementsInstancedBaseInstance(
    mode: Int,
    count: Int,
    type: Int,
    indices: Long,
    primcount: Int,
    baseinstance: Int
) = GL45C.glDrawElementsInstancedBaseInstance(mode, count, type, indices, primcount, baseinstance)

actual inline fun glGetActiveAtomicCounterBufferi(
    program: Int,
    bufferIndex: Int,
    pname: Int
): Int = GL42C.glGetActiveAtomicCounterBufferi(program, bufferIndex, pname)

actual inline fun glTextureStorage3D(
    texture: Int,
    levels: Int,
    internalFormat: Int,
    width: Int,
    height: Int,
    depth: Int
) {
    GL45C.glTextureStorage3D(texture, levels, internalFormat, width, height, depth)
}

actual inline fun glGetTextureImage(
    texture: Int,
    level: Int,
    format: Int,
    type: Int,
    pixels: ByteBuffer
) = GL45C.glGetTextureImage(texture, level, format, type, pixels)

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
) = GL45C.glGetTextureSubImage(texture, level, xoffset, yoffset, zoffset, width, height, depth, format, type, pixels)


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
) = GL45C.glTextureSubImage3D(texture, level, xoffset, yoffset, zoffset, width, height, depth, format, type, pixels)

actual inline fun glClearNamedBufferData(
    buffer: Int,
    internalformat: Int,
    format: Int,
    type: Int,
    data: ByteBuffer
) = GL45C.glClearNamedBufferData(buffer, internalformat, format, type, data)

actual inline fun glClearNamedBufferData(
    buffer: Int,
    internalformat: Int,
    format: Int,
    type: Int,
    data: IntArray
) = GL45C.glClearNamedBufferData(buffer, internalformat, format, type, data)

actual inline fun glClearBufferData(
    target: Int,
    internalformat: Int,
    format: Int,
    type: Int,
    data: IntArray
) = GL45C.glClearBufferData(target, internalformat, format, type, data)