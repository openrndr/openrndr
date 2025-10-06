package org.openrndr.webgl

import web.console.console
import web.gl.WebGLShader
import web.gl.WebGL2RenderingContext as GL

class VertexShaderWebGL(val shaderObject: WebGLShader, val name: String) {
    companion object {
        fun fromString(context: GL, code: String, name: String): VertexShaderWebGL {
            val shader = context.createShader(GL.VERTEX_SHADER) ?: error("failed to create shader")
            context.shaderSource(shader, code)
            context.compileShader(shader)
            require(context.getShaderParameter(shader, GL.COMPILE_STATUS) as Boolean) {
                val error = context.getShaderInfoLog(shader)?:""
                error.split("\n").forEach {
                    console.error(it)
                }
                console.error("---")
                code.split("\n").forEachIndexed { index, it ->
                    console.log("$index\t$it")
                }
                """fragment shader compilation failed""".trimMargin()
            }
            return VertexShaderWebGL(shader, name)
        }
    }
}