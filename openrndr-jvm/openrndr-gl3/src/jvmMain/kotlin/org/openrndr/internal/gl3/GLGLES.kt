@file:Suppress("NOTHING_TO_INLINE")

package org.openrndr.internal.gl3

import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memASCII
import org.lwjgl.system.NativeType
import org.openrndr.internal.Driver
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import org.lwjgl.opengl.GL45C as GL
import org.lwjgl.opengles.GLES32 as GLES

var driverType = DriverGL3Configuration.driverType

inline fun glEnable(target: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glEnable(target)
        DriverTypeGL.GLES -> GLES.glEnable(target)
    }
}

// --- [ glDisable ] ---
inline fun glDisable(@NativeType("GLenum") target: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glDisable(target)
        DriverTypeGL.GLES -> GLES.glDisable(target)
    }
}

inline fun glGenBuffers(): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGenBuffers()
        DriverTypeGL.GLES -> GLES.glGenBuffers()
    }
}

inline fun glBindBuffer(target: Int, buffer: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glBindBuffer(target, buffer)
        DriverTypeGL.GLES -> GLES.glBindBuffer(target, buffer)
    }
}

inline fun glBufferData(target: Int, size: Long, usage: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glBufferData(target, size, usage)
        DriverTypeGL.GLES -> GLES.glBufferData(target, size, usage)
    }
}

inline fun glGetIntegerv(pname: Int, params: IntArray) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetIntegerv(pname, params)
        DriverTypeGL.GLES -> GLES.glGetIntegerv(pname, params)
    }
}

inline fun glGetInteger(pname: Int): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetInteger(pname)
        DriverTypeGL.GLES -> GLES.glGetInteger(pname)
    }
}

inline fun glGetString(pname: Int): String? {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetString(pname)
        DriverTypeGL.GLES -> GLES.glGetString(pname)
    }
}

inline fun glBufferSubData(target: Int, offset: Long, data: ByteBuffer) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glBufferSubData(target, offset, data)
        DriverTypeGL.GLES -> GLES.glBufferSubData(target, offset, data)
    }
}

// --- [ glBindFramebuffer ] ---
inline fun glBindFramebuffer(target: Int, framebuffer: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glBindFramebuffer(target, framebuffer)
        DriverTypeGL.GLES -> GLES.glBindFramebuffer(target, framebuffer)
    }
}

// --- [ glScissor ] ---
fun glScissor(
    @NativeType("GLint") x: Int,
    @NativeType("GLint") y: Int,
    @NativeType("GLsizei") width: Int,
    @NativeType("GLsizei") height: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glScissor(x, y, width, height)
        DriverTypeGL.GLES -> GLES.glScissor(x, y, width, height)
    }
}

// --- [ glViewport ] ---
inline fun glViewport(
    @NativeType("GLint") x: Int,
    @NativeType("GLint") y: Int,
    @NativeType("GLsizei") w: Int,
    @NativeType("GLsizei") h: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glViewport(x, y, w, h)
        DriverTypeGL.GLES -> GLES.glViewport(x, y, w, h)
    }
}

// --- [ glDepthMask ] ---
inline fun glDepthMask(@NativeType("GLboolean") flag: Boolean) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glDepthMask(flag)
        DriverTypeGL.GLES -> GLES.glDepthMask(flag)
    }
}

// --- [ glClearColor ] ---
inline fun glClearColor(
    @NativeType("GLfloat") red: Float,
    @NativeType("GLfloat") green: Float,
    @NativeType("GLfloat") blue: Float,
    @NativeType("GLfloat") alpha: Float
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glClearColor(red, green, blue, alpha)
        DriverTypeGL.GLES -> GLES.glClearColor(red, green, blue, alpha)
    }
}


// --- [ glClear ] ---
inline fun glClear(@NativeType("GLbitfield") mask: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glClear(mask)
        DriverTypeGL.GLES -> GLES.glClear(mask)
    }
}

inline fun glGenFramebuffers(): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGenFramebuffers()
        DriverTypeGL.GLES -> GLES.glGenFramebuffers()
    }
}

inline fun glDrawBuffers(bufs: IntArray) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glDrawBuffers(bufs)
        DriverTypeGL.GLES -> GLES.glDrawBuffers(bufs)
    }
}

// --- [ glFramebufferTexture ] ---
inline fun glFramebufferTexture(
    @NativeType("GLenum") target: Int,
    @NativeType("GLenum") attachment: Int,
    @NativeType("GLuint") texture: Int,
    @NativeType("GLint") level: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glFramebufferTexture(target, attachment, texture, level)
        DriverTypeGL.GLES -> GLES.glFramebufferTexture(target, attachment, texture, level)
    }
}

// --- [ glBlendEquationi ] ---
inline fun glBlendEquationi(@NativeType("GLuint") buf: Int, @NativeType("GLenum") mode: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glBlendEquationi(buf, mode)
        DriverTypeGL.GLES -> GLES.glBlendEquationi(buf, mode)
    }
}


// --- [ glBlendFunci ] ---
inline fun glBlendFunci(
    @NativeType("GLuint") buf: Int,
    @NativeType("GLenum") sfactor: Int,
    @NativeType("GLenum") dfactor: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glBlendFunci(buf, sfactor, dfactor)
        DriverTypeGL.GLES -> GLES.glBlendFunci(buf, sfactor, dfactor)

    }
}


// --- [ glBindVertexArray ] ---
inline fun glBindVertexArray(@NativeType("GLuint") array: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glBindVertexArray(array)
        DriverTypeGL.GLES -> GLES.glBindVertexArray(array)
    }
}

// --- [ glClearDepth ] ---
inline fun glClearDepth(@NativeType("GLdouble") depth: Double) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glClearDepth(depth)
        DriverTypeGL.GLES -> GLES.glClearDepthf(depth.toFloat())
    }
}

// --- [ glCreateShader ] ---
inline fun glCreateShader(@NativeType("GLenum") type: Int): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glCreateShader(type)
        DriverTypeGL.GLES -> GLES.glCreateShader(type)
    }
}

inline fun glShaderSource(
    @NativeType("GLuint") shader: Int,
    @NativeType("GLchar const * const *") string: CharSequence
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glShaderSource(shader, string)
        DriverTypeGL.GLES -> GLES.glShaderSource(shader, string)
    }
}


// --- [ glCompileShader ] ---
inline fun glCompileShader(@NativeType("GLuint") shader: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glCompileShader(shader)
        DriverTypeGL.GLES -> GLES.glCompileShader(shader)
    }
}

fun glGetShaderiv(
    @NativeType("GLuint") shader: Int,
    @NativeType("GLenum") pname: Int,
    @NativeType("GLint *") params: IntArray
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetShaderiv(shader, pname, params)
        DriverTypeGL.GLES -> GLES.glGetShaderiv(shader, pname, params)
    }
}

fun glGetProgramiv(
    @NativeType("GLuint") program: Int,
    @NativeType("GLenum") pname: Int,
    @NativeType("GLint *") params: IntArray
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetProgramiv(program, pname, params)
        DriverTypeGL.GLES -> GLES.glGetProgramiv(program, pname, params)
    }
}

fun glGetShaderInfoLog(
    @NativeType("GLuint") shader: Int,
    @NativeType("GLsizei *") length: IntArray?,
    @NativeType("GLchar *") infoLog: ByteBuffer
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetShaderInfoLog(shader, length, infoLog)
        DriverTypeGL.GLES -> GLES.glGetShaderInfoLog(shader, length, infoLog)
    }
}

// --- [ glCreateProgram ] ---
fun glCreateProgram(): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glCreateProgram()
        DriverTypeGL.GLES -> GLES.glCreateProgram()
    }
}

// --- [ glAttachShader ] ---
fun glAttachShader(@NativeType("GLuint") program: Int, @NativeType("GLuint") shader: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glAttachShader(program, shader)
        DriverTypeGL.GLES -> GLES.glAttachShader(program, shader)
    }
}

// --- [ glLinkProgram ] ---
fun glLinkProgram(@NativeType("GLuint") program: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glLinkProgram(program)
        DriverTypeGL.GLES -> GLES.glLinkProgram(program)
    }
}

// --- [ glFinish ] ---
inline fun glFinish() {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glFinish()
        DriverTypeGL.GLES -> GLES.glFinish()
    }
}

// --- [ glUseProgram ] ---
inline fun glUseProgram(@NativeType("GLuint") program: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glUseProgram(program)
        DriverTypeGL.GLES -> GLES.glUseProgram(program)
    }
}

inline fun glGetUniformBlockIndex(
    @NativeType("GLuint") program: Int,
    @NativeType("GLchar const *") uniformBlockName: CharSequence
): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetUniformBlockIndex(program, uniformBlockName)
        DriverTypeGL.GLES -> GLES.glGetUniformBlockIndex(program, uniformBlockName)
    }
}

inline fun glGetAttribLocation(
    @NativeType("GLuint") program: Int,
    @NativeType("GLchar const *") name: CharSequence
): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetAttribLocation(program, name)
        DriverTypeGL.GLES -> GLES.glGetAttribLocation(program, name)
    }
}

// --- [ glDeleteProgram ] ---
inline fun glDeleteProgram(@NativeType("GLuint") program: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glDeleteProgram(program)
        DriverTypeGL.GLES -> GLES.glDeleteProgram(program)
    }
}

// --- [ glUniformBlockBinding ] ---
inline fun glUniformBlockBinding(
    @NativeType("GLuint") program: Int,
    @NativeType("GLuint") uniformBlockIndex: Int,
    @NativeType("GLuint") uniformBlockBinding: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding)
        DriverTypeGL.GLES -> GLES.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding)
    }
}

fun glGetActiveUniformBlockiv(
    @NativeType("GLuint") program: Int,
    @NativeType("GLuint") uniformBlockIndex: Int,
    @NativeType("GLenum") pname: Int,
    @NativeType("GLint *") params: IntBuffer
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, params)
        DriverTypeGL.GLES -> GLES.glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, params)
    }
}

fun glGetActiveUniformsiv(
    @NativeType("GLuint") program: Int,
    @NativeType("GLuint const *") uniformIndices: IntBuffer,
    @NativeType("GLenum") pname: Int,
    @NativeType("GLint *") params: IntBuffer
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetActiveUniformsiv(program, uniformIndices, pname, params)
        DriverTypeGL.GLES -> GLES.glGetActiveUniformsiv(program, uniformIndices, pname, params)
    }
}

fun glGetActiveUniformName(
    @NativeType("GLuint") program: Int,
    @NativeType("GLuint") uniformIndex: Int,
    @NativeType("GLsizei") bufSize: Int
): String {
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

// --- [ glGetError ] ---
fun glGetError(): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetError()
        DriverTypeGL.GLES -> GLES.glGetError()
    }
}

// --- [ glBindBufferBase ] ---
fun glBindBufferBase(
    @NativeType("GLenum") target: Int,
    @NativeType("GLuint") index: Int,
    @NativeType("GLuint") buffer: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glBindBufferBase(target, index, buffer)
        DriverTypeGL.GLES -> GLES.glBindBufferBase(target, index, buffer)
    }
}

// --- [ glColorMask ] ---
inline fun glColorMask(
    @NativeType("GLboolean") red: Boolean,
    @NativeType("GLboolean") green: Boolean,
    @NativeType("GLboolean") blue: Boolean,
    @NativeType("GLboolean") alpha: Boolean
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glColorMask(red, green, blue, alpha)
        DriverTypeGL.GLES -> GLES.glColorMask(red, green, blue, alpha)
    }
}


// --- [ glBlendEquation ] ---
inline fun glBlendEquation(@NativeType("GLenum") mode: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glBlendEquation(mode)
        DriverTypeGL.GLES -> GLES.glBlendEquation(mode)
    }
}

// --- [ glBlendFunc ] ---
inline fun glBlendFunc(@NativeType("GLenum") sfactor: Int, @NativeType("GLenum") dfactor: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glBlendFunc(sfactor, dfactor)
        DriverTypeGL.GLES -> GLES.glBlendFunc(sfactor, dfactor)
    }
}

// --- [ glBlendEquationSeparate ] ---
inline fun glBlendEquationSeparate(@NativeType("GLenum") modeRGB: Int, @NativeType("GLenum") modeAlpha: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glBlendEquationSeparate(modeRGB, modeAlpha)
        DriverTypeGL.GLES -> GLES.glBlendEquationSeparate(modeRGB, modeAlpha)
    }
}

// --- [ glBlendFuncSeparate ] ---
inline fun glBlendFuncSeparate(
    @NativeType("GLenum") sfactorRGB: Int,
    @NativeType("GLenum") dfactorRGB: Int,
    @NativeType("GLenum") sfactorAlpha: Int,
    @NativeType("GLenum") dfactorAlpha: Int
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha)
        DriverTypeGL.GLES -> GLES.glBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha)
    }
}

// --- [ glDepthFunc ] ---
inline fun glDepthFunc(@NativeType("GLenum") func: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glDepthFunc(func)
        DriverTypeGL.GLES -> GLES.glDepthFunc(func)
    }
}

// --- [ glCullFace ] ---
inline fun glCullFace(@NativeType("GLenum") mode: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glCullFace(mode)
        DriverTypeGL.GLES -> GLES.glCullFace(mode)
    }
}

inline fun glGenVertexArrays(@NativeType("GLuint *") arrays: IntArray) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glGenVertexArrays(arrays)
        DriverTypeGL.GLES -> GLES.glGenVertexArrays(arrays)
    }
}

// --- [ glDrawArraysInstanced ] ---
inline fun glDrawArraysInstanced(
    @NativeType("GLenum") mode: Int,
    @NativeType("GLint") first: Int,
    @NativeType("GLsizei") count: Int,
    @NativeType("GLsizei") primcount: Int
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glDrawArraysInstanced(mode, first, count, primcount)
        DriverTypeGL.GLES -> GLES.glDrawArraysInstanced(mode, first, count, primcount)
    }
}

fun glDrawElements(
    @NativeType("GLenum") mode: Int,
    @NativeType("GLsizei") count: Int,
    @NativeType("GLenum") type: Int,
    @NativeType("void const *") indices: Long
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glDrawElements(mode, count, type, indices)
        DriverTypeGL.GLES -> GLES.glDrawElements(mode, count, type, indices)
    }
}

fun glDrawElementsInstanced(
    @NativeType("GLenum") mode: Int,
    @NativeType("GLsizei") count: Int,
    @NativeType("GLenum") type: Int,
    @NativeType("void const *") indices: Long,
    @NativeType("GLsizei") primcount: Int
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glDrawElementsInstanced(mode, count, type, indices, primcount)
        DriverTypeGL.GLES -> GLES.glDrawElementsInstanced(mode, count, type, indices, primcount)
    }
}


// --- [ glEnableVertexAttribArray ] ---
inline fun glEnableVertexAttribArray(@NativeType("GLuint") index: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glEnableVertexAttribArray(index)
        DriverTypeGL.GLES -> GLES.glEnableVertexAttribArray(index)
    }
}

fun glVertexAttribPointer(
    @NativeType("GLuint") index: Int,
    @NativeType("GLint") size: Int,
    @NativeType("GLenum") type: Int,
    @NativeType("GLboolean") normalized: Boolean,
    @NativeType("GLsizei") stride: Int,
    @NativeType("void const *") pointer: Long
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glVertexAttribPointer(index, size, type, normalized, stride, pointer)
        DriverTypeGL.GLES -> GLES.glVertexAttribPointer(index, size, type, normalized, stride, pointer)
    }
}

// --- [ glVertexAttribDivisor ] ---
fun glVertexAttribDivisor(@NativeType("GLuint") index: Int, @NativeType("GLuint") divisor: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glVertexAttribDivisor(index, divisor)
        DriverTypeGL.GLES -> GLES.glVertexAttribDivisor(index, divisor)
    }
}

fun glDeleteBuffers(@NativeType("GLuint const *") buffer: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glDeleteBuffers(buffer)
        DriverTypeGL.GLES -> GLES.glDeleteBuffers(buffer)
    }
}

fun glDeleteVertexArrays(@NativeType("GLuint const *") array: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glDeleteVertexArrays(array)
        DriverTypeGL.GLES -> GLES.glDeleteVertexArrays(array)
    }
}

fun glGetUniformLocation(@NativeType("GLuint") program: Int, @NativeType("GLchar const *") name: CharSequence): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetUniformLocation(program, name)
        DriverTypeGL.GLES -> GLES.glGetUniformLocation(program, name)
    }
}


// --- [ glProgramUniform4f ] ---
inline fun glProgramUniform4f(
    @NativeType("GLuint") program: Int,
    @NativeType("GLint") location: Int,
    @NativeType("GLfloat") x: Float,
    @NativeType("GLfloat") y: Float,
    @NativeType("GLfloat") z: Float,
    @NativeType("GLfloat") w: Float
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform4f(program, location, x, y, z, w)
        DriverTypeGL.GLES -> GLES.glProgramUniform4f(program, location, x, y, z, w)
    }
}

// --- [ glUniform4f ] ---
inline fun glUniform4f(
    @NativeType("GLint") location: Int,
    @NativeType("GLfloat") v0: Float,
    @NativeType("GLfloat") v1: Float,
    @NativeType("GLfloat") v2: Float,
    @NativeType("GLfloat") v3: Float
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform4f(location, v0, v1, v2, v3)
        DriverTypeGL.GLES -> GLES.glUniform4f(location, v0, v1, v2, v3)
    }
}

// --- [ glProgramUniform3f ] ---
inline fun glProgramUniform3f(
    @NativeType("GLuint") program: Int,
    @NativeType("GLint") location: Int,
    @NativeType("GLfloat") x: Float,
    @NativeType("GLfloat") y: Float,
    @NativeType("GLfloat") z: Float
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform3f(program, location, x, y, z)
        DriverTypeGL.GLES -> GLES.glProgramUniform3f(program, location, x, y, z)
    }
}


// --- [ glUniform3f ] ---
inline fun glUniform3f(
    @NativeType("GLint") location: Int,
    @NativeType("GLfloat") v0: Float,
    @NativeType("GLfloat") v1: Float,
    @NativeType("GLfloat") v2: Float
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform3f(location, v0, v1, v2)
        DriverTypeGL.GLES -> GLES.glUniform3f(location, v0, v1, v2)
    }
}

// --- [ glProgramUniform2f ] ---
inline fun glProgramUniform2f(
    @NativeType("GLuint") program: Int,
    @NativeType("GLint") location: Int,
    @NativeType("GLfloat") x: Float,
    @NativeType("GLfloat") y: Float
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform2f(program, location, x, y)
        DriverTypeGL.GLES -> GLES.glProgramUniform2f(program, location, x, y)
    }
}

// --- [ glUniform2f ] ---
inline fun glUniform2f(
    @NativeType("GLint") location: Int,
    @NativeType("GLfloat") v0: Float,
    @NativeType("GLfloat") v1: Float
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform2f(location, v0, v1)
        DriverTypeGL.GLES -> GLES.glUniform2f(location, v0, v1)
    }
}

// --- [ glProgramUniform1i ] ---
inline fun glProgramUniform1i(
    @NativeType("GLuint") program: Int,
    @NativeType("GLint") location: Int,
    @NativeType("GLint") x: Int
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform1i(program, location, x)
        DriverTypeGL.GLES -> GLES.glProgramUniform1i(program, location, x)
    }
}

// --- [ glUniform1i ] ---
inline fun glUniform1i(@NativeType("GLint") location: Int, @NativeType("GLint") v0: Int) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform1i(location, v0)
        DriverTypeGL.GLES -> GLES.glUniform1i(location, v0)
    }
}

// --- [ glProgramUniform1f ] ---
inline fun glProgramUniform1f(
    @NativeType("GLuint") program: Int,
    @NativeType("GLint") location: Int,
    @NativeType("GLfloat") x: Float
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform1f(program, location, x)
        DriverTypeGL.GLES -> GLES.glProgramUniform1f(program, location, x)
    }
}

inline fun glUniform1f(@NativeType("GLint") location: Int, @NativeType("GLint") v0: Float) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform1f(location, v0)
        DriverTypeGL.GLES -> GLES.glUniform1f(location, v0)
    }
}

fun glProgramUniformMatrix3fv(
    @NativeType("GLuint") program: Int,
    @NativeType("GLint") location: Int,
    @NativeType("GLboolean") transpose: Boolean,
    @NativeType("GLfloat const *") value: FloatArray
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniformMatrix3fv(program, location, transpose, value)
        DriverTypeGL.GLES -> GLES.glProgramUniformMatrix3fv(program, location, transpose, value)
    }
}

fun glUniformMatrix3fv(
    @NativeType("GLint") location: Int,
    @NativeType("GLboolean") transpose: Boolean,
    @NativeType("GLfloat const *") value: FloatArray
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniformMatrix3fv(location, transpose, value)
        DriverTypeGL.GLES -> GLES.glUniformMatrix3fv(location, transpose, value)
    }
}

inline fun glProgramUniformMatrix4fv(
    @NativeType("GLuint") program: Int,
    @NativeType("GLint") location: Int,
    @NativeType("GLboolean") transpose: Boolean,
    @NativeType("GLfloat const *") value: FloatArray
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniformMatrix4fv(program, location, transpose, value)
        DriverTypeGL.GLES -> GLES.glProgramUniformMatrix4fv(program, location, transpose, value)
    }
}

inline fun glUniformMatrix4fv(
    @NativeType("GLint") location: Int,
    @NativeType("GLboolean") transpose: Boolean,
    @NativeType("GLfloat const *") value: FloatArray
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniformMatrix4fv(location, transpose, value)
        DriverTypeGL.GLES -> GLES.glUniformMatrix4fv(location, transpose, value)
    }
}

fun glProgramUniform4iv(
    @NativeType("GLuint") program: Int,
    @NativeType("GLint") location: Int,
    @NativeType("GLint const *") value: IntArray
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform4iv(program, location, value)
        DriverTypeGL.GLES -> GLES.glProgramUniform4iv(program, location, value)
    }
}

fun glUniform4iv(@NativeType("GLint") location: Int, @NativeType("GLint const *") value: IntArray) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform4iv(location, value)
        DriverTypeGL.GLES -> GLES.glUniform4iv(location, value)
    }
}

inline fun glProgramUniform3iv(
    @NativeType("GLuint") program: Int,
    @NativeType("GLint") location: Int,
    @NativeType("GLint const *") value: IntArray
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform3iv(program, location, value)
        DriverTypeGL.GLES -> GLES.glProgramUniform3iv(program, location, value)
    }
}


inline fun glUniform3iv(@NativeType("GLint") location: Int, @NativeType("GLint const *") value: IntArray) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform3iv(location, value)
        DriverTypeGL.GLES -> GLES.glUniform3iv(location, value)
    }
}

fun glProgramUniform2iv(
    @NativeType("GLuint") program: Int,
    @NativeType("GLint") location: Int,
    @NativeType("GLint const *") value: IntArray
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform2iv(program, location, value)
        DriverTypeGL.GLES -> GLES.glProgramUniform2iv(program, location, value)
    }
}

fun glUniform2iv(@NativeType("GLint") location: Int, @NativeType("GLint const *") value: IntArray) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform2iv(location, value)
        DriverTypeGL.GLES -> GLES.glUniform2iv(location, value)
    }
}

fun glProgramUniform2fv(
    @NativeType("GLuint") program: Int,
    @NativeType("GLint") location: Int,
    @NativeType("GLfloat const *") value: FloatArray
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform2fv(program, location, value)
        DriverTypeGL.GLES -> GLES.glProgramUniform2fv(program, location, value)
    }
}

fun glUniform2fv(@NativeType("GLint") location: Int, @NativeType("GLfloat const *") value: FloatArray) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform2fv(location, value)
        DriverTypeGL.GLES -> GLES.glUniform2fv(location, value)
    }
}

fun glProgramUniform3fv(
    @NativeType("GLuint") program: Int,
    @NativeType("GLint") location: Int,
    @NativeType("GLfloat const *") value: FloatArray
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform3fv(program, location, value)
        DriverTypeGL.GLES -> GLES.glProgramUniform3fv(program, location, value)
    }
}

fun glUniform3fv(@NativeType("GLint") location: Int, @NativeType("GLfloat const *") value: FloatArray) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform3fv(location, value)
        DriverTypeGL.GLES -> GLES.glUniform3fv(location, value)
    }
}

fun glProgramUniform4fv(
    @NativeType("GLuint") program: Int,
    @NativeType("GLint") location: Int,
    @NativeType("GLfloat const *") value: FloatArray
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform4fv(program, location, value)
        DriverTypeGL.GLES -> GLES.glProgramUniform4fv(program, location, value)
    }
}


fun glUniform4fv(@NativeType("GLint") location: Int, @NativeType("GLfloat const *") value: FloatArray) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform4fv(location, value)
        DriverTypeGL.GLES -> GLES.glUniform4fv(location, value)
    }
}

fun glProgramUniform1fv(
    @NativeType("GLuint") program: Int,
    @NativeType("GLint") location: Int,
    @NativeType("GLfloat const *") value: FloatArray
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform1fv(program, location, value)
        DriverTypeGL.GLES -> GLES.glProgramUniform1fv(program, location, value)
    }
}

fun glUniform1fv(@NativeType("GLint") location: Int, @NativeType("GLfloat const *") value: FloatArray) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform1fv(location, value)
        DriverTypeGL.GLES -> GLES.glUniform1fv(location, value)
    }
}

fun glProgramUniform1iv(
    @NativeType("GLuint") program: Int,
    @NativeType("GLint") location: Int,
    @NativeType("GLint const *") value: IntArray
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniform1iv(program, location, value)
        DriverTypeGL.GLES -> GLES.glProgramUniform1iv(program, location, value)
    }
}

fun glUniform1iv(@NativeType("GLint") location: Int, @NativeType("GLint const *") value: IntArray) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniform1iv(location, value)
        DriverTypeGL.GLES -> GLES.glUniform1iv(location, value)
    }
}

fun glProgramUniformMatrix4fv(
    @NativeType("GLuint") program: Int,
    @NativeType("GLint") location: Int,
    @NativeType("GLboolean") transpose: Boolean,
    @NativeType("GLfloat const *") value: FloatBuffer
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glProgramUniformMatrix4fv(program, location, transpose, value)
        DriverTypeGL.GLES -> GLES.glProgramUniformMatrix4fv(program, location, transpose, value)
    }
}

fun glUniformMatrix4fv(
    @NativeType("GLint") location: Int,
    @NativeType("GLboolean") transpose: Boolean,
    @NativeType("GLfloat const *") value: FloatBuffer
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glUniformMatrix4fv(location, transpose, value)
        DriverTypeGL.GLES -> GLES.glUniformMatrix4fv(location, transpose, value)
    }
}

fun glGetActiveUniform(
    @NativeType("GLuint") program: Int,
    @NativeType("GLuint") index: Int,
    @NativeType("GLsizei *") length: IntBuffer,
    @NativeType("GLint *") size: IntBuffer,
    @NativeType("GLenum *") type: IntBuffer,
    @NativeType("GLchar *") name: ByteBuffer
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glGetActiveUniform(program, index, length, size, type, name)
        DriverTypeGL.GLES -> GLES.glGetActiveUniform(program, index, length, size, type, name)
    }
}

// --- [ glTexParameteri ] ---
fun glTexParameteri(
    @NativeType("GLenum") target: Int,
    @NativeType("GLenum") pname: Int,
    @NativeType("GLint") param: Int
) {
    when (driverType) {
        DriverTypeGL.GL -> GL.glTexParameteri(target, pname, param)
        DriverTypeGL.GLES -> GLES.glTexParameteri(target, pname, param)
    }
}

fun glGenTextures(): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGenTextures()
        DriverTypeGL.GLES -> GLES.glGenTextures()
    }
}

// --- [ glBindTexture ] ---
fun glBindTexture(@NativeType("GLenum") target: Int, @NativeType("GLuint") texture: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glBindTexture(target, texture)
        DriverTypeGL.GLES -> GLES.glBindTexture(target, texture)
    }
}

fun glTexImage3D(
    @NativeType("GLenum") target: Int,
    @NativeType("GLint") level: Int,
    @NativeType("GLint") internalformat: Int,
    @NativeType("GLsizei") width: Int,
    @NativeType("GLsizei") height: Int,
    @NativeType("GLsizei") depth: Int,
    @NativeType("GLint") border: Int,
    @NativeType("GLenum") format: Int,
    @NativeType("GLenum") type: Int,
    @NativeType("void const *") pixels: ByteBuffer?
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

// --- [ glActiveTexture ] ---
inline fun glActiveTexture(@NativeType("GLenum") texture: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glActiveTexture(texture)
        DriverTypeGL.GLES -> GLES.glActiveTexture(texture)
    }
}

fun glDeleteTextures(@NativeType("GLuint const *") texture: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glDeleteTextures(texture)
        DriverTypeGL.GLES -> GLES.glDeleteTextures(texture)
    }
}

// --- [ glReadBuffer ] ---
inline fun glReadBuffer(@NativeType("GLenum") src: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glReadBuffer(src)
        DriverTypeGL.GLES -> GLES.glReadBuffer(src)
    }
}

// --- [ glGenerateMipmap ] ---
fun glGenerateMipmap(@NativeType("GLenum") target: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGenerateMipmap(target)
        DriverTypeGL.GLES -> GLES.glGenerateMipmap(target)
    }
}

fun glTexSubImage3D(
    @NativeType("GLenum") target: Int,
    @NativeType("GLint") level: Int,
    @NativeType("GLint") xoffset: Int,
    @NativeType("GLint") yoffset: Int,
    @NativeType("GLint") zoffset: Int,
    @NativeType("GLsizei") width: Int,
    @NativeType("GLsizei") height: Int,
    @NativeType("GLsizei") depth: Int,
    @NativeType("GLenum") format: Int,
    @NativeType("GLenum") type: Int,
    @NativeType("void const *") pixels: ByteBuffer
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

// --- [ glCopyTexSubImage2D ] ---
fun glCopyTexSubImage2D(
    @NativeType("GLenum") target: Int,
    @NativeType("GLint") level: Int,
    @NativeType("GLint") xoffset: Int,
    @NativeType("GLint") yoffset: Int,
    @NativeType("GLint") x: Int,
    @NativeType("GLint") y: Int,
    @NativeType("GLsizei") width: Int,
    @NativeType("GLsizei") height: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height)
        DriverTypeGL.GLES -> GLES.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height)
    }
}

fun glCompressedTexSubImage3D(
    @NativeType("GLenum") target: Int,
    @NativeType("GLint") level: Int,
    @NativeType("GLint") xoffset: Int,
    @NativeType("GLint") yoffset: Int,
    @NativeType("GLint") zoffset: Int,
    @NativeType("GLsizei") width: Int,
    @NativeType("GLsizei") height: Int,
    @NativeType("GLsizei") depth: Int,
    @NativeType("GLenum") format: Int,
    @NativeType("void const *") data: ByteBuffer
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
fun glCopyTexSubImage3D(
    @NativeType("GLenum") target: Int,
    @NativeType("GLint") level: Int,
    @NativeType("GLint") xoffset: Int,
    @NativeType("GLint") yoffset: Int,
    @NativeType("GLint") zoffset: Int,
    @NativeType("GLint") x: Int,
    @NativeType("GLint") y: Int,
    @NativeType("GLsizei") width: Int,
    @NativeType("GLsizei") height: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glCopyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height)
        DriverTypeGL.GLES -> GLES.glCopyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height)
    }
}


fun glTexImage2D(
    @NativeType("GLenum") target: Int,
    @NativeType("GLint") level: Int,
    @NativeType("GLint") internalformat: Int,
    @NativeType("GLsizei") width: Int,
    @NativeType("GLsizei") height: Int,
    @NativeType("GLint") border: Int,
    @NativeType("GLenum") format: Int,
    @NativeType("GLenum") type: Int,
    @NativeType("void const *") pixels: ByteBuffer?
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels)
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

// --- [ glBlitFramebuffer ] ---
fun glBlitFramebuffer(
    @NativeType("GLint") srcX0: Int,
    @NativeType("GLint") srcY0: Int,
    @NativeType("GLint") srcX1: Int,
    @NativeType("GLint") srcY1: Int,
    @NativeType("GLint") dstX0: Int,
    @NativeType("GLint") dstY0: Int,
    @NativeType("GLint") dstX1: Int,
    @NativeType("GLint") dstY1: Int,
    @NativeType("GLbitfield") mask: Int,
    @NativeType("GLenum") filter: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter)
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

// --- [ glCopyImageSubData ] ---
fun glCopyImageSubData(
    @NativeType("GLuint") srcName: Int,
    @NativeType("GLenum") srcTarget: Int,
    @NativeType("GLint") srcLevel: Int,
    @NativeType("GLint") srcX: Int,
    @NativeType("GLint") srcY: Int,
    @NativeType("GLint") srcZ: Int,
    @NativeType("GLuint") dstName: Int,
    @NativeType("GLenum") dstTarget: Int,
    @NativeType("GLint") dstLevel: Int,
    @NativeType("GLint") dstX: Int,
    @NativeType("GLint") dstY: Int,
    @NativeType("GLint") dstZ: Int,
    @NativeType("GLsizei") srcWidth: Int,
    @NativeType("GLsizei") srcHeight: Int,
    @NativeType("GLsizei") srcDepth: Int
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

// --- [ glTexParameterf ] ---
inline fun glTexParameterf(
    @NativeType("GLenum") target: Int,
    @NativeType("GLenum") pname: Int,
    @NativeType("GLfloat") param: Float
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glTexParameterf(target, pname, param)
        DriverTypeGL.GLES -> GLES.glTexParameterf(target, pname, param)
    }
}

// --- [ glPixelStorei ] ---
fun glPixelStorei(@NativeType("GLenum") pname: Int, @NativeType("GLint") param: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glPixelStorei(pname, param)
        DriverTypeGL.GLES -> GLES.glPixelStorei(pname, param)
    }
}


fun glTexSubImage2D(
    @NativeType("GLenum") target: Int,
    @NativeType("GLint") level: Int,
    @NativeType("GLint") xoffset: Int,
    @NativeType("GLint") yoffset: Int,
    @NativeType("GLsizei") width: Int,
    @NativeType("GLsizei") height: Int,
    @NativeType("GLenum") format: Int,
    @NativeType("GLenum") type: Int,
    @NativeType("void const *") pixels: ByteBuffer
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels)
        DriverTypeGL.GLES -> GLES.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels)
    }
}

fun glCompressedTexSubImage2D(
    @NativeType("GLenum") target: Int,
    @NativeType("GLint") level: Int,
    @NativeType("GLint") xoffset: Int,
    @NativeType("GLint") yoffset: Int,
    @NativeType("GLsizei") width: Int,
    @NativeType("GLsizei") height: Int,
    @NativeType("GLenum") format: Int,
    @NativeType("void const *") data: ByteBuffer
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, data)
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


// --- [ glFramebufferTexture2D ] ---
fun glFramebufferTexture2D(
    @NativeType("GLenum") target: Int,
    @NativeType("GLenum") attachment: Int,
    @NativeType("GLenum") textarget: Int,
    @NativeType("GLuint") texture: Int,
    @NativeType("GLint") level: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glFramebufferTexture2D(target, attachment, textarget, texture, level)
        DriverTypeGL.GLES -> GLES.glFramebufferTexture2D(target, attachment, textarget, texture, level)
    }
}

// --- [ glFramebufferTextureLayer ] ---
fun glFramebufferTextureLayer(
    @NativeType("GLenum") target: Int,
    @NativeType("GLenum") attachment: Int,
    @NativeType("GLuint") texture: Int,
    @NativeType("GLint") level: Int,
    @NativeType("GLint") layer: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glFramebufferTextureLayer(target, attachment, texture, level, layer)
        DriverTypeGL.GLES -> GLES.glFramebufferTextureLayer(target, attachment, texture, level, layer)
    }

}

fun glGetBoolean(@NativeType("GLenum") pname: Int): Boolean {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetBoolean(pname)
        DriverTypeGL.GLES -> GLES.glGetBoolean(pname)
    }
}

fun glGetTexLevelParameteri(
    @NativeType("GLenum") target: Int,
    @NativeType("GLint") level: Int,
    @NativeType("GLenum") pname: Int
): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetTexLevelParameteri(target, level, pname)
        DriverTypeGL.GLES -> {
            val values = IntArray(4)
            if (Driver.glVersion >= DriverVersionGL.GLES_VERSION_3_1) {
                values[0] = GLES.glGetTexLevelParameteri(target, level, pname)
            } else {
                (Driver.instance as DriverGL3).angleExtensions?.glTexLevelParameterivANGLE(target, level, pname, values)
            }
            values[0]
        }
    }
}

fun glGetTexParameteri(@NativeType("GLenum") target: Int, @NativeType("GLenum") pname: Int): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glGetTexParameteri(target, pname)
        DriverTypeGL.GLES -> GLES.glGetTexParameteri(target, pname)
    }
}

fun glDeleteFramebuffers(@NativeType("GLuint const *") framebuffer: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glDeleteFramebuffers(framebuffer)
        DriverTypeGL.GLES -> GLES.glDeleteFramebuffers(framebuffer)
    }
}

// --- [ glStencilFuncSeparate ] ---
fun glStencilFuncSeparate(
    @NativeType("GLenum") face: Int,
    @NativeType("GLenum") func: Int,
    @NativeType("GLint") ref: Int,
    @NativeType("GLuint") mask: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glStencilFuncSeparate(face, func, ref, mask)
        DriverTypeGL.GLES -> GLES.glStencilFuncSeparate(face, func, ref, mask)
    }
}

// --- [ glStencilOpSeparate ] ---
fun glStencilOpSeparate(
    @NativeType("GLenum") face: Int,
    @NativeType("GLenum") sfail: Int,
    @NativeType("GLenum") dpfail: Int,
    @NativeType("GLenum") dppass: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glStencilOpSeparate(face, sfail, dpfail, dppass)
        DriverTypeGL.GLES -> GLES.glStencilOpSeparate(face, sfail, dpfail, dppass)
    }
}

// --- [ glStencilMaskSeparate ] ---
fun glStencilMaskSeparate(@NativeType("GLenum") face: Int, @NativeType("GLuint") mask: Int) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glStencilMaskSeparate(face, mask)
        DriverTypeGL.GLES -> GLES.glStencilMaskSeparate(face, mask)
    }
}

// --- [ glDrawArrays ] ---
fun glDrawArrays(
    @NativeType("GLenum") mode: Int,
    @NativeType("GLint") first: Int,
    @NativeType("GLsizei") count: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glDrawArrays(mode, first, count)
        DriverTypeGL.GLES -> GLES.glDrawArrays(mode, first, count)
    }
}

// --- [ glCheckFramebufferStatus ] ---
fun glCheckFramebufferStatus(@NativeType("GLenum") target: Int): Int {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glCheckFramebufferStatus(target)
        DriverTypeGL.GLES -> GLES.glCheckFramebufferStatus(target)
    }
}

// --- [ glClearBufferfi ] ---
fun glClearBufferfi(
    @NativeType("GLenum") buffer: Int,
    @NativeType("GLint") drawbuffer: Int,
    @NativeType("GLfloat") depth: Float,
    @NativeType("GLint") stencil: Int
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glClearBufferfi(buffer, drawbuffer, depth, stencil)
        DriverTypeGL.GLES -> GLES.glClearBufferfi(buffer, drawbuffer, depth, stencil)
    }
}

fun glClearBufferfv(
    @NativeType("GLenum") buffer: Int,
    @NativeType("GLint") drawbuffer: Int,
    @NativeType("GLfloat const *") value: FloatBuffer
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glClearBufferfv(buffer, drawbuffer, value)
        DriverTypeGL.GLES -> GLES.glClearBufferfv(buffer, drawbuffer, value)
    }
}

fun glClearBufferiv(
    @NativeType("GLenum") buffer: Int,
    @NativeType("GLint") drawbuffer: Int,
    @NativeType("GLint const *") value: IntBuffer
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glClearBufferiv(buffer, drawbuffer, value)
        DriverTypeGL.GLES -> GLES.glClearBufferiv(buffer, drawbuffer, value)
    }
}

// --- [ glTexImage2DMultisample ] ---
fun glTexImage2DMultisample(
    @NativeType("GLenum") target: Int,
    @NativeType("GLsizei") samples: Int,
    @NativeType("GLint") internalformat: Int,
    @NativeType("GLsizei") width: Int,
    @NativeType("GLsizei") height: Int,
    @NativeType("GLboolean") fixedsamplelocations: Boolean
) {
    return when (driverType) {
        DriverTypeGL.GL -> GL.glTexImage2DMultisample(target, samples, internalformat, width, height, fixedsamplelocations)
        DriverTypeGL.GLES -> {
            val angle = (Driver.instance as DriverGL3).angleExtensions
            if (angle != null && angle.glTexStorage2DMultisampleANGLEAddress != 0L) {
                angle.glTexStorage2DMultisampleANGLE(target, samples, internalformat, width, height, fixedsamplelocations)
            } else {
                error("not supported")
            }
        }
    }
}

// --- [ glFramebufferRenderbuffer ] ---
fun glFramebufferRenderbuffer(
    @NativeType("GLenum") target: Int,
    @NativeType("GLenum") attachment: Int,
    @NativeType("GLenum") renderbuffertarget: Int,
    @NativeType("GLuint") renderbuffer: Int
) {
    return when(driverType) {
        DriverTypeGL.GL -> GL.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer)
        DriverTypeGL.GLES -> GLES.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer)
    }
}

fun glGenRenderbuffers(): Int {
    return when(driverType) {
        DriverTypeGL.GL -> GL.glGenRenderbuffers()
        DriverTypeGL.GLES -> GLES.glGenRenderbuffers()
    }
}

// --- [ glBindRenderbuffer ] ---
fun glBindRenderbuffer(@NativeType("GLenum") target: Int, @NativeType("GLuint") renderbuffer: Int) {
    return when(driverType) {
        DriverTypeGL.GL -> GL.glBindRenderbuffer(target, renderbuffer)
        DriverTypeGL.GLES -> GLES.glBindRenderbuffer(target, renderbuffer)
    }
}

// --- [ glRenderbufferStorage ] ---
fun glRenderbufferStorage(
    @NativeType("GLenum") target: Int,
    @NativeType("GLenum") internalformat: Int,
    @NativeType("GLsizei") width: Int,
    @NativeType("GLsizei") height: Int
) {
    return when(driverType) {
        DriverTypeGL.GL -> GL.glRenderbufferStorage(target, internalformat, width, height)
        DriverTypeGL.GLES -> GLES.glRenderbufferStorage(target, internalformat, width, height)
    }
}

// --- [ glRenderbufferStorageMultisample ] ---
fun glRenderbufferStorageMultisample(
    @NativeType("GLenum") target: Int,
    @NativeType("GLsizei") samples: Int,
    @NativeType("GLenum") internalformat: Int,
    @NativeType("GLsizei") width: Int,
    @NativeType("GLsizei") height: Int
) {
    return when(driverType) {
        DriverTypeGL.GL -> GL.glRenderbufferStorageMultisample(target, samples, internalformat, width, height)
        DriverTypeGL.GLES -> GLES.glRenderbufferStorageMultisample(target, samples, internalformat, width, height)
    }
}

fun glClearBufferfv(
    @NativeType("GLenum") buffer: Int,
    @NativeType("GLint") drawbuffer: Int,
    @NativeType("GLfloat const *") value: FloatArray
) {
    return when(driverType) {
        DriverTypeGL.GL -> GL.glClearBufferfv(buffer, drawbuffer, value)
        DriverTypeGL.GLES -> GLES.glClearBufferfv(buffer, drawbuffer, value)
    }
}

// --- [ glFlush ] ---
fun glFlush() {
    return when(driverType) {
        DriverTypeGL.GL -> GL.glFlush()
        DriverTypeGL.GLES -> GLES.glFlush()
    }
}