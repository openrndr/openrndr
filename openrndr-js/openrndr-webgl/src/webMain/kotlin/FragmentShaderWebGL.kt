package org.openrndr.webgl

import io.github.oshai.kotlinlogging.KotlinLogging
import web.console.console
import web.gl.COMPILE_STATUS
import web.gl.FRAGMENT_SHADER
import web.gl.WebGLShader
import web.gl.WebGL2RenderingContext as GL

private val logger = KotlinLogging.logger {  }
class FragmentShaderWebGL(val shaderObject: WebGLShader, val name: String) {
    @OptIn(ExperimentalWasmJsInterop::class)
    companion object {
        fun fromString(context: GL, code: String, name: String): FragmentShaderWebGL {

            logger.debug { "Creating fragment shader $name" }

            val shader = context.createShader(FRAGMENT_SHADER) ?: error("failed to create shader")
            context.shaderSource(shader, code)
            context.compileShader(shader)
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
            return FragmentShaderWebGL(shader, name)
        }
    }
}