package org.openrndr.internal.gl3

import org.lwjgl.opengl.GL33C
import org.lwjgl.opengl.GL40C

class GeometryShaderGL3(val shaderObject: Int, val name: String) {
    companion object {
        fun fromString(code: String, name: String): GeometryShaderGL3 {
            val shaderObject = glCreateShader(GL33C.GL_GEOMETRY_SHADER)
            glShaderSource(shaderObject, code)
            glCompileShader(shaderObject)
            val compileStatus = IntArray(1)
            glGetShaderiv(shaderObject, GL33C.GL_COMPILE_STATUS, compileStatus)
            if (compileStatus[0] != GL33C.GL_TRUE) {
                checkShaderInfoLog(shaderObject, code, name)
                throw Exception("could not compile vertex shader")
            }
            return GeometryShaderGL3(shaderObject, name)
        }
    }
}

class TessellationControlShaderGL3(val shaderObject: Int, val name: String) {
    companion object {
        fun fromString(code: String, name: String): TessellationControlShaderGL3 {
            val shaderObject = GL33C.glCreateShader(GL40C.GL_TESS_CONTROL_SHADER)
            glShaderSource(shaderObject, code)
            glCompileShader(shaderObject)
            val compileStatus = IntArray(1)
            glGetShaderiv(shaderObject, GL33C.GL_COMPILE_STATUS, compileStatus)
            if (compileStatus[0] != GL33C.GL_TRUE) {
                checkShaderInfoLog(shaderObject, code, name)
                throw Exception("could not compile vertex shader")
            }
            return TessellationControlShaderGL3(shaderObject, name)
        }
    }
}

class TessellationEvaluationShaderGL3(val shaderObject: Int, val name: String) {
    companion object {
        fun fromString(code: String, name: String): TessellationEvaluationShaderGL3 {
            val shaderObject = glCreateShader(GL40C.GL_TESS_EVALUATION_SHADER)
            glShaderSource(shaderObject, code)
            glCompileShader(shaderObject)
            val compileStatus = IntArray(1)
            glGetShaderiv(shaderObject, GL33C.GL_COMPILE_STATUS, compileStatus)
            if (compileStatus[0] != GL33C.GL_TRUE) {
                checkShaderInfoLog(shaderObject, code, name)
                throw Exception("could not compile vertex shader")
            }
            return TessellationEvaluationShaderGL3(shaderObject, name)
        }
    }
}