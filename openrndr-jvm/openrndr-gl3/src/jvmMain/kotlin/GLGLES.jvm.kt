@file:Suppress("NOTHING_TO_INLINE")

package org.openrndr.internal.gl3

import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memASCII
import org.openrndr.internal.Driver
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import org.lwjgl.opengl.GL45C as GL
import org.lwjgl.opengles.GLES32 as GLES

var driverType = DriverGL3Configuration.driverType

actual inline fun glEnable(target: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glEnable(target)
        DriverTypeGL.GLES -> GLES.glEnable(target)
    }
}

// --- [ glIsEnabled ] ---
actual inline fun glIsEnabled(cap: Int): Boolean {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glIsEnabled(cap)
        DriverTypeGL.GLES -> GLES.glIsEnabled(cap)
    }
}

// --- [ glDisable ] ---
actual inline fun glDisable(target: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glDisable(target)
        DriverTypeGL.GLES -> GLES.glDisable(target)
    }
}

actual inline fun glGenBuffers(): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGenBuffers()
        DriverTypeGL.GLES -> GLES.glGenBuffers()
    }
}

actual inline fun glBindBuffer(target: Int, buffer: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glBindBuffer(target, buffer)
        DriverTypeGL.GLES -> GLES.glBindBuffer(target, buffer)
    }
}

actual inline fun glBufferData(target: Int, size: Long, usage: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glBufferData(target, size, usage)
        DriverTypeGL.GLES -> GLES.glBufferData(target, size, usage)
    }
}

actual inline fun glGetIntegerv(pname: Int, params: IntArray) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetIntegerv(pname, params)
        DriverTypeGL.GLES -> GLES.glGetIntegerv(pname, params)
    }
}

actual inline fun glGetInteger(pname: Int): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetInteger(pname)
        DriverTypeGL.GLES -> GLES.glGetInteger(pname)
    }
}

actual inline fun glGetString(pname: Int): String? {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetString(pname)
        DriverTypeGL.GLES -> GLES.glGetString(pname)
    }
}

actual inline fun glBufferSubData(target: Int, offset: Long, data: ByteBuffer) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glBufferSubData(target, offset, data)
        DriverTypeGL.GLES -> GLES.glBufferSubData(target, offset, data)
    }
}

actual inline fun glBindFramebuffer(target: Int, framebuffer: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glBindFramebuffer(target, framebuffer)
        DriverTypeGL.GLES -> GLES.glBindFramebuffer(target, framebuffer)
    }
}

// --- [ glScissor ] ---
actual inline fun glScissor(x: Int, y: Int, width: Int, height: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glScissor(x, y, width, height)
        DriverTypeGL.GLES -> GLES.glScissor(x, y, width, height)
    }
}

// --- [ glViewport ] ---
actual inline fun glViewport(x: Int, y: Int, w: Int, h: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glViewport(x, y, w, h)
        DriverTypeGL.GLES -> GLES.glViewport(x, y, w, h)
    }
}

actual inline fun glDepthMask(flag: Boolean) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glDepthMask(flag)
        DriverTypeGL.GLES -> GLES.glDepthMask(flag)
    }
}

actual inline fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glClearColor(red, green, blue, alpha)
        DriverTypeGL.GLES -> GLES.glClearColor(red, green, blue, alpha)
    }
}


// --- [ glClear ] ---
actual inline fun glClear(mask: Int): Unit {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glClear(mask)
        DriverTypeGL.GLES -> GLES.glClear(mask)
    }
}

actual inline fun glGenFramebuffers(): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGenFramebuffers()
        DriverTypeGL.GLES -> GLES.glGenFramebuffers()
    }
}

actual inline fun glDrawBuffers(bufs: IntArray) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glDrawBuffers(bufs)
        DriverTypeGL.GLES -> GLES.glDrawBuffers(bufs)
    }
}

actual inline fun glFramebufferTexture(target: Int, attachment: Int, texture: Int, level: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glFramebufferTexture(target, attachment, texture, level)
        DriverTypeGL.GLES -> GLES.glFramebufferTexture(target, attachment, texture, level)
    }
}

actual inline fun glBlendEquationi(buf: Int, mode: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glBlendEquationi(buf, mode)
        DriverTypeGL.GLES -> GLES.glBlendEquationi(buf, mode)
    }
}

actual inline fun glBlendFunci(buf: Int, sfactor: Int, dfactor: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glBlendFunci(buf, sfactor, dfactor)
        DriverTypeGL.GLES -> GLES.glBlendFunci(buf, sfactor, dfactor)
    }
}

actual inline fun glBindVertexArray(array: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glBindVertexArray(array)
        DriverTypeGL.GLES -> GLES.glBindVertexArray(array)
    }
}

// --- [ glClearDepth ] ---
actual inline fun glClearDepth(depth: Double) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glClearDepth(depth)
        DriverTypeGL.GLES -> GLES.glClearDepthf(depth.toFloat())
    }
}

// --- [ glCreateShader ] ---
actual inline fun glCreateShader(type: Int): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glCreateShader(type)
        DriverTypeGL.GLES -> GLES.glCreateShader(type)
    }
}

actual inline fun glShaderSource(
    shader: Int, string: CharSequence
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glShaderSource(shader, string)
        DriverTypeGL.GLES -> GLES.glShaderSource(shader, string)
    }
}

actual inline fun glCompileShader(shader: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glCompileShader(shader)
        DriverTypeGL.GLES -> GLES.glCompileShader(shader)
    }
}

actual inline fun glGetShaderiv(shader: Int, pname: Int, params: IntArray) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetShaderiv(shader, pname, params)
        DriverTypeGL.GLES -> GLES.glGetShaderiv(shader, pname, params)
    }
}

actual inline fun glGetProgramiv(program: Int, pname: Int, params: IntArray) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetProgramiv(program, pname, params)
        DriverTypeGL.GLES -> GLES.glGetProgramiv(program, pname, params)
    }
}

actual inline fun glGetShaderInfoLog(shader: Int, length: IntArray?, infoLog: ByteBuffer) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetShaderInfoLog(shader, length, infoLog)
        DriverTypeGL.GLES -> GLES.glGetShaderInfoLog(shader, length, infoLog)
    }
}

actual inline fun glGetProgramInfoLog(program: Int, length: IntArray?, infoLog: ByteBuffer) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetProgramInfoLog(program, length, infoLog)
        DriverTypeGL.GLES -> GLES.glGetProgramInfoLog(program, length, infoLog)
    }
}


actual inline fun glCreateProgram(): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glCreateProgram()
        DriverTypeGL.GLES -> GLES.glCreateProgram()
    }
}


actual inline fun glAttachShader(program: Int, shader: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glAttachShader(program, shader)
        DriverTypeGL.GLES -> GLES.glAttachShader(program, shader)
    }
}

actual inline fun glLinkProgram(program: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glLinkProgram(program)
        DriverTypeGL.GLES -> GLES.glLinkProgram(program)
    }
}

actual inline fun glFinish() {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glFinish()
        DriverTypeGL.GLES -> GLES.glFinish()
    }
}

actual inline fun glUseProgram(program: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glUseProgram(program)
        DriverTypeGL.GLES -> GLES.glUseProgram(program)
    }
}

actual inline fun glGetUniformBlockIndex(program: Int, uniformBlockName: CharSequence): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetUniformBlockIndex(program, uniformBlockName)
        DriverTypeGL.GLES -> GLES.glGetUniformBlockIndex(program, uniformBlockName)
    }
}

actual inline fun glGetAttribLocation(program: Int, name: CharSequence): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetAttribLocation(program, name)
        DriverTypeGL.GLES -> GLES.glGetAttribLocation(program, name)
    }
}

actual inline fun glDeleteProgram(program: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glDeleteProgram(program)
        DriverTypeGL.GLES -> GLES.glDeleteProgram(program)
    }
}

actual inline fun glUniformBlockBinding(
    program: Int,
    uniformBlockIndex: Int,
    uniformBlockBinding: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding)
        DriverTypeGL.GLES -> GLES.glUniformBlockBinding(
            program,
            uniformBlockIndex,
            uniformBlockBinding
        )
    }
}

actual inline fun glGetActiveUniformBlockiv(
    program: Int,
    uniformBlockIndex: Int,
    pname: Int,
    params: IntBuffer
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, params)
        DriverTypeGL.GLES -> GLES.glGetActiveUniformBlockiv(
            program,
            uniformBlockIndex,
            pname,
            params
        )
    }
}

actual inline fun glGetActiveUniformsiv(
    program: Int,
    uniformIndices: IntBuffer,
    pname: Int,
    params: IntBuffer
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetActiveUniformsiv(program, uniformIndices, pname, params)
        DriverTypeGL.GLES -> GLES.glGetActiveUniformsiv(program, uniformIndices, pname, params)
    }
}

actual inline fun glGetActiveUniformsiv(
    program: Int,
    uniformIndices: IntArray,
    pname: Int,
    params: IntArray
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetActiveUniformsiv(program, uniformIndices, pname, params)
        DriverTypeGL.GLES -> GLES.glGetActiveUniformsiv(program, uniformIndices, pname, params)
    }
}

actual inline fun glGetActiveUniformName(program: Int, uniformIndex: Int, bufSize: Int): String {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetActiveUniformName(program, uniformIndex, bufSize)
        DriverTypeGL.GLES -> {
            val length = IntArray(1) { 1024 }
            val type = IntArray(1)
            val size = IntArray(1)
            val name = MemoryUtil.memAlloc(1024)
            GLES.glGetActiveUniform(program, uniformIndex, length, size, type, name)
            val ascii = memASCII(name, length.get(0))
            MemoryUtil.memFree(name)
            ascii
        }
    }
}



actual inline fun glGetError(): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetError()
        DriverTypeGL.GLES -> GLES.glGetError()
    }
}


actual inline fun glBindBufferBase(target: Int, index: Int, buffer: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glBindBufferBase(target, index, buffer)
        DriverTypeGL.GLES -> GLES.glBindBufferBase(target, index, buffer)
    }
}


actual inline fun glColorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glColorMask(red, green, blue, alpha)
        DriverTypeGL.GLES -> GLES.glColorMask(red, green, blue, alpha)
    }
}

actual inline fun glBlendEquation(mode: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glBlendEquation(mode)
        DriverTypeGL.GLES -> GLES.glBlendEquation(mode)
    }
}

actual inline fun glBlendFunc(sfactor: Int, dfactor: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glBlendFunc(sfactor, dfactor)
        DriverTypeGL.GLES -> GLES.glBlendFunc(sfactor, dfactor)
    }
}

actual inline fun glBlendEquationSeparate(modeRGB: Int, modeAlpha: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glBlendEquationSeparate(modeRGB, modeAlpha)
        DriverTypeGL.GLES -> GLES.glBlendEquationSeparate(modeRGB, modeAlpha)
    }
}

actual inline fun glBlendFuncSeparate(
    sfactorRGB: Int,
    dfactorRGB: Int,
    sfactorAlpha: Int,
    dfactorAlpha: Int
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glBlendFuncSeparate(
            sfactorRGB,
            dfactorRGB,
            sfactorAlpha,
            dfactorAlpha
        )

        DriverTypeGL.GLES -> GLES.glBlendFuncSeparate(
            sfactorRGB,
            dfactorRGB,
            sfactorAlpha,
            dfactorAlpha
        )
    }
}

actual inline fun glDepthFunc(func: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glDepthFunc(func)
        DriverTypeGL.GLES -> GLES.glDepthFunc(func)
    }
}

actual inline fun glCullFace(mode: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glCullFace(mode)
        DriverTypeGL.GLES -> GLES.glCullFace(mode)
    }
}

actual inline fun glGenVertexArrays(arrays: IntArray) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glGenVertexArrays(arrays)
        DriverTypeGL.GLES -> GLES.glGenVertexArrays(arrays)
    }
}

actual inline fun glDrawArraysInstanced(mode: Int, first: Int, count: Int, primcount: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glDrawArraysInstanced(mode, first, count, primcount)
        DriverTypeGL.GLES -> GLES.glDrawArraysInstanced(mode, first, count, primcount)
    }
}

actual inline fun glDrawElements(mode: Int, count: Int, type: Int, indices: Long) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glDrawElements(mode, count, type, indices)
        DriverTypeGL.GLES -> GLES.glDrawElements(mode, count, type, indices)
    }
}

actual inline fun glDrawElementsInstanced(
    mode: Int,
    count: Int,
    type: Int,
    indices: Long,
    primcount: Int
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glDrawElementsInstanced(mode, count, type, indices, primcount)
        DriverTypeGL.GLES -> GLES.glDrawElementsInstanced(mode, count, type, indices, primcount)
    }
}


actual inline fun glEnableVertexAttribArray(index: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glEnableVertexAttribArray(index)
        DriverTypeGL.GLES -> GLES.glEnableVertexAttribArray(index)
    }
}

actual inline fun glVertexAttribPointer(
    index: Int,
    size: Int,
    type: Int,
    normalized: Boolean,
    stride: Int,
    pointer: Long
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glVertexAttribPointer(index, size, type, normalized, stride, pointer)
        DriverTypeGL.GLES -> GLES.glVertexAttribPointer(
            index,
            size,
            type,
            normalized,
            stride,
            pointer
        )
    }
}

actual inline fun glVertexAttribIPointer(
    index: Int,
    size: Int,
    type: Int,
    stride: Int,
    pointer: Long
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glVertexAttribIPointer(index, size, type, stride, pointer)
        DriverTypeGL.GLES -> GLES.glVertexAttribIPointer(index, size, type, stride, pointer)
    }
}


actual inline fun glVertexAttribDivisor(index: Int, divisor: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glVertexAttribDivisor(index, divisor)
        DriverTypeGL.GLES -> GLES.glVertexAttribDivisor(index, divisor)
    }
}

actual inline fun glDeleteBuffers(buffer: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glDeleteBuffers(buffer)
        DriverTypeGL.GLES -> GLES.glDeleteBuffers(buffer)
    }
}

actual inline fun glDeleteVertexArrays(array: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glDeleteVertexArrays(array)
        DriverTypeGL.GLES -> GLES.glDeleteVertexArrays(array)
    }
}

actual inline fun glGetUniformLocation(program: Int, name: CharSequence): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetUniformLocation(program, name)
        DriverTypeGL.GLES -> GLES.glGetUniformLocation(program, name)
    }
}

actual inline fun glProgramUniform4f(
    program: Int,
    location: Int,
    x: Float,
    y: Float,
    z: Float,
    w: Float
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform4f(program, location, x, y, z, w)
        DriverTypeGL.GLES -> GLES.glProgramUniform4f(program, location, x, y, z, w)
    }
}

actual inline fun glUniform4f(location: Int, v0: Float, v1: Float, v2: Float, v3: Float) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform4f(location, v0, v1, v2, v3)
        DriverTypeGL.GLES -> GLES.glUniform4f(location, v0, v1, v2, v3)
    }
}

actual inline fun glProgramUniform3f(program: Int, location: Int, x: Float, y: Float, z: Float) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform3f(program, location, x, y, z)
        DriverTypeGL.GLES -> GLES.glProgramUniform3f(program, location, x, y, z)
    }
}

actual inline fun glUniform3f(location: Int, v0: Float, v1: Float, v2: Float) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform3f(location, v0, v1, v2)
        DriverTypeGL.GLES -> GLES.glUniform3f(location, v0, v1, v2)
    }
}

actual inline fun glProgramUniform2f(program: Int, location: Int, x: Float, y: Float) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform2f(program, location, x, y)
        DriverTypeGL.GLES -> GLES.glProgramUniform2f(program, location, x, y)
    }
}

actual inline fun glUniform2f(location: Int, v0: Float, v1: Float) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform2f(location, v0, v1)
        DriverTypeGL.GLES -> GLES.glUniform2f(location, v0, v1)
    }
}

actual inline fun glProgramUniform1i(program: Int, location: Int, x: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform1i(program, location, x)
        DriverTypeGL.GLES -> GLES.glProgramUniform1i(program, location, x)
    }
}

actual inline fun glUniform1i(location: Int, v0: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform1i(location, v0)
        DriverTypeGL.GLES -> GLES.glUniform1i(location, v0)
    }
}


actual inline fun glProgramUniform1f(program: Int, location: Int, x: Float) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform1f(program, location, x)
        DriverTypeGL.GLES -> GLES.glProgramUniform1f(program, location, x)
    }
}

actual inline fun glUniform1f(location: Int, v0: Float) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform1f(location, v0)
        DriverTypeGL.GLES -> GLES.glUniform1f(location, v0)
    }
}


actual inline fun glUniform2i(
    location: Int, v0: Int, v1: Int
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform2i(location, v0, v1)
        DriverTypeGL.GLES -> GLES.glUniform2i(location, v0, v1)
    }
}

actual inline fun glUniform3i(
    location: Int, v0: Int, v1: Int, v2: Int
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform3i(location, v0, v1, v2)
        DriverTypeGL.GLES -> GLES.glUniform3i(location, v0, v1, v2)
    }
}

actual inline fun glUniform4i(
    location: Int, v0: Int, v1: Int, v2: Int, v3: Int,
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform4i(location, v0, v1, v2, v3)
        DriverTypeGL.GLES -> GLES.glUniform4i(location, v0, v1, v2, v3)
    }
}


actual inline fun glProgramUniform2i(program: Int, location: Int, x: Int, y: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform2i(program, location, x, y)
        DriverTypeGL.GLES -> GLES.glProgramUniform2i(program, location, x, y)
    }
}


actual inline fun glProgramUniform3i(
    program: Int, location: Int, x: Int, y: Int, z: Int

) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform3i(program, location, x, y, z)
        DriverTypeGL.GLES -> GLES.glProgramUniform3i(program, location, x, y, z)
    }
}

actual inline fun glProgramUniform4i(program: Int, location: Int, x: Int, y: Int, z: Int, w: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform4i(program, location, x, y, z, w)
        DriverTypeGL.GLES -> GLES.glProgramUniform4i(program, location, x, y, z, w)
    }
}

actual inline fun glProgramUniformMatrix3fv(
    program: Int,
    location: Int,
    transpose: Boolean,
    value: FloatArray
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniformMatrix3fv(program, location, transpose, value)
        DriverTypeGL.GLES -> GLES.glProgramUniformMatrix3fv(program, location, transpose, value)
    }
}

actual inline fun glProgramUniformMatrix3fv(
    program: Int, location: Int, transpose: Boolean, value: FloatBuffer
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniformMatrix3fv(program, location, transpose, value)
        DriverTypeGL.GLES -> GLES.glProgramUniformMatrix3fv(program, location, transpose, value)
    }
}

actual inline fun glUniformMatrix3fv(location: Int, transpose: Boolean, value: FloatArray) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniformMatrix3fv(location, transpose, value)
        DriverTypeGL.GLES -> GLES.glUniformMatrix3fv(location, transpose, value)
    }
}

actual inline fun glProgramUniformMatrix4fv(
    program: Int,
    location: Int,
    transpose: Boolean,
    value: FloatArray
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniformMatrix4fv(program, location, transpose, value)
        DriverTypeGL.GLES -> GLES.glProgramUniformMatrix4fv(program, location, transpose, value)
    }
}

actual inline fun glUniformMatrix4fv(location: Int, transpose: Boolean, value: FloatArray) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniformMatrix4fv(location, transpose, value)
        DriverTypeGL.GLES -> GLES.glUniformMatrix4fv(location, transpose, value)
    }
}

actual inline fun glProgramUniform4iv(
    program: Int, location: Int, value: IntArray
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform4iv(program, location, value)
        DriverTypeGL.GLES -> GLES.glProgramUniform4iv(program, location, value)
    }
}

actual inline fun glUniform4iv(location: Int, value: IntArray) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform4iv(location, value)
        DriverTypeGL.GLES -> GLES.glUniform4iv(location, value)
    }
}

actual inline fun glProgramUniform3iv(
    program: Int, location: Int, value: IntArray
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform3iv(program, location, value)
        DriverTypeGL.GLES -> GLES.glProgramUniform3iv(program, location, value)
    }
}

actual inline fun glUniform3iv(location: Int, value: IntArray) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform3iv(location, value)
        DriverTypeGL.GLES -> GLES.glUniform3iv(location, value)
    }
}

actual inline fun glProgramUniform2iv(
    program: Int, location: Int, value: IntArray
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform2iv(program, location, value)
        DriverTypeGL.GLES -> GLES.glProgramUniform2iv(program, location, value)
    }
}

actual inline fun glUniform2iv(location: Int, value: IntArray) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform2iv(location, value)
        DriverTypeGL.GLES -> GLES.glUniform2iv(location, value)
    }
}

actual inline fun glProgramUniform2fv(program: Int, location: Int, value: FloatArray) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform2fv(program, location, value)
        DriverTypeGL.GLES -> GLES.glProgramUniform2fv(program, location, value)
    }
}

actual inline fun glUniform2fv(
    location: Int, value: FloatArray
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform2fv(location, value)
        DriverTypeGL.GLES -> GLES.glUniform2fv(location, value)
    }
}

actual inline fun glProgramUniform3fv(
    program: Int, location: Int, value: FloatArray
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform3fv(program, location, value)
        DriverTypeGL.GLES -> GLES.glProgramUniform3fv(program, location, value)
    }
}

actual inline fun glUniform3fv(location: Int, value: FloatArray) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform3fv(location, value)
        DriverTypeGL.GLES -> GLES.glUniform3fv(location, value)
    }
}

actual inline fun glProgramUniform4fv(program: Int, location: Int, value: FloatArray) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform4fv(program, location, value)
        DriverTypeGL.GLES -> GLES.glProgramUniform4fv(program, location, value)
    }
}


actual inline fun glUniform4fv(location: Int, value: FloatArray) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform4fv(location, value)
        DriverTypeGL.GLES -> GLES.glUniform4fv(location, value)
    }
}

actual inline fun glProgramUniform1fv(program: Int, location: Int, value: FloatArray) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform1fv(program, location, value)
        DriverTypeGL.GLES -> GLES.glProgramUniform1fv(program, location, value)
    }
}

actual inline fun glUniform1fv(
    location: Int, value: FloatArray
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform1fv(location, value)
        DriverTypeGL.GLES -> GLES.glUniform1fv(location, value)
    }
}

actual inline fun glProgramUniform1iv(
    program: Int, location: Int, value: IntArray
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform1iv(program, location, value)
        DriverTypeGL.GLES -> GLES.glProgramUniform1iv(program, location, value)
    }
}

actual inline fun glUniform1iv(location: Int, value: IntArray) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform1iv(location, value)
        DriverTypeGL.GLES -> GLES.glUniform1iv(location, value)
    }
}

actual inline fun glProgramUniformMatrix4fv(
    program: Int,
    location: Int,
    transpose: Boolean,
    value: FloatBuffer
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniformMatrix4fv(program, location, transpose, value)
        DriverTypeGL.GLES -> GLES.glProgramUniformMatrix4fv(program, location, transpose, value)
    }
}

actual inline fun glUniformMatrix3fv(
    location: Int, transpose: Boolean, value: FloatBuffer
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniformMatrix3fv(location, transpose, value)
        DriverTypeGL.GLES -> GLES.glUniformMatrix3fv(location, transpose, value)
    }
}

actual inline fun glUniformMatrix4fv(
    location: Int, transpose: Boolean, value: FloatBuffer
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniformMatrix4fv(location, transpose, value)
        DriverTypeGL.GLES -> GLES.glUniformMatrix4fv(location, transpose, value)
    }
}

actual inline fun glGetActiveUniform(
    program: Int, index: Int, length: IntArray, size: IntArray, type: IntArray
): String {
    val nameBuffer = BufferUtils.createByteBuffer(256)
    when (driverType) {
        DriverTypeGL.GL -> GL.glGetActiveUniform(program, index, length, size, type, nameBuffer)
        DriverTypeGL.GLES -> GLES.glGetActiveUniform(program, index, length, size, type, nameBuffer)
    }
    val nameBytes = ByteArray(length[0])
    (nameBuffer as Buffer).rewind()
    nameBuffer.get(nameBytes)
    return String(nameBytes)
}


actual inline fun glTexParameteri(target: Int, pname: Int, param: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glTexParameteri(target, pname, param)
        DriverTypeGL.GLES -> GLES.glTexParameteri(target, pname, param)
    }
}

actual inline fun glGenTextures(): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGenTextures()
        DriverTypeGL.GLES -> GLES.glGenTextures()
    }
}

actual inline fun glBindTexture(target: Int, texture: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glBindTexture(target, texture)
        DriverTypeGL.GLES -> GLES.glBindTexture(target, texture)
    }
}

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
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glTexImage3D(
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

        DriverTypeGL.GLES -> GLES.glTexImage3D(
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
    }
}

actual inline fun glActiveTexture(texture: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glActiveTexture(texture)
        DriverTypeGL.GLES -> GLES.glActiveTexture(texture)
    }
}

actual inline fun glDeleteTextures(texture: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glDeleteTextures(texture)
        DriverTypeGL.GLES -> GLES.glDeleteTextures(texture)
    }
}

actual inline fun glReadBuffer(src: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glReadBuffer(src)
        DriverTypeGL.GLES -> GLES.glReadBuffer(src)
    }
}

actual inline fun glGenerateMipmap(target: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGenerateMipmap(target)
        DriverTypeGL.GLES -> GLES.glGenerateMipmap(target)
    }
}

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
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glTexSubImage3D(
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

        DriverTypeGL.GLES -> GLES.glTexSubImage3D(
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
    }
}


actual inline fun glCopyTexSubImage2D(
    target: Int,
    level: Int,
    xoffset: Int,
    yoffset: Int,
    x: Int,
    y: Int,
    width: Int,
    height: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glCopyTexSubImage2D(
            target,
            level,
            xoffset,
            yoffset,
            x,
            y,
            width,
            height
        )

        DriverTypeGL.GLES -> GLES.glCopyTexSubImage2D(
            target,
            level,
            xoffset,
            yoffset,
            x,
            y,
            width,
            height
        )
    }
}

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
    return when (driverType) {
        DriverTypeGL.GL -> GL.glCompressedTexSubImage3D(
            target,
            level,
            xoffset,
            yoffset,
            zoffset,
            width,
            height,
            depth,
            format,
            data
        )

        DriverTypeGL.GLES -> GLES.glCompressedTexSubImage3D(
            target,
            level,
            xoffset,
            yoffset,
            zoffset,
            width,
            height,
            depth,
            format,
            data
        )
    }
}


// --- [ glCopyTexSubImage3D ] ---
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
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glCopyTexSubImage3D(
            target,
            level,
            xoffset,
            yoffset,
            zoffset,
            x,
            y,
            width,
            height
        )

        DriverTypeGL.GLES -> GLES.glCopyTexSubImage3D(
            target,
            level,
            xoffset,
            yoffset,
            zoffset,
            x,
            y,
            width,
            height
        )
    }
}


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
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glTexImage2D(
            target,
            level,
            internalformat,
            width,
            height,
            border,
            format,
            type,
            pixels
        )

        DriverTypeGL.GLES -> GLES.glTexImage2D(
            target,
            level,
            internalformat,
            width,
            height,
            border,
            format,
            type,
            pixels
        )
    }
}

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
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glBlitFramebuffer(
            srcX0,
            srcY0,
            srcX1,
            srcY1,
            dstX0,
            dstY0,
            dstX1,
            dstY1,
            mask,
            filter
        )

        DriverTypeGL.GLES -> GLES.glBlitFramebuffer(
            srcX0,
            srcY0,
            srcX1,
            srcY1,
            dstX0,
            dstY0,
            dstX1,
            dstY1,
            mask,
            filter
        )
    }
}


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
    return when (driverType) {
        DriverTypeGL.GL -> GL.glCopyImageSubData(
            srcName,
            srcTarget,
            srcLevel,
            srcX,
            srcY,
            srcZ,
            dstName,
            dstTarget,
            dstLevel,
            dstX,
            dstY,
            dstZ,
            srcWidth,
            srcHeight,
            srcDepth
        )

        DriverTypeGL.GLES -> GLES.glCopyImageSubData(
            srcName,
            srcTarget,
            srcLevel,
            srcX,
            srcY,
            srcZ,
            dstName,
            dstTarget,
            dstLevel,
            dstX,
            dstY,
            dstZ,
            srcWidth,
            srcHeight,
            srcDepth
        )
    }
}

actual inline fun glTexParameterf(target: Int, pname: Int, param: Float) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glTexParameterf(target, pname, param)
        DriverTypeGL.GLES -> GLES.glTexParameterf(target, pname, param)
    }
}


actual inline fun glPixelStorei(pname: Int, param: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glPixelStorei(pname, param)
        DriverTypeGL.GLES -> GLES.glPixelStorei(pname, param)
    }
}


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
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glTexSubImage2D(
            target,
            level,
            xoffset,
            yoffset,
            width,
            height,
            format,
            type,
            pixels
        )

        DriverTypeGL.GLES -> GLES.glTexSubImage2D(
            target,
            level,
            xoffset,
            yoffset,
            width,
            height,
            format,
            type,
            pixels
        )
    }
}

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
    return when (driverType) {
        DriverTypeGL.GL -> GL.glCompressedTexSubImage2D(
            target,
            level,
            xoffset,
            yoffset,
            width,
            height,
            format,
            data
        )

        DriverTypeGL.GLES -> GLES.glCompressedTexSubImage2D(
            target,
            level,
            xoffset,
            yoffset,
            width,
            height,
            format,
            data
        )
    }
}


actual inline fun glFramebufferTexture2D(
    target: Int,
    attachment: Int,
    textarget: Int,
    texture: Int,
    level: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glFramebufferTexture2D(target, attachment, textarget, texture, level)
        DriverTypeGL.GLES -> GLES.glFramebufferTexture2D(
            target,
            attachment,
            textarget,
            texture,
            level
        )
    }
}


actual inline fun glFramebufferTextureLayer(
    target: Int,
    attachment: Int,
    texture: Int,
    level: Int,
    layer: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glFramebufferTextureLayer(target, attachment, texture, level, layer)
        DriverTypeGL.GLES -> GLES.glFramebufferTextureLayer(
            target,
            attachment,
            texture,
            level,
            layer
        )
    }

}

actual inline fun glGetBoolean(pname: Int): Boolean {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetBoolean(pname)
        DriverTypeGL.GLES -> GLES.glGetBoolean(pname)
    }
}

actual inline fun glGetTexLevelParameteri(
    target: Int,
    level: Int,
    pname: Int
): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetTexLevelParameteri(target, level, pname)
        DriverTypeGL.GLES -> {
            val values = IntArray(4)
            if (Driver.glVersion >= DriverVersionGL.GLES_VERSION_3_1) {
                values[0] = GLES.glGetTexLevelParameteri(target, level, pname)
            } else {
                TODO("implement this")
//                (Driver.instance as DriverGL3).angleExtensions?.glTexLevelParameterivANGLE(
//                    target,
//                    level,
//                    pname,
//                    values
//                )
            }
            values[0]
        }
    }
}

actual inline fun glGetTexParameteri(target: Int, pname: Int): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetTexParameteri(target, pname)
        DriverTypeGL.GLES -> GLES.glGetTexParameteri(target, pname)
    }
}

actual inline fun glDeleteFramebuffers(framebuffer: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glDeleteFramebuffers(framebuffer)
        DriverTypeGL.GLES -> GLES.glDeleteFramebuffers(framebuffer)
    }
}


actual inline fun glStencilFuncSeparate(
    face: Int,
    func: Int,
    ref: Int,
    mask: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glStencilFuncSeparate(face, func, ref, mask)
        DriverTypeGL.GLES -> GLES.glStencilFuncSeparate(face, func, ref, mask)
    }
}

actual inline fun glStencilOpSeparate(
    face: Int,
    sfail: Int,
    dpfail: Int,
    dppass: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glStencilOpSeparate(face, sfail, dpfail, dppass)
        DriverTypeGL.GLES -> GLES.glStencilOpSeparate(face, sfail, dpfail, dppass)
    }
}

actual inline fun glStencilMaskSeparate(face: Int, mask: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glStencilMaskSeparate(face, mask)
        DriverTypeGL.GLES -> GLES.glStencilMaskSeparate(face, mask)
    }
}

actual inline fun glDrawArrays(
    mode: Int,
    first: Int,
    count: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glDrawArrays(mode, first, count)
        DriverTypeGL.GLES -> GLES.glDrawArrays(mode, first, count)
    }
}

// --- [ glCheckFramebufferStatus ] ---
actual inline fun glCheckFramebufferStatus(target: Int): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glCheckFramebufferStatus(target)
        DriverTypeGL.GLES -> GLES.glCheckFramebufferStatus(target)
    }
}

// --- [ glClearBufferfi ] ---
actual inline fun glClearBufferfi(buffer: Int, drawbuffer: Int, depth: Float, stencil: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glClearBufferfi(buffer, drawbuffer, depth, stencil)
        DriverTypeGL.GLES -> GLES.glClearBufferfi(buffer, drawbuffer, depth, stencil)
    }
}

actual inline fun glClearBufferfv(
    buffer: Int,
    drawbuffer: Int,
    value: FloatBuffer
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glClearBufferfv(buffer, drawbuffer, value)
        DriverTypeGL.GLES -> GLES.glClearBufferfv(buffer, drawbuffer, value)
    }
}

actual inline fun glClearBufferiv(
    buffer: Int,
    drawbuffer: Int,
    value: IntBuffer
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glClearBufferiv(buffer, drawbuffer, value)
        DriverTypeGL.GLES -> GLES.glClearBufferiv(buffer, drawbuffer, value)
    }
}

// --- [ glTexImage2DMultisample ] ---
actual inline fun glTexImage2DMultisample(
    target: Int,
    samples: Int,
    internalformat: Int,
    width: Int,
    height: Int,
    fixedsamplelocations: Boolean
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glTexImage2DMultisample(
            target,
            samples,
            internalformat,
            width,
            height,
            fixedsamplelocations
        )

        DriverTypeGL.GLES -> {
            TODO("implement this")
//            val angle = (Driver.instance as DriverGL3).angleExtensions
//            if (angle != null && angle.glTexStorage2DMultisampleANGLEAddress != 0L) {
//                angle.glTexStorage2DMultisampleANGLE(
//                    target,
//                    samples,
//                    internalformat,
//                    width,
//                    height,
//                    fixedsamplelocations
//                )
//            } else {
//                error("not supported")
//            }
        }
    }
}

// --- [ glTexStorage2D ] ---
actual inline fun glTexStorage2D(
    target: Int,
    levels: Int,
    internalformat: Int,
    width: Int,
    height: Int
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glTexStorage2D(target, levels, internalformat, width, height)
        DriverTypeGL.GLES -> GLES.glTexStorage2D(target, levels, internalformat, width, height)
    }
}

// --- [ glTexStorage2DMultisample ] ---
actual inline fun glTexStorage2DMultisample(
    target: Int,
    samples: Int,
    internalformat: Int,
    width: Int,
    height: Int,
    fixedsamplelocations: Boolean
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glTexStorage2DMultisample(
            target,
            samples,
            internalformat,
            width,
            height,
            fixedsamplelocations
        )

        DriverTypeGL.GLES -> GLES.glTexStorage2DMultisample(
            target,
            samples,
            internalformat,
            width,
            height,
            fixedsamplelocations
        )
    }
}


actual inline fun glTexStorage3D(
    target: Int,
    levels: Int,
    internalformat: Int,
    width: Int,
    height: Int,
    depth: Int
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glTexStorage3D(target, levels, internalformat, width, height, depth)
        DriverTypeGL.GLES -> GLES.glTexStorage3D(
            target,
            levels,
            internalformat,
            width,
            height,
            depth
        )
    }
}


actual inline fun glFramebufferRenderbuffer(
    target: Int,
    attachment: Int,
    renderbuffertarget: Int,
    renderbuffer: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glFramebufferRenderbuffer(
            target,
            attachment,
            renderbuffertarget,
            renderbuffer
        )

        DriverTypeGL.GLES -> GLES.glFramebufferRenderbuffer(
            target,
            attachment,
            renderbuffertarget,
            renderbuffer
        )
    }
}

actual inline fun glGenRenderbuffers(): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGenRenderbuffers()
        DriverTypeGL.GLES -> GLES.glGenRenderbuffers()
    }
}

actual inline fun glDeleteRenderbuffers(renderbuffer: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glDeleteRenderbuffers(renderbuffer)
        DriverTypeGL.GLES -> GLES.glDeleteRenderbuffers(renderbuffer)
    }
}


actual inline fun glBindRenderbuffer(target: Int, renderbuffer: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glBindRenderbuffer(target, renderbuffer)
        DriverTypeGL.GLES -> GLES.glBindRenderbuffer(target, renderbuffer)
    }
}


actual inline fun glRenderbufferStorage(
    target: Int,
    internalformat: Int,
    width: Int,
    height: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glRenderbufferStorage(target, internalformat, width, height)
        DriverTypeGL.GLES -> GLES.glRenderbufferStorage(target, internalformat, width, height)
    }
}


actual inline fun glRenderbufferStorageMultisample(
    target: Int,
    samples: Int,
    internalformat: Int,
    width: Int,
    height: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glRenderbufferStorageMultisample(
            target,
            samples,
            internalformat,
            width,
            height
        )

        DriverTypeGL.GLES -> GLES.glRenderbufferStorageMultisample(
            target,
            samples,
            internalformat,
            width,
            height
        )
    }
}

actual inline fun glClearBufferfv(
    buffer: Int,
    drawbuffer: Int,
    value: FloatArray
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glClearBufferfv(buffer, drawbuffer, value)
        DriverTypeGL.GLES -> GLES.glClearBufferfv(buffer, drawbuffer, value)
    }
}

actual inline fun glFlush() {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glFlush()
        DriverTypeGL.GLES -> GLES.glFlush()
    }
}

actual inline fun glGetProgramResourceiv(
    program: Int,
    programInterface: Int,
    index: Int,
    props: IntArray,
    length: IntArray?,
    params: IntArray
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glGetProgramResourceiv(
            program,
            programInterface,
            index,
            props,
            length,
            params
        )

        DriverTypeGL.GLES -> GLES.glGetProgramResourceiv(
            program,
            programInterface,
            index,
            props,
            length,
            params
        )
    }
}

actual inline fun glGetProgramResourceIndex(
    program: Int,
    programInterface: Int,
    name: CharSequence
): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetProgramResourceIndex(program, programInterface, name)
        DriverTypeGL.GLES -> GLES.glGetProgramResourceIndex(program, programInterface, name)
    }
}

actual inline fun glGetProgrami(program: Int, pname: Int): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetProgrami(program, pname)
        DriverTypeGL.GLES -> GLES.glGetProgrami(program, pname)
    }
}


actual inline fun glDispatchCompute(
    num_groups_x: Int,
    num_groups_y: Int,
    num_groups_z: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glDispatchCompute(num_groups_x, num_groups_y, num_groups_z)
        DriverTypeGL.GLES -> GLES.glDispatchCompute(num_groups_x, num_groups_y, num_groups_z)
    }
}


actual inline fun glMemoryBarrier(barriers: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glMemoryBarrier(barriers)
        DriverTypeGL.GLES -> GLES.glMemoryBarrier(barriers)
    }
}

// --- [ glBindImageTexture ] ---
actual inline fun glBindImageTexture(
    unit: Int,
    texture: Int,
    level: Int,
    layered: Boolean,
    layer: Int,
    access: Int,
    format: Int
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glBindImageTexture(
            unit,
            texture,
            level,
            layered,
            layer,
            access,
            format
        )

        DriverTypeGL.GLES -> GLES.glBindImageTexture(
            unit,
            texture,
            level,
            layered,
            layer,
            access,
            format
        )
    }
}

// tessellation


actual inline fun glPatchParameteri(pname: Int, value: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glPatchParameteri(pname, value)
        DriverTypeGL.GLES -> GLES.glPatchParameteri(pname, value)
    }
}

actual inline fun glBlendFuncSeparatei(
    buf: Int,
    sfactorRGB: Int,
    dfactorRGB: Int,
    sfactorAlpha: Int,
    dfactorAlpha: Int
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glBlendFuncSeparatei(
            buf,
            sfactorRGB,
            dfactorRGB,
            sfactorAlpha,
            dfactorAlpha
        )

        DriverTypeGL.GLES -> GLES.glBlendFuncSeparatei(
            buf,
            sfactorRGB,
            dfactorRGB,
            sfactorAlpha,
            dfactorAlpha
        )
    }
}

actual inline fun glIsBuffer(buffer: Int): Boolean {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glIsBuffer(buffer)
        DriverTypeGL.GLES -> GLES.glIsBuffer(buffer)
    }
}

actual inline fun glIsTexture(texture: Int): Boolean {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glIsTexture(texture)
        DriverTypeGL.GLES -> GLES.glIsTexture(texture)
    }
}

actual inline fun glBufferData(target: Int, data: ByteBuffer, usage: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glBufferData(target, data, usage)
        DriverTypeGL.GLES -> GLES.glBufferData(target, data, usage)
    }
}

actual inline fun glBufferData(target: Int, data: IntArray, usage: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glBufferData(target, data, usage)
        DriverTypeGL.GLES -> GLES.glBufferData(target, data, usage)
    }
}

actual inline fun glBlendEquationSeparatei(
    buf: Int,
    modeRGB: Int,
    modeAlpha: Int
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glBlendEquationSeparatei(buf, modeRGB, modeAlpha)
        DriverTypeGL.GLES -> GLES.glBlendEquationSeparatei(buf, modeRGB, modeAlpha)
    }
}

actual inline fun glReadPixels(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    format: Int,
    type: Int,
    pixels: ByteBuffer
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glReadPixels(x, y, width, height, format, type, pixels)
        DriverTypeGL.GLES -> GLES.glReadPixels(x, y, width, height, format, type, pixels)
    }
}

actual inline fun glGetTexParameterf(target: Int, pname: Int): Float {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetTexParameterf(target, pname)
        DriverTypeGL.GLES -> GLES.glGetTexParameterf(target, pname)
    }
}

actual inline fun glMapBufferRange(
    target: Int,
    offset: Long,
    length: Long,
    access: Int
): ByteBuffer? {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glMapBufferRange(target, offset, length, access)
        DriverTypeGL.GLES -> GLES.glMapBufferRange(target, offset, length, access)
    }
}

actual inline fun glUnmapBuffer(target: Int): Boolean {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glUnmapBuffer(target)
        DriverTypeGL.GLES -> GLES.glUnmapBuffer(target)
    }
}