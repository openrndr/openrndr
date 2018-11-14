package org.openrndr.internal.gl3

import org.lwjgl.opengl.GL33C.*

class VertexShaderGL3(val shaderObject:Int, val name:String="<unknown-vertex-shader>") {
    companion object {
        fun fromString(code:String, name:String="<unknown-vertex-shader>"):VertexShaderGL3 {
            val shaderObject = glCreateShader(GL_VERTEX_SHADER)
            glShaderSource(shaderObject, code)
            glCompileShader(shaderObject)
            val compileStatus = IntArray(1)
            glGetShaderiv(shaderObject, GL_COMPILE_STATUS,compileStatus)
            if (compileStatus[0] != GL_TRUE) {
                checkShaderInfoLog(shaderObject, code, name)
                throw Exception("could not compile vertex shader")
            }
            return VertexShaderGL3(shaderObject, name)
        }
    }
}