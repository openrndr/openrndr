package org.openrndr.internal.gl3

import org.lwjgl.opengl.GL33C

class GeometryShaderGL3(val shaderObject: Int, val name: String) {
    companion object {
        fun fromString(code: String, name: String): GeometryShaderGL3 {
            val shaderObject = GL33C.glCreateShader(GL33C.GL_GEOMETRY_SHADER)
            GL33C.glShaderSource(shaderObject, code)
            GL33C.glCompileShader(shaderObject)
            val compileStatus = IntArray(1)
            GL33C.glGetShaderiv(shaderObject, GL33C.GL_COMPILE_STATUS, compileStatus)
            if (compileStatus[0] != GL33C.GL_TRUE) {
                checkShaderInfoLog(shaderObject, code, name)
                throw Exception("could not compile vertex shader")
            }
            return GeometryShaderGL3(shaderObject, name)
        }
    }
}