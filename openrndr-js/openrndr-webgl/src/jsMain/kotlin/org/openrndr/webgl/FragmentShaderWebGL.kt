package org.openrndr.webgl

import org.khronos.webgl.WebGLShader
import org.khronos.webgl.WebGLRenderingContext as GL

class FragmentShaderWebGL(val shaderObject: WebGLShader, val name: String) {
    companion object {
        fun fromString(context: GL, code: String, name: String): FragmentShaderWebGL {
            val shader = context.createShader(GL.FRAGMENT_SHADER) ?: error("failed to create shader")
            context.shaderSource(shader, code)
            context.compileShader(shader)
            require(context.getShaderParameter(shader, GL.COMPILE_STATUS) as Boolean) {
                """fragment shader compilation failed"""
            }
            return FragmentShaderWebGL(shader, name)
        }
    }
}