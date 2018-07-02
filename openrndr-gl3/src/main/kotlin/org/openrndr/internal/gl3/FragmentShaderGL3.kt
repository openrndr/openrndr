package org.openrndr.internal.gl3

import mu.KotlinLogging
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20

private val logger = KotlinLogging.logger {}

class FragmentShaderGL3(val shaderObject:Int, val name:String="<unknown-fragment-shader>") {
    companion object {
        fun fromString(code:String, name:String="<unknown-fragment-shader>"): FragmentShaderGL3 {
            logger.trace { "compiling shader $name from $code" }
            val shaderObject = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER)
            GL20.glShaderSource(shaderObject, code)
            GL20.glCompileShader(shaderObject)

            val compileStatus = IntArray(1)
            GL20.glGetShaderiv(shaderObject, GL20.GL_COMPILE_STATUS, compileStatus)
            if (compileStatus[0] != GL11.GL_TRUE) {
                checkShaderInfoLog(shaderObject, code, name)
                throw Exception("could not compile vertex shader")
            }
            return FragmentShaderGL3(shaderObject, name)
        }
    }
}