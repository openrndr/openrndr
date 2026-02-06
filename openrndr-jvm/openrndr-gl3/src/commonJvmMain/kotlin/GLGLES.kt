package org.openrndr.internal.gl3

import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer


expect inline fun glEnable(target: Int)
expect inline fun glIsEnabled(cap: Int): Boolean
expect inline fun glDisable(target: Int)
expect inline fun glGenBuffers(): Int

expect inline fun glBindBuffer(target: Int, buffer: Int)

expect inline fun glBufferData(target: Int, size: Long, usage: Int)
expect inline fun glBufferData(target: Int, data: ByteBuffer, usage: Int)
expect inline fun glBufferData(target: Int, data: IntArray, usage: Int)

expect inline fun glGetIntegerv(pname: Int, params: IntArray)

expect inline fun glGetInteger(pname: Int): Int
expect inline fun glGetString(pname: Int): String?
expect inline fun glBufferSubData(target: Int, offset: Long, data: ByteBuffer)
expect inline fun glBindFramebuffer(target: Int, framebuffer: Int)

expect inline fun glScissor(x: Int, y: Int, width: Int, height: Int)
expect inline fun glViewport(x: Int, y: Int, w: Int, h: Int)
expect inline fun glDepthMask(flag: Boolean)
expect inline fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float)
expect inline fun glClear(mask: Int): Unit
expect inline fun glGenFramebuffers(): Int
expect inline fun glDrawBuffers(bufs: IntArray)
expect inline fun glFramebufferTexture(target: Int, attachment: Int, texture: Int, level: Int)
expect inline fun glBlendEquationi(buf: Int, mode: Int)
expect inline fun glBlendFunci(buf: Int, sfactor: Int, dfactor: Int)
expect inline fun glBindVertexArray(array: Int)
expect inline fun glClearDepth(depth: Double)
expect inline fun glCreateShader(type: Int): Int
expect inline fun glShaderSource(shader: Int, string: CharSequence)
expect inline fun glCompileShader(shader: Int)
expect inline fun glGetShaderiv(shader: Int, pname: Int, params: IntArray)
expect inline fun glGetProgramiv(program: Int, pname: Int, params: IntArray)
expect inline fun glGetShaderInfoLog(shader: Int, length: IntArray?, infoLog: ByteBuffer)
expect inline fun glGetProgramInfoLog(program: Int, length: IntArray?, infoLog: ByteBuffer)
expect inline fun glCreateProgram(): Int
expect inline fun glAttachShader(program: Int, shader: Int)
expect inline fun glLinkProgram(program: Int)
expect inline fun glFinish()
expect inline fun glUseProgram(program: Int)
expect inline fun glGetUniformBlockIndex(program: Int, uniformBlockName: CharSequence): Int
expect inline fun glGetAttribLocation(program: Int, name: CharSequence): Int
expect inline fun glDeleteProgram(program: Int)
expect inline fun glUniformBlockBinding(
    program: Int,
    uniformBlockIndex: Int,
    uniformBlockBinding: Int
)

expect inline fun glGetActiveUniformBlockiv(
    program: Int,
    uniformBlockIndex: Int,
    pname: Int,
    params: IntBuffer
)

expect inline fun glGetActiveUniformsiv(
    program: Int,
    uniformIndices: IntBuffer,
    pname: Int,
    params: IntBuffer
)

expect inline fun glGetActiveUniformsiv(
    program: Int,
    uniformIndices: IntArray,
    pname: Int,
    params: IntArray
)

expect inline fun glGetActiveUniformName(program: Int, uniformIndex: Int, bufSize: Int = 1024): String
expect inline fun glGetError(): Int
expect inline fun glBindBufferBase(target: Int, index: Int, buffer: Int)
expect inline fun glColorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean)
expect inline fun glBlendEquation(mode: Int)
expect inline fun glBlendFunc(sfactor: Int, dfactor: Int)

expect inline fun glBlendEquationSeparatei(buf: Int, modeRGB: Int, modeAlpha: Int)


expect inline fun glBlendEquationSeparate(modeRGB: Int, modeAlpha: Int)
expect inline fun glBlendFuncSeparate(
    sfactorRGB: Int,
    dfactorRGB: Int,
    sfactorAlpha: Int,
    dfactorAlpha: Int
)

expect inline fun glBlendFuncSeparatei(
    buf: Int,
    sfactorRGB: Int,
    dfactorRGB: Int,
    sfactorAlpha: Int,
    dfactorAlpha: Int
)


expect inline fun glDepthFunc(func: Int)
expect inline fun glCullFace(mode: Int)
expect inline fun glGenVertexArrays(arrays: IntArray)
expect inline fun glDrawArraysInstanced(mode: Int, first: Int, count: Int, primcount: Int)
expect inline fun glDrawElements(mode: Int, count: Int, type: Int, indices: Long)
expect inline fun glDrawElementsInstanced(
    mode: Int,
    count: Int,
    type: Int,
    indices: Long,
    primcount: Int
)

expect inline fun glMultiDrawArrays(mode: Int, first: IntArray, count: IntArray)
expect inline fun glMultiDrawElements(mode: Int, count: IntArray, type: Int, indices: Any)
expect inline fun glMultiDrawArraysIndirect(mode: Int, indirect: Any, drawcount: Int, stride: Int)
expect inline fun glMultiDrawElementsIndirect(mode: Int, type: Int, indirect: Any, drawcount: Int, stride: Int)


expect inline fun glEnableVertexAttribArray(index: Int)
expect inline fun glVertexAttribPointer(
    index: Int,
    size: Int,
    type: Int,
    normalized: Boolean,
    stride: Int,
    pointer: Long
)

expect inline fun glVertexAttribIPointer(
    index: Int,
    size: Int,
    type: Int,
    stride: Int,
    pointer: Long
)

expect inline fun glVertexAttribDivisor(index: Int, divisor: Int)
expect inline fun glDeleteBuffers(buffer: Int)
expect inline fun glDeleteVertexArrays(array: Int)
expect inline fun glGetUniformLocation(program: Int, name: CharSequence): Int
expect inline fun glProgramUniform4f(
    program: Int,
    location: Int,
    x: Float,
    y: Float,
    z: Float,
    w: Float
)

expect inline fun glUniform4f(location: Int, v0: Float, v1: Float, v2: Float, v3: Float)
expect inline fun glProgramUniform3f(program: Int, location: Int, x: Float, y: Float, z: Float)
expect inline fun glUniform3f(location: Int, v0: Float, v1: Float, v2: Float)
expect inline fun glProgramUniform2f(program: Int, location: Int, x: Float, y: Float)
expect inline fun glUniform2f(location: Int, v0: Float, v1: Float)
expect inline fun glProgramUniform1i(program: Int, location: Int, x: Int)
expect inline fun glUniform1i(location: Int, v0: Int)
expect inline fun glProgramUniform1f(program: Int, location: Int, x: Float)
expect inline fun glUniform1f(location: Int, v0: Float)
expect inline fun glUniform2i(location: Int, v0: Int, v1: Int)
expect inline fun glUniform3i(location: Int, v0: Int, v1: Int, v2: Int)
expect inline fun glUniform4i(location: Int, v0: Int, v1: Int, v2: Int, v3: Int)
expect inline fun glProgramUniform2i(program: Int, location: Int, x: Int, y: Int)
expect inline fun glProgramUniform3i(program: Int, location: Int, x: Int, y: Int, z: Int)
expect inline fun glProgramUniform4i(program: Int, location: Int, x: Int, y: Int, z: Int, w: Int)
expect inline fun glProgramUniformMatrix3fv(
    program: Int,
    location: Int,
    transpose: Boolean,
    value: FloatArray
)

expect inline fun glProgramUniformMatrix3fv(
    program: Int,
    location: Int,
    transpose: Boolean,
    value: FloatBuffer
)

expect inline fun glUniformMatrix3fv(location: Int, transpose: Boolean, value: FloatArray)
expect inline fun glProgramUniformMatrix4fv(
    program: Int,
    location: Int,
    transpose: Boolean,
    value: FloatArray
)

expect inline fun glUniformMatrix4fv(location: Int, transpose: Boolean, value: FloatArray)
expect inline fun glProgramUniform4iv(program: Int, location: Int, value: IntArray)
expect inline fun glUniform4iv(location: Int, value: IntArray)
expect inline fun glProgramUniform3iv(program: Int, location: Int, value: IntArray)
expect inline fun glUniform3iv(location: Int, value: IntArray)
expect inline fun glProgramUniform2iv(program: Int, location: Int, value: IntArray)
expect inline fun glUniform2iv(location: Int, value: IntArray)
expect inline fun glProgramUniform2fv(program: Int, location: Int, value: FloatArray)
expect inline fun glUniform2fv(location: Int, value: FloatArray)
expect inline fun glProgramUniform3fv(program: Int, location: Int, value: FloatArray)
expect inline fun glUniform3fv(location: Int, value: FloatArray)
expect inline fun glProgramUniform4fv(program: Int, location: Int, value: FloatArray)
expect inline fun glUniform4fv(location: Int, value: FloatArray)
expect inline fun glProgramUniform1fv(program: Int, location: Int, value: FloatArray)
expect inline fun glUniform1fv(location: Int, value: FloatArray)
expect inline fun glProgramUniform1iv(program: Int, location: Int, value: IntArray)
expect inline fun glUniform1iv(location: Int, value: IntArray)
expect inline fun glProgramUniformMatrix4fv(
    program: Int,
    location: Int,
    transpose: Boolean,
    value: FloatBuffer
)

expect inline fun glUniformMatrix3fv(location: Int, transpose: Boolean, value: FloatBuffer)
expect inline fun glUniformMatrix4fv(location: Int, transpose: Boolean, value: FloatBuffer)
expect inline fun glGetActiveUniform(
    program: Int,
    index: Int,
    length: IntArray,
    size: IntArray,
    type: IntArray,
) : String

expect inline fun glTexParameteri(target: Int, pname: Int, param: Int)
expect inline fun glGenTextures(): Int
expect inline fun glBindTexture(target: Int, texture: Int)
expect inline fun glTexImage3D(
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
)

expect inline fun glActiveTexture(texture: Int)
expect inline fun glDeleteTextures(texture: Int)
expect inline fun glReadBuffer(src: Int)
expect inline fun glGenerateMipmap(target: Int)
expect inline fun glTexSubImage3D(
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
)

expect inline fun glCopyTexSubImage2D(
    target: Int,
    level: Int,
    xoffset: Int,
    yoffset: Int,
    x: Int,
    y: Int,
    width: Int,
    height: Int
)

expect inline fun glCompressedTexSubImage3D(
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
)

expect inline fun glCopyTexSubImage3D(
    target: Int,
    level: Int,
    xoffset: Int,
    yoffset: Int,
    zoffset: Int,
    x: Int,
    y: Int,
    width: Int,
    height: Int
)

expect inline fun glTexImage2D(
    target: Int,
    level: Int,
    internalformat: Int,
    width: Int,
    height: Int,
    border: Int,
    format: Int,
    type: Int,
    pixels: ByteBuffer?
)

expect inline fun glBlitFramebuffer(
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
)

expect inline fun glCopyImageSubData(
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
)

expect inline fun glTexParameterf(target: Int, pname: Int, param: Float)
expect inline fun glPixelStorei(pname: Int, param: Int)
expect inline fun glTexSubImage2D(
    target: Int,
    level: Int,
    xoffset: Int,
    yoffset: Int,
    width: Int,
    height: Int,
    format: Int,
    type: Int,
    pixels: ByteBuffer
)

expect inline fun glCompressedTexSubImage2D(
    target: Int,
    level: Int,
    xoffset: Int,
    yoffset: Int,
    width: Int,
    height: Int,
    format: Int,
    data: ByteBuffer
)

expect inline fun glFramebufferTexture2D(
    target: Int,
    attachment: Int,
    textarget: Int,
    texture: Int,
    level: Int
)

expect inline fun glFramebufferTextureLayer(
    target: Int,
    attachment: Int,
    texture: Int,
    level: Int,
    layer: Int
)

expect inline fun glGetBoolean(pname: Int): Boolean
expect inline fun glGetTexLevelParameteri(target: Int, level: Int, pname: Int): Int
expect inline fun glGetTexParameteri(target: Int, pname: Int): Int
expect inline fun glDeleteFramebuffers(framebuffer: Int)
expect inline fun glStencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int)
expect inline fun glStencilOpSeparate(face: Int, sfail: Int, dpfail: Int, dppass: Int)
expect inline fun glStencilMaskSeparate(face: Int, mask: Int)
expect inline fun glDrawArrays(mode: Int, first: Int, count: Int)
expect inline fun glCheckFramebufferStatus(target: Int): Int
expect inline fun glClearBufferfi(buffer: Int, drawbuffer: Int, depth: Float, stencil: Int)
expect inline fun glClearBufferfv(buffer: Int, drawbuffer: Int, value: FloatBuffer)
expect inline fun glClearBufferiv(buffer: Int, drawbuffer: Int, value: IntBuffer)
expect inline fun glTexImage2DMultisample(
    target: Int,
    samples: Int,
    internalformat: Int,
    width: Int,
    height: Int,
    fixedsamplelocations: Boolean
)

expect inline fun glTexStorage2D(
    target: Int,
    levels: Int,
    internalformat: Int,
    width: Int,
    height: Int
)

expect inline fun glTexStorage2DMultisample(
    target: Int,
    samples: Int,
    internalformat: Int,
    width: Int,
    height: Int,
    fixedsamplelocations: Boolean
)

expect inline fun glTexStorage3D(
    target: Int,
    levels: Int,
    internalformat: Int,
    width: Int,
    height: Int,
    depth: Int
)

expect inline fun glFramebufferRenderbuffer(
    target: Int,
    attachment: Int,
    renderbuffertarget: Int,
    renderbuffer: Int
)

expect inline fun glGenRenderbuffers(): Int
expect inline fun glDeleteRenderbuffers(renderbuffer: Int)
expect inline fun glBindRenderbuffer(target: Int, renderbuffer: Int)
expect inline fun glRenderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int)
expect inline fun glRenderbufferStorageMultisample(
    target: Int,
    samples: Int,
    internalformat: Int,
    width: Int,
    height: Int
)

expect inline fun glClearBufferfv(buffer: Int, drawbuffer: Int, value: FloatArray)
expect inline fun glFlush()
expect inline fun glGetProgramResourceiv(
    program: Int,
    programInterface: Int,
    index: Int,
    props: IntArray,
    length: IntArray?,
    params: IntArray
)

expect inline fun glGetProgramResourceIndex(
    program: Int,
    programInterface: Int,
    name: CharSequence
): Int

expect inline fun glGetProgrami(program: Int, pname: Int): Int
expect inline fun glDispatchCompute(num_groups_x: Int, num_groups_y: Int, num_groups_z: Int)
expect inline fun glMemoryBarrier(barriers: Int)
expect inline fun glBindImageTexture(
    unit: Int,
    texture: Int,
    level: Int,
    layered: Boolean,
    layer: Int,
    access: Int,
    format: Int
)

expect inline fun glPatchParameteri(pname: Int, value: Int)

expect inline fun glReadPixels(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    format: Int,
    type: Int,
    pixels: ByteBuffer
)

expect inline fun glGetTexParameterf(target: Int, pname: Int): Float

expect inline fun glMapBufferRange(target :Int, offset :Long, length: Long, access:Int): ByteBuffer?

expect inline fun glUnmapBuffer(target: Int): Boolean


expect inline fun glIsBuffer(buffer: Int): Boolean

expect inline fun glIsTexture(texture: Int): Boolean