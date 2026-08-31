package org.openrndr.webgl

import io.github.oshai.kotlinlogging.KotlinLogging
import web.console.console
import web.gl.COMPILE_STATUS
import web.gl.VERTEX_SHADER
import web.gl.WebGLShader
import web.gl.WebGL2RenderingContext as GL

private val logger = KotlinLogging.logger {  }

class VertexShaderWebGL(val shaderObject: WebGLShader, val name: String) {
    companion object {
        fun fromString(context: GL, code: String, name: String): VertexShaderWebGL {
            logger.debug { "Creating vertex shader"}
            val shader = context.createShader(VERTEX_SHADER) ?: error("failed to create shader")
            context.checkErrors()
            logger.debug { "Got shader: $shader" }

            logger.debug { "set shader source" }
            context.shaderSource(shader, code)
            context.checkErrors()
            logger.debug { "compiler shader" }
            context.compileShader(shader)
            context.checkErrors()
            require(context.getShaderParameter(shader, COMPILE_STATUS)?.unsafeCast<JsBoolean>()?.toBoolean() == true) {
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