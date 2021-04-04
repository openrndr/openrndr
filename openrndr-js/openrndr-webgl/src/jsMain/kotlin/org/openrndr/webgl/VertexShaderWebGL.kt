package org.openrndr.webgl

import org.khronos.webgl.WebGLShader
import org.khronos.webgl.WebGLRenderingContext as GL

class VertexShaderWebGL(val shaderObject: WebGLShader, val name: String) {
    companion object {
        fun fromString(context: GL, code: String, name: String): VertexShaderWebGL {
            val shader = context.createShader(GL.VERTEX_SHADER) ?: error("failed to create shader")
            context.shaderSource(shader, code)
            context.compileShader(shader)
            require(context.getShaderParameter(shader, GL.COMPILE_STATUS) as Boolean) {
                """vertex shader compilation failed"""
            }
            return VertexShaderWebGL(shader, name)
        }
    }
}