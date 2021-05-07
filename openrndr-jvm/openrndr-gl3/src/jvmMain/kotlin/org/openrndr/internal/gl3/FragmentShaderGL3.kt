package org.openrndr.internal.gl3

import mu.KotlinLogging
import org.lwjgl.opengl.GL33C.*

private val logger = KotlinLogging.logger {}

class FragmentShaderGL3(val shaderObject:Int, val name:String="<unknown-fragment-shader>") {
    companion object {
        fun fromString(code:String, name:String="<unknown-fragment-shader>"): FragmentShaderGL3 {
            logger.trace { "compiling shader $name from $code" }
            val shaderObject = glCreateShader(GL_FRAGMENT_SHADER)
            glShaderSource(shaderObject, code)
            glCompileShader(shaderObject)

            val compileStatus = IntArray(1)
            glGetShaderiv(shaderObject, GL_COMPILE_STATUS, compileStatus)
            if (compileStatus[0] != GL_TRUE) {
                checkShaderInfoLog(shaderObject, code, name)
                throw Exception("could not compile vertex shader")
            }
            return FragmentShaderGL3(shaderObject, name)
        }
    }
}