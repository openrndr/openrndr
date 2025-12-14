package org.openrndr.internal.gl

import android.opengl.GLES30

internal fun compileShader(type: Int, src: String): Int {
    val id = GLES30.glCreateShader(type)
    GLES30.glShaderSource(id, src)
    GLES30.glCompileShader(id)
    val ok = IntArray(1)
    GLES30.glGetShaderiv(id, GLES30.GL_COMPILE_STATUS, ok, 0)
    if (ok[0] == 0) {
        val log = GLES30.glGetShaderInfoLog(id)
        GLES30.glDeleteShader(id)
        throw RuntimeException("GLES compile failed:\n$log\n---\n$src")
    }
    return id
}

internal fun linkProgram(vs: Int, fs: Int): Int {
    val prog = GLES30.glCreateProgram()
    GLES30.glAttachShader(prog, vs)
    GLES30.glAttachShader(prog, fs)
    GLES30.glLinkProgram(prog)
    val ok = IntArray(1)
    GLES30.glGetProgramiv(prog, GLES30.GL_LINK_STATUS, ok, 0)
    if (ok[0] == 0) {
        val log = GLES30.glGetProgramInfoLog(prog)
        GLES30.glDeleteProgram(prog)
        throw RuntimeException("GLES link failed:\n$log")
    }
    GLES30.glDetachShader(prog, vs); GLES30.glDetachShader(prog, fs)
    GLES30.glDeleteShader(vs); GLES30.glDeleteShader(fs)
    return prog
}