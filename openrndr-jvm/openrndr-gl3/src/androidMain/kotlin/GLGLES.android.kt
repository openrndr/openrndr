@file:Suppress("NOTHING_TO_INLINE")

package org.openrndr.internal.gl3

import android.annotation.TargetApi
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLES31
import android.opengl.GLES31Ext
import android.os.Build
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import android.opengl.GLES32 as GLES


actual inline fun glEnable(target: Int) = GLES.glEnable(target)
actual inline fun glIsEnabled(cap: Int): Boolean = GLES.glIsEnabled(cap)
actual inline fun glDisable(target: Int) = GLES.glDisable(target)

//actual inline fun glGenBuffers(): Int {
//
//}

actual inline fun glBindBuffer(target: Int, buffer: Int) = GLES.glBindBuffer(target, buffer)


actual inline fun glGetIntegerv(pname: Int, params: IntArray) {
    // I assume 0 is an offset into params
    GLES.glGetIntegerv(pname, params, 0)
}

actual inline fun glGenBuffers(): Int {
    val buffers = IntArray(1)
    GLES.glGenBuffers(1, buffers, 0)
    return buffers[0]
}

actual inline fun glBufferData(target: Int, size: Long, usage: Int) {
    GLES.glBufferData(target, size.toInt(), null, usage)
}

actual inline fun glBufferData(target: Int, data: ByteBuffer, usage: Int) {
    GLES.glBufferData(target, data.remaining(), data, usage)

}

actual inline fun glBufferData(target: Int, data: IntArray, usage: Int) {
    val buffer = BufferUtils.createIntBuffer(data.size)
    buffer.put(data)
    buffer.flip()
    GLES.glBufferData(target, buffer.remaining(), buffer, usage)
}

actual inline fun glGetInteger(pname: Int): Int {
    val result = IntArray(1)
    GLES.glGetIntegerv(pname, result, 0)
    return result[0]
}

actual inline fun glGetString(pname: Int): String? {
    return GLES.glGetString(pname)
}

actual inline fun glBindFramebuffer(target: Int, framebuffer: Int) {
    GLES.glBindFramebuffer(target, framebuffer)
}

actual inline fun glScissor(x: Int, y: Int, width: Int, height: Int) {
    GLES.glScissor(x, y, width, height)
}

actual inline fun glViewport(x: Int, y: Int, w: Int, h: Int) {
    GLES.glViewport(x, y, w, h)
}

actual inline fun glDepthMask(flag: Boolean) {
    GLES.glDepthMask(flag)
}

actual inline fun glClearColor(
    red: Float,
    green: Float,
    blue: Float,
    alpha: Float
) {
    GLES.glClearColor(red, green, blue, alpha)
}

actual inline fun glClear(mask: Int): Unit {
    GLES.glClear(mask)
}

actual inline fun glGenFramebuffers(): Int {
    val result = IntArray(1)
    GLES.glGenFramebuffers(1, result, 0)
    return result[0]
}

actual inline fun glDrawBuffers(bufs: IntArray) {
}

actual inline fun glBufferSubData(
    target: Int,
    offset: Long,
    data: ByteBuffer
) {
    GLES.glBufferSubData(target, offset.toInt(), data.remaining(), data)
}

actual inline fun glFramebufferTexture(
    target: Int,
    attachment: Int,
    texture: Int,
    level: Int
) = GLES.glFramebufferTexture(target, attachment, texture, level)

actual inline fun glBlendEquationi(buf: Int, mode: Int) = GLES.glBlendEquationi(buf, mode)

actual inline fun glBlendFunci(buf: Int, sfactor: Int, dfactor: Int) =
    GLES.glBlendFunci(buf, sfactor, dfactor)

actual inline fun glBindVertexArray(array: Int) = GLES.glBindVertexArray(array)
actual inline fun glClearDepth(depth: Double) {
    GLES.glClearDepthf(depth.toFloat())
}

actual inline fun glCreateShader(type: Int): Int = GLES.glCreateShader(type)
actual inline fun glShaderSource(shader: Int, string: CharSequence) =
    GLES.glShaderSource(shader, string.toString())

actual inline fun glCompileShader(shader: Int) = GLES.glCompileShader(shader)
actual inline fun glGetShaderiv(shader: Int, pname: Int, params: IntArray) =
    GLES.glGetShaderiv(shader, pname, params, 0)

actual inline fun glGetProgramiv(program: Int, pname: Int, params: IntArray) =
    GLES.glGetProgramiv(program, pname, params, 0)

actual inline fun glGetShaderInfoLog(
    shader: Int,
    length: IntArray?,
    infoLog: ByteBuffer
) {
    val result = GLES.glGetShaderInfoLog(shader)
    infoLog.put(result.toByteArray())
}

actual inline fun glGetProgramInfoLog(
    program: Int,
    length: IntArray?,
    infoLog: ByteBuffer
) {
    val result = GLES.glGetProgramInfoLog(program)
    infoLog.put(result.toByteArray())
}

actual inline fun glCreateProgram(): Int = GLES.glCreateProgram()
actual inline fun glAttachShader(program: Int, shader: Int) = GLES.glAttachShader(program, shader)

actual inline fun glLinkProgram(program: Int) = GLES.glLinkProgram(program)

actual inline fun glFinish() {
    GLES.glFinish()
}

actual inline fun glUseProgram(program: Int) = GLES.glUseProgram(program)

actual inline fun glGetUniformBlockIndex(
    program: Int,
    uniformBlockName: CharSequence
): Int = GLES.glGetUniformBlockIndex(program, uniformBlockName.toString())

actual inline fun glGetAttribLocation(program: Int, name: CharSequence): Int =
    GLES.glGetAttribLocation(program, name.toString())

actual inline fun glDeleteProgram(program: Int) = GLES.glDeleteProgram(program)

actual inline fun glUniformBlockBinding(
    program: Int,
    uniformBlockIndex: Int,
    uniformBlockBinding: Int
) = GLES.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding)

actual inline fun glGetActiveUniformBlockiv(
    program: Int,
    uniformBlockIndex: Int,
    pname: Int,
    params: IntBuffer
) {
    GLES.glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, params)
}

actual inline fun glGetActiveUniformsiv(
    program: Int,
    uniformIndices: IntBuffer,
    pname: Int,
    params: IntBuffer
) {
    GLES.glGetActiveUniformsiv(program, uniformIndices.remaining(), uniformIndices, pname, params)
}

actual inline fun glGetActiveUniformsiv(
    program: Int,
    uniformIndices: IntArray,
    pname: Int,
    params: IntArray
) = GLES.glGetActiveUniformsiv(program, uniformIndices.size, uniformIndices, 0, pname, params, 0)

actual inline fun glGetActiveUniformName(
    program: Int,
    uniformIndex: Int,
    bufSize: Int
): String {
    val length = IntArray(1) { 1024 }
    val type = IntArray(1)
    val size = IntArray(1)
    val name = GLES.glGetActiveUniform(program, uniformIndex, size, 0, type, 0)
    return name
}

actual inline fun glGetError(): Int = GLES.glGetError()
actual inline fun glBindBufferBase(target: Int, index: Int, buffer: Int) =
    GLES.glBindBufferBase(target, index, buffer)

actual inline fun glColorMask(
    red: Boolean,
    green: Boolean,
    blue: Boolean,
    alpha: Boolean
) {
    GLES.glColorMask(red, green, blue, alpha)
}

actual inline fun glBlendEquation(mode: Int) = GLES20.glBlendEquation(mode)
actual inline fun glBlendFunc(sfactor: Int, dfactor: Int) {
    GLES20.glBlendFunc(sfactor, dfactor)
}

actual inline fun glBlendEquationSeparatei(
    buf: Int,
    modeRGB: Int,
    modeAlpha: Int
) = GLES.glBlendEquationSeparatei(buf, modeRGB, modeAlpha)

actual inline fun glBlendEquationSeparate(modeRGB: Int, modeAlpha: Int) =
    GLES20.glBlendEquationSeparate(modeRGB, modeAlpha)

actual inline fun glBlendFuncSeparate(
    sfactorRGB: Int,
    dfactorRGB: Int,
    sfactorAlpha: Int,
    dfactorAlpha: Int
) = GLES20.glBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha)

actual inline fun glBlendFuncSeparatei(
    buf: Int,
    sfactorRGB: Int,
    dfactorRGB: Int,
    sfactorAlpha: Int,
    dfactorAlpha: Int
) {
    GLES.glBlendFuncSeparatei(buf, sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha)
}

actual inline fun glDepthFunc(func: Int) = GLES20.glDepthFunc(func)
actual inline fun glCullFace(mode: Int) = GLES20.glCullFace(mode)

actual inline fun glGenVertexArrays(arrays: IntArray) =
    GLES.glGenVertexArrays(arrays.size, arrays, 0)

actual inline fun glDrawArraysInstanced(
    mode: Int,
    first: Int,
    count: Int,
    primcount: Int
) = GLES30.glDrawArraysInstanced(mode, first, count, primcount)

actual inline fun glDrawElements(
    mode: Int,
    count: Int,
    type: Int,
    indices: Long
) = GLES30.glDrawElements(mode, count, type, indices.toInt())

actual inline fun glDrawElementsInstanced(
    mode: Int,
    count: Int,
    type: Int,
    indices: Long,
    primcount: Int
) = GLES30.glDrawElementsInstanced(mode, count, type, indices.toInt(), primcount)

actual inline fun glEnableVertexAttribArray(index: Int) {
    GLES30.glEnableVertexAttribArray(index)
}

actual inline fun glVertexAttribPointer(
    index: Int,
    size: Int,
    type: Int,
    normalized: Boolean,
    stride: Int,
    pointer: Long
) = GLES20.glVertexAttribPointer(index, size, type, normalized, stride, pointer.toInt())

actual inline fun glVertexAttribIPointer(
    index: Int,
    size: Int,
    type: Int,
    stride: Int,
    pointer: Long
) = GLES30.glVertexAttribIPointer(index, size, type, stride, pointer.toInt())

actual inline fun glVertexAttribDivisor(index: Int, divisor: Int) =
    GLES30.glVertexAttribDivisor(index, divisor)

actual inline fun glDeleteBuffers(buffer: Int) {
    GLES20.glDeleteBuffers(1, intArrayOf(buffer), 0)
}

actual inline fun glDeleteVertexArrays(array: Int) =
    GLES.glDeleteVertexArrays(1, intArrayOf(array), 0)

actual inline fun glGetUniformLocation(program: Int, name: CharSequence): Int =
    GLES20.glGetUniformLocation(program, name.toString())

actual inline fun glProgramUniform4f(
    program: Int,
    location: Int,
    x: Float,
    y: Float,
    z: Float,
    w: Float
) = GLES31.glProgramUniform4f(program, location, x, y, z, w)

actual inline fun glUniform4f(
    location: Int,
    v0: Float,
    v1: Float,
    v2: Float,
    v3: Float
) = GLES20.glUniform4f(location, v0, v1, v2, v3)

actual inline fun glProgramUniform3f(
    program: Int,
    location: Int,
    x: Float,
    y: Float,
    z: Float
) = GLES31.glProgramUniform3f(program, location, x, y, z)

actual inline fun glUniform3f(
    location: Int,
    v0: Float,
    v1: Float,
    v2: Float
) = GLES20.glUniform3f(location, v0, v1, v2)

actual inline fun glProgramUniform2f(
    program: Int,
    location: Int,
    x: Float,
    y: Float
) = GLES31.glProgramUniform2f(program, location, x, y)

actual inline fun glUniform2f(location: Int, v0: Float, v1: Float) =
    GLES20.glUniform2f(location, v0, v1)

actual inline fun glProgramUniform1i(program: Int, location: Int, x: Int) =
    GLES31.glProgramUniform1i(program, location, x)

actual inline fun glUniform1i(location: Int, v0: Int) = GLES31.glUniform1i(location, v0)

actual inline fun glProgramUniform1f(program: Int, location: Int, x: Float) =
    GLES31.glProgramUniform1f(program, location, x)

actual inline fun glUniform1f(location: Int, v0: Float) = GLES20.glUniform1f(location, v0)

actual inline fun glUniform2i(location: Int, v0: Int, v1: Int) =
    GLES20.glUniform2i(location, v0, v1)

actual inline fun glUniform3i(
    location: Int,
    v0: Int,
    v1: Int,
    v2: Int
) = GLES20.glUniform3i(location, v0, v1, v2)

actual inline fun glUniform4i(
    location: Int,
    v0: Int,
    v1: Int,
    v2: Int,
    v3: Int
) = GLES20.glUniform4i(location, v0, v1, v2, v3)

actual inline fun glProgramUniform2i(
    program: Int,
    location: Int,
    x: Int,
    y: Int
) = GLES31.glProgramUniform2i(program, location, x, y)

actual inline fun glProgramUniform3i(
    program: Int,
    location: Int,
    x: Int,
    y: Int,
    z: Int
) = GLES31.glProgramUniform3i(program, location, x, y, z)

actual inline fun glProgramUniform4i(
    program: Int,
    location: Int,
    x: Int,
    y: Int,
    z: Int,
    w: Int
) = GLES31.glProgramUniform4i(program, location, x, y, z, w)

actual inline fun glProgramUniformMatrix3fv(
    program: Int,
    location: Int,
    transpose: Boolean,
    value: FloatArray
) = GLES31.glProgramUniformMatrix3fv(program, location, value.size / 9, transpose, value, 0)

actual inline fun glProgramUniformMatrix3fv(
    program: Int,
    location: Int,
    transpose: Boolean,
    value: FloatBuffer
) = GLES31.glProgramUniformMatrix3fv(program, location, value.remaining() / 9, transpose, value)

actual inline fun glUniformMatrix3fv(
    location: Int,
    transpose: Boolean,
    value: FloatArray
) = GLES20.glUniformMatrix3fv(location, value.size / 9, transpose, value, 0)

actual inline fun glProgramUniformMatrix4fv(
    program: Int,
    location: Int,
    transpose: Boolean,
    value: FloatArray
) = GLES31.glProgramUniformMatrix4fv(program, location, value.size / 16, transpose, value, 0)

actual inline fun glUniformMatrix4fv(
    location: Int,
    transpose: Boolean,
    value: FloatArray
) = GLES20.glUniformMatrix4fv(location, value.size / 16, transpose, value, 0)

actual inline fun glProgramUniform4iv(
    program: Int,
    location: Int,
    value: IntArray
) = GLES31.glProgramUniform4iv(program, location, value.size / 4, value, 0)

actual inline fun glUniform4iv(location: Int, value: IntArray) =
    GLES20.glUniform4iv(location, value.size / 4, value, 0)

actual inline fun glProgramUniform3iv(
    program: Int,
    location: Int,
    value: IntArray
) = GLES31.glProgramUniform3iv(program, location, value.size / 3, value, 0)

actual inline fun glUniform3iv(location: Int, value: IntArray) =
    GLES20.glUniform3iv(location, value.size / 3, value, 0)

actual inline fun glProgramUniform2iv(
    program: Int,
    location: Int,
    value: IntArray
) = GLES31.glProgramUniform2iv(program, location, value.size / 2, value, 0)

actual inline fun glUniform2iv(location: Int, value: IntArray) =
    GLES20.glUniform2iv(location, value.size / 2, value, 0)

actual inline fun glProgramUniform2fv(
    program: Int,
    location: Int,
    value: FloatArray
) = GLES31.glProgramUniform2fv(program, location, value.size / 2, value, 0)

actual inline fun glUniform2fv(location: Int, value: FloatArray) =
    GLES20.glUniform2fv(location, value.size / 2, value, 0)

actual inline fun glProgramUniform3fv(
    program: Int,
    location: Int,
    value: FloatArray
) = GLES31.glProgramUniform3fv(program, location, value.size / 3, value, 0)

actual inline fun glUniform3fv(location: Int, value: FloatArray) =
    GLES20.glUniform2fv(location, value.size / 3, value, 0)

actual inline fun glProgramUniform4fv(
    program: Int,
    location: Int,
    value: FloatArray
) = GLES31.glProgramUniform4fv(program, location, value.size / 4, value, 0)

actual inline fun glUniform4fv(location: Int, value: FloatArray) =
    GLES20.glUniform4fv(location, value.size / 4, value, 0)

actual inline fun glProgramUniform1fv(
    program: Int,
    location: Int,
    value: FloatArray
) = GLES31.glProgramUniform1fv(program, location, value.size, value, 0)

actual inline fun glUniform1fv(location: Int, value: FloatArray) =
    GLES20.glUniform1fv(location, value.size, value, 0)

actual inline fun glProgramUniform1iv(
    program: Int,
    location: Int,
    value: IntArray
) = GLES31.glProgramUniform1iv(program, location, value.size, value, 0)

actual inline fun glUniform1iv(location: Int, value: IntArray) =
    GLES20.glUniform1iv(location, value.size, value, 0)

actual inline fun glProgramUniformMatrix4fv(
    program: Int,
    location: Int,
    transpose: Boolean,
    value: FloatBuffer
) = GLES31.glProgramUniformMatrix4fv(program, location, value.remaining() / 16, transpose, value)

actual inline fun glUniformMatrix3fv(
    location: Int,
    transpose: Boolean,
    value: FloatBuffer
) = GLES20.glUniformMatrix3fv(location, value.remaining() / 9, transpose, value)

actual inline fun glUniformMatrix4fv(
    location: Int,
    transpose: Boolean,
    value: FloatBuffer
) = GLES20.glUniformMatrix4fv(location, value.remaining() / 16, transpose, value)

actual inline fun glGetActiveUniform(
    program: Int,
    index: Int,
    length: IntArray,
    size: IntArray,
    type: IntArray,
): String {
    return GLES.glGetActiveUniform(program, index, size, 0, type, 0)
}

actual inline fun glTexParameteri(target: Int, pname: Int, param: Int) =
    GLES20.glTexParameteri(target, pname, param)

actual inline fun glGenTextures(): Int {
    val result = IntArray(1)
    GLES20.glGenTextures(1, result, 0)
    return result[0]
}

actual inline fun glBindTexture(target: Int, texture: Int) = GLES20.glBindTexture(target, texture)
actual inline fun glTexImage3D(
    target: Int,
    level: Int,
    internalformat: Int,
    width: Int,
    height: Int,
    depth: Int,
    border: Int,
    format: Int,
    type: Int,
    pixels: ByteBuffer?
) = GLES30.glTexImage3D(
    target,
    level,
    internalformat,
    width,
    height,
    depth,
    border,
    format,
    type,
    pixels
)

actual inline fun glActiveTexture(texture: Int) = GLES20.glActiveTexture(texture)

actual inline fun glDeleteTextures(texture: Int) =
    GLES20.glDeleteTextures(1, intArrayOf(texture), 0)

actual inline fun glReadBuffer(src: Int) = GLES30.glReadBuffer(src)

actual inline fun glGenerateMipmap(target: Int) = GLES20.glGenerateMipmap(target)

actual inline fun glTexSubImage3D(
    target: Int,
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
) = GLES30.glTexSubImage3D(
    target,
    level,
    xoffset,
    yoffset,
    zoffset,
    width,
    height,
    depth,
    format,
    type,
    pixels
)

actual inline fun glCopyTexSubImage2D(
    target: Int,
    level: Int,
    xoffset: Int,
    yoffset: Int,
    x: Int,
    y: Int,
    width: Int,
    height: Int
) = GLES30.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height)

actual inline fun glCompressedTexSubImage3D(
    target: Int,
    level: Int,
    xoffset: Int,
    yoffset: Int,
    zoffset: Int,
    width: Int,
    height: Int,
    depth: Int,
    format: Int,
    data: ByteBuffer
) {
    GLES30.glCompressedTexSubImage3D(
        target,
        level,
        xoffset,
        yoffset,
        zoffset,
        width,
        height,
        depth,
        format,
        data.remaining(),
        data
    )
}

actual inline fun glCopyTexSubImage3D(
    target: Int,
    level: Int,
    xoffset: Int,
    yoffset: Int,
    zoffset: Int,
    x: Int,
    y: Int,
    width: Int,
    height: Int
) = GLES30.glCopyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height)

actual inline fun glTexImage2D(
    target: Int,
    level: Int,
    internalformat: Int,
    width: Int,
    height: Int,
    border: Int,
    format: Int,
    type: Int,
    pixels: ByteBuffer?
) = GLES30.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels)

actual inline fun glBlitFramebuffer(
    srcX0: Int,
    srcY0: Int,
    srcX1: Int,
    srcY1: Int,
    dstX0: Int,
    dstY0: Int,
    dstX1: Int,
    dstY1: Int,
    mask: Int,
    filter: Int
) = GLES30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter)

actual inline fun glCopyImageSubData(
    srcName: Int,
    srcTarget: Int,
    srcLevel: Int,
    srcX: Int,
    srcY: Int,
    srcZ: Int,
    dstName: Int,
    dstTarget: Int,
    dstLevel: Int,
    dstX: Int,
    dstY: Int,
    dstZ: Int,
    srcWidth: Int,
    srcHeight: Int,
    srcDepth: Int
) {
    GLES.glCopyImageSubData(
        srcName, srcTarget, srcLevel, srcX, srcY, srcZ, dstName,
        dstTarget, dstLevel, dstX, dstY, dstZ, srcWidth, srcHeight, srcDepth
    )
}

actual inline fun glTexParameterf(target: Int, pname: Int, param: Float) =
    GLES20.glTexParameterf(target, pname, param)

actual inline fun glPixelStorei(pname: Int, param: Int) = GLES20.glPixelStorei(pname, param)

actual inline fun glTexSubImage2D(
    target: Int,
    level: Int,
    xoffset: Int,
    yoffset: Int,
    width: Int,
    height: Int,
    format: Int,
    type: Int,
    pixels: ByteBuffer
) = GLES20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels)

actual inline fun glCompressedTexSubImage2D(
    target: Int,
    level: Int,
    xoffset: Int,
    yoffset: Int,
    width: Int,
    height: Int,
    format: Int,
    data: ByteBuffer
) {
    GLES20.glCompressedTexSubImage2D(
        target,
        level,
        xoffset,
        yoffset,
        width,
        height,
        format,
        data.remaining(),
        data
    )
}

actual inline fun glFramebufferTexture2D(
    target: Int,
    attachment: Int,
    textarget: Int,
    texture: Int,
    level: Int
) = GLES20.glFramebufferTexture2D(target, attachment, textarget, texture, level)

actual inline fun glFramebufferTextureLayer(
    target: Int,
    attachment: Int,
    texture: Int,
    level: Int,
    layer: Int
) = GLES30.glFramebufferTextureLayer(target, attachment, texture, level, layer)

actual inline fun glGetBoolean(pname: Int): Boolean {
    val result = BooleanArray(1)
    GLES20.glGetBooleanv(pname, result, 0)
    return result[0]
}

actual inline fun glGetTexLevelParameteri(
    target: Int,
    level: Int,
    pname: Int
): Int {
    val result = IntArray(1)
    GLES.glGetTexLevelParameteriv(target, level, pname, result, 0)
    return result[0]
}

actual inline fun glGetTexParameteri(target: Int, pname: Int): Int {
    val result = IntArray(1)
    GLES20.glGetTexParameteriv(target, pname, result, 0)
    return result[0]
}

actual inline fun glDeleteFramebuffers(framebuffer: Int) =
    GLES20.glDeleteFramebuffers(1, intArrayOf(framebuffer), 0)

actual inline fun glStencilFuncSeparate(
    face: Int,
    func: Int,
    ref: Int,
    mask: Int
) = GLES20.glStencilFuncSeparate(face, func, ref, mask)

actual inline fun glStencilOpSeparate(
    face: Int,
    sfail: Int,
    dpfail: Int,
    dppass: Int
) = GLES20.glStencilOpSeparate(face, sfail, dpfail, dppass)

actual inline fun glStencilMaskSeparate(face: Int, mask: Int) =
    GLES20.glStencilMaskSeparate(face, mask)

actual inline fun glDrawArrays(mode: Int, first: Int, count: Int) =
    GLES20.glDrawArrays(mode, first, count)

actual inline fun glCheckFramebufferStatus(target: Int): Int =
    GLES20.glCheckFramebufferStatus(target)

actual inline fun glClearBufferfi(
    buffer: Int,
    drawbuffer: Int,
    depth: Float,
    stencil: Int
) = GLES30.glClearBufferfi(buffer, drawbuffer, depth, stencil)

actual inline fun glClearBufferfv(
    buffer: Int,
    drawbuffer: Int,
    value: FloatBuffer
) = GLES30.glClearBufferfv(buffer, drawbuffer, value)

actual inline fun glClearBufferiv(
    buffer: Int,
    drawbuffer: Int,
    value: IntBuffer
) = GLES30.glClearBufferiv(buffer, drawbuffer, value)

actual inline fun glTexImage2DMultisample(
    target: Int,
    samples: Int,
    internalformat: Int,
    width: Int,
    height: Int,
    fixedsamplelocations: Boolean
) {
    notSupportedOnAndroid()
}

actual inline fun glTexStorage2D(
    target: Int,
    levels: Int,
    internalformat: Int,
    width: Int,
    height: Int
) = GLES30.glTexStorage2D(target, levels, internalformat, width, height)

actual inline fun glTexStorage2DMultisample(
    target: Int,
    samples: Int,
    internalformat: Int,
    width: Int,
    height: Int,
    fixedsamplelocations: Boolean
) = GLES.glTexStorage2DMultisample(
    target,
    samples,
    internalformat,
    width,
    height,
    fixedsamplelocations
)

actual inline fun glTexStorage3D(
    target: Int,
    levels: Int,
    internalformat: Int,
    width: Int,
    height: Int,
    depth: Int
) {
    GLES30.glTexStorage3D(target, levels, internalformat, width, height, depth)
}

actual inline fun glFramebufferRenderbuffer(
    target: Int,
    attachment: Int,
    renderbuffertarget: Int,
    renderbuffer: Int
) = GLES20.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer)

actual inline fun glGenRenderbuffers(): Int {
    val result = IntArray(1)
    GLES20.glGenRenderbuffers(1, result, 0)
    return result[0]
}

actual inline fun glDeleteRenderbuffers(renderbuffer: Int) =
    GLES20.glDeleteRenderbuffers(1, intArrayOf(renderbuffer), 0)

actual inline fun glBindRenderbuffer(target: Int, renderbuffer: Int) =
    GLES20.glBindRenderbuffer(target, renderbuffer)

actual inline fun glRenderbufferStorage(
    target: Int,
    internalformat: Int,
    width: Int,
    height: Int
) = GLES20.glRenderbufferStorage(target, internalformat, width, height)

actual inline fun glRenderbufferStorageMultisample(
    target: Int,
    samples: Int,
    internalformat: Int,
    width: Int,
    height: Int
) = GLES30.glRenderbufferStorageMultisample(target, samples, internalformat, width, height)

actual inline fun glClearBufferfv(
    buffer: Int,
    drawbuffer: Int,
    value: FloatArray
) {
    GLES.glClearBufferfv(buffer, drawbuffer, value, 0)
}

actual inline fun glFlush() = GLES20.glFlush()

actual inline fun glGetProgramResourceiv(
    program: Int,
    programInterface: Int,
    index: Int,
    props: IntArray,
    length: IntArray?,
    params: IntArray
) = GLES31.glGetProgramResourceiv(
    program, programInterface, index,
    props.size, props, 0,
    length?.size ?: 0, length, 0,
    params, 0
)

actual inline fun glGetProgramResourceIndex(
    program: Int,
    programInterface: Int,
    name: CharSequence
): Int = GLES31.glGetProgramResourceIndex(program, programInterface, name.toString())

actual inline fun glGetProgrami(program: Int, pname: Int): Int {
    val result = IntArray(1)
    GLES20.glGetProgramiv(program, pname, result, 0)
    return result[0]
}

actual inline fun glDispatchCompute(
    num_groups_x: Int,
    num_groups_y: Int,
    num_groups_z: Int
) {
    GLES31.glDispatchCompute(num_groups_x, num_groups_y, num_groups_z)
}

actual inline fun glMemoryBarrier(barriers: Int) {
    GLES31.glMemoryBarrier(barriers)
}

actual inline fun glBindImageTexture(
    unit: Int,
    texture: Int,
    level: Int,
    layered: Boolean,
    layer: Int,
    access: Int,
    format: Int
) = GLES31.glBindImageTexture(unit, texture, level, layered, layer,access, format)

actual inline fun glPatchParameteri(pname: Int, value: Int) {
    notSupportedOnAndroid()
}

actual inline fun glReadPixels(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    format: Int,
    type: Int,
    pixels: ByteBuffer
) = GLES20.glReadPixels(x, y, width, height, format, type, pixels)

actual inline fun glGetTexParameterf(target: Int, pname: Int): Float {
    notSupportedOnAndroid()
    error("")
}

actual inline fun glMapBufferRange(
    target: Int,
    offset: Long,
    length: Long,
    access: Int
): ByteBuffer? {
    val buf = GLES.glMapBufferRange(target ,offset.toInt(), length.toInt(), access)
    return buf as ByteBuffer
}

actual inline fun glUnmapBuffer(target: Int): Boolean = GLES.glUnmapBuffer(target)
actual inline fun glIsBuffer(buffer: Int): Boolean = GLES20.glIsBuffer(buffer)
actual inline fun glIsTexture(texture: Int): Boolean = GLES20.glIsTexture(texture)

