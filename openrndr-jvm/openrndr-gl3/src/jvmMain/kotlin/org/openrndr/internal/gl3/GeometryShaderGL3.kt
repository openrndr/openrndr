package org.openrndr.internal.gl3

import org.lwjgl.opengl.GL33C
import org.lwjgl.opengl.GL40C
import org.openrndr.internal.Driver

class GeometryShaderGL3(val shaderObject: Int, val name: String) {
    companion object {
        fun fromString(code: String, name: String): GeometryShaderGL3 {
            require(Driver.glType == DriverTypeGL.GL) {
                "Geometry shaders are not supported by ${Driver.glType}"
            }
            val shaderObject = glCreateShader(GL33C.GL_GEOMETRY_SHADER)
            glShaderSource(shaderObject, code)
            glCompileShader(shaderObject)
            val compileStatus = IntArray(1)
            glGetShaderiv(shaderObject, GL33C.GL_COMPILE_STATUS, compileStatus)
            if (compileStatus[0] != GL33C.GL_TRUE) {
                checkShaderInfoLog(shaderObject, code, name)
                throw Exception("could not compile geometry shader")
            }
            return GeometryShaderGL3(shaderObject, name)
        }
    }
}

class TessellationControlShaderGL3(val shaderObject: Int, val name: String) {
    companion object {
        fun fromString(code: String, name: String): TessellationControlShaderGL3 {
            require(Driver.glType == DriverTypeGL.GL) {
                "Tessellation control shaders are not supported by ${Driver.glType}"
            }

            val shaderObject = glCreateShader(GL40C.GL_TESS_CONTROL_SHADER)
            glShaderSource(shaderObject, code)
            glCompileShader(shaderObject)
            val compileStatus = IntArray(1)
            glGetShaderiv(shaderObject, GL33C.GL_COMPILE_STATUS, compileStatus)
            if (compileStatus[0] != GL33C.GL_TRUE) {
                checkShaderInfoLog(shaderObject, code, name)
                throw Exception("could not compile tessellation control shader")
            }
            return TessellationControlShaderGL3(shaderObject, name)
        }
    }
}

class TessellationEvaluationShaderGL3(val shaderObject: Int, val name: String) {
    companion object {
        fun fromString(code: String, name: String): TessellationEvaluationShaderGL3 {
            require(Driver.glType == DriverTypeGL.GL) {
                "Tessellation evaluation shaders are not supported by ${Driver.glType}"
            }

            val shaderObject = glCreateShader(GL40C.GL_TESS_EVALUATION_SHADER)
            glShaderSource(shaderObject, code)
            glCompileShader(shaderObject)
            val compileStatus = IntArray(1)
            glGetShaderiv(shaderObject, GL33C.GL_COMPILE_STATUS, compileStatus)
            if (compileStatus[0] != GL33C.GL_TRUE) {
                checkShaderInfoLog(shaderObject, code, name)
                throw Exception("could not compile tessellation evaluation shader")
            }
            return TessellationEvaluationShaderGL3(shaderObject, name)
        }
    }
}