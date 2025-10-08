package org.openrndr.webgl

import io.github.oshai.kotlinlogging.KotlinLogging
import web.console.console
import web.gl.WebGLShader
import kotlin.js.JsBoolean
import kotlin.js.toBoolean
import kotlin.js.unsafeCast
import web.gl.WebGL2RenderingContext as GL

private val logger = KotlinLogging.logger {  }

class VertexShaderWebGL(val shaderObject: WebGLShader, val name: String) {
    companion object {
        fun fromString(context: GL, code: String, name: String): VertexShaderWebGL {
            logger.debug { "Creating vertex shader"}
            val shader = context.createShader(GL.VERTEX_SHADER) ?: error("failed to create shader")
            logger.debug { "Got shader: $shader" }

            logger.debug { "set shader source" }
            context.shaderSource(shader, code)
            logger.debug { "compiler shader" }
            context.compileShader(shader)
            require(context.getShaderParameter(shader, GL.COMPILE_STATUS)?.unsafeCast<JsBoolean>()?.toBoolean() == true) {
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