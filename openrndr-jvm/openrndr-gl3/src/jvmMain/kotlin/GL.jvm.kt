@file:Suppress("NOTHING_TO_INLINE")

package org.openrndr.internal.gl3

import org.lwjgl.opengl.*
import org.lwjgl.opengles.GLES32
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Desktop GL entry points that have no OpenGL ES counterpart. Calling a GLxxC function while running
 * on GLES initializes LWJGL's desktop GL capabilities, which crashes with an obscure
 * "setFunctionMissingAddresses has been called already"; fail with a readable error instead.
 */
inline fun notSupportedOnGLES(function: String): Nothing {
    error("$function is not supported on OpenGL ES")
}

actual inline fun glBufferStorage(buffer: Int, size: Long, flags: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL44C.glBufferStorage(buffer, size, flags)
        DriverTypeGL.GLES -> notSupportedOnGLES("glBufferStorage")
    }
}

actual inline fun glNamedBufferSubData(
    buffer: Int,
    offset: Long,
    data: ByteBuffer
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL45C.glNamedBufferSubData(buffer, offset, data)
        DriverTypeGL.GLES -> notSupportedOnGLES("glNamedBufferSubData")
    }
}

actual inline fun glNamedBufferSubData(
    buffer: Int,
    offset: Long,
    data: IntArray
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL45C.glNamedBufferSubData(buffer, offset, data)
        DriverTypeGL.GLES -> notSupportedOnGLES("glNamedBufferSubData")
    }
}

actual inline fun glBufferSubData(buffer: Int, offset: Long, data: IntArray) {
    return when (driverType) {
        DriverTypeGL.GL -> GL15C.glBufferSubData(buffer, offset, data)
        DriverTypeGL.GLES -> GLES32.glBufferSubData(buffer, offset, data)
    }
}

actual inline fun glGetBufferSubData(
    buffer: Int,
    offset: Long,
    result: IntArray
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL15C.glGetBufferSubData(buffer, offset, result)
        DriverTypeGL.GLES -> glesGetBufferSubData(buffer, offset, result)
    }
}

actual inline fun glGetBufferSubData(
    buffer: Int,
    offset: Long,
    result: ByteBuffer
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL15C.glGetBufferSubData(buffer, offset, result)
        DriverTypeGL.GLES -> glesGetBufferSubData(buffer, offset, result)
    }
}

actual inline fun glGetNamedBufferSubData(
    buffer: Int,
    size: Long,
    result: IntArray
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL45C.glGetNamedBufferSubData(buffer, size, result)
        DriverTypeGL.GLES -> notSupportedOnGLES("glGetNamedBufferSubData")
    }
}

actual inline fun glGetNamedBufferSubData(
    buffer: Int,
    size: Long,
    result: ByteBuffer
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL45C.glGetNamedBufferSubData(buffer, size, result)
        DriverTypeGL.GLES -> notSupportedOnGLES("glGetNamedBufferSubData")
    }
}

actual inline fun glGenerateTextureMipmap(texture: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL45C.glGenerateTextureMipmap(texture)
        DriverTypeGL.GLES -> notSupportedOnGLES("glGenerateTextureMipmap")
    }
}

actual inline fun glGetTexImage(
    target: Int,
    level: Int,
    internalType: Int,
    targetType: Int,
    pixels: ByteBuffer
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL42C.glGetTexImage(target, level, internalType, targetType, pixels)
        DriverTypeGL.GLES -> notSupportedOnGLES("glGetTexImage")
    }
}

actual inline fun glGetCompressedTexImage(
    target: Int,
    level: Int,
    pixels: ByteBuffer
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL42C.glGetCompressedTexImage(target, level, pixels)
        DriverTypeGL.GLES -> notSupportedOnGLES("glGetCompressedTexImage")
    }
}

actual inline fun glClearTexImage(
    texture: Int,
    level: Int,
    format: Int,
    type: Int,
    data: FloatArray
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL44C.glClearTexImage(texture, level, format, type, data)
        DriverTypeGL.GLES -> notSupportedOnGLES("glClearTexImage")
    }
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
    return when (driverType) {
        DriverTypeGL.GL -> GL44C.glClearTexSubImage(texture, level, xoffset, yoffset, zoffset, width, height, depth, format, type, data)
        DriverTypeGL.GLES -> notSupportedOnGLES("glClearTexSubImage")
    }
}

actual inline fun glTexBuffer(target: Int, internalFormat: Int, buffer: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL31C.glTexBuffer(target, internalFormat, buffer)
        DriverTypeGL.GLES -> GLES32.glTexBuffer(target, internalFormat, buffer)
    }
}

actual inline fun glDrawArraysInstancedBaseInstance(
    primitive: Int,
    vertexOffset: Int,
    vertexCount: Int,
    instanceCount: Int,
    instanceOffset: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL42C.glDrawArraysInstancedBaseInstance(primitive, vertexOffset, vertexCount, instanceCount, instanceOffset)
        DriverTypeGL.GLES -> notSupportedOnGLES("glDrawArraysInstancedBaseInstance")
    }
}

actual inline fun glDrawElementsInstancedBaseInstance(
    mode: Int,
    count: Int,
    type: Int,
    indices: Long,
    primcount: Int,
    baseinstance: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL45C.glDrawElementsInstancedBaseInstance(mode, count, type, indices, primcount, baseinstance)
        DriverTypeGL.GLES -> notSupportedOnGLES("glDrawElementsInstancedBaseInstance")
    }
}

actual inline fun glGetActiveAtomicCounterBufferi(
    program: Int,
    bufferIndex: Int,
    pname: Int
): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL42C.glGetActiveAtomicCounterBufferi(program, bufferIndex, pname)
        DriverTypeGL.GLES -> glesGetActiveAtomicCounterBufferi(program, bufferIndex, pname)
    }
}

actual inline fun glTextureStorage3D(
    texture: Int,
    levels: Int,
    internalFormat: Int,
    width: Int,
    height: Int,
    depth: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL45C.glTextureStorage3D(texture, levels, internalFormat, width, height, depth)
        DriverTypeGL.GLES -> notSupportedOnGLES("glTextureStorage3D")
    }
}

actual inline fun glGetTextureImage(
    texture: Int,
    level: Int,
    format: Int,
    type: Int,
    pixels: ByteBuffer
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL45C.glGetTextureImage(texture, level, format, type, pixels)
        DriverTypeGL.GLES -> notSupportedOnGLES("glGetTextureImage")
    }
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
    return when (driverType) {
        DriverTypeGL.GL -> GL45C.glGetTextureSubImage(texture, level, xoffset, yoffset, zoffset, width, height, depth, format, type, pixels)
        DriverTypeGL.GLES -> notSupportedOnGLES("glGetTextureSubImage")
    }
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
    return when (driverType) {
        DriverTypeGL.GL -> GL45C.glTextureSubImage3D(texture, level, xoffset, yoffset, zoffset, width, height, depth, format, type, pixels)
        DriverTypeGL.GLES -> notSupportedOnGLES("glTextureSubImage3D")
    }
}

actual inline fun glClearNamedBufferData(
    buffer: Int,
    internalformat: Int,
    format: Int,
    type: Int,
    data: ByteBuffer
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL45C.glClearNamedBufferData(buffer, internalformat, format, type, data)
        DriverTypeGL.GLES -> notSupportedOnGLES("glClearNamedBufferData")
    }
}

actual inline fun glClearNamedBufferData(
    buffer: Int,
    internalformat: Int,
    format: Int,
    type: Int,
    data: IntArray
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL45C.glClearNamedBufferData(buffer, internalformat, format, type, data)
        DriverTypeGL.GLES -> notSupportedOnGLES("glClearNamedBufferData")
    }
}

actual inline fun glClearBufferData(
    target: Int,
    internalformat: Int,
    format: Int,
    type: Int,
    data: IntArray
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL45C.glClearBufferData(target, internalformat, format, type, data)
        DriverTypeGL.GLES -> notSupportedOnGLES("glClearBufferData")
    }
}

/**
 * GLES has no glGetBufferSubData; read the range through a mapping of the buffer bound to [target].
 * Like glGetBufferSubData, this writes at [result]'s position without moving it.
 */
@PublishedApi
internal fun glesGetBufferSubData(target: Int, offset: Long, result: ByteBuffer) {
    val mapped = GLES32.glMapBufferRange(target, offset, result.remaining().toLong(), GLES32.GL_MAP_READ_BIT)
        ?: error("glMapBufferRange failed")
    val position = result.position()
    result.put(mapped)
    result.position(position)
    GLES32.glUnmapBuffer(target)
}

@PublishedApi
internal fun glesGetBufferSubData(target: Int, offset: Long, result: IntArray) {
    val mapped = GLES32.glMapBufferRange(target, offset, result.size * 4L, GLES32.GL_MAP_READ_BIT)
        ?: error("glMapBufferRange failed")
    mapped.order(ByteOrder.nativeOrder()).asIntBuffer().get(result)
    GLES32.glUnmapBuffer(target)
}

/**
 * GLES has no glGetActiveAtomicCounterBufferiv; the binding point is available through the program
 * interface query instead.
 */
@PublishedApi
internal fun glesGetActiveAtomicCounterBufferi(program: Int, bufferIndex: Int, pname: Int): Int {
    if (pname != GLES32.GL_ATOMIC_COUNTER_BUFFER_BINDING) {
        notSupportedOnGLES("glGetActiveAtomicCounterBufferi")
    }
    val result = IntArray(1)
    GLES32.glGetProgramResourceiv(
        program,
        GLES32.GL_ATOMIC_COUNTER_BUFFER,
        bufferIndex,
        intArrayOf(GLES32.GL_BUFFER_BINDING),
        null,
        result
    )
    return result[0]
}
