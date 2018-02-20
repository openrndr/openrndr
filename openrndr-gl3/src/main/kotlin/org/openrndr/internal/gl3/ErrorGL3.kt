package org.openrndr.internal.gl3

import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL30.GL_INVALID_FRAMEBUFFER_OPERATION

class GL3Exception(message: String) : Exception(message)

fun checkGLErrors(errorFunction: ((Int)->String?)?=null) {
    val error = glGetError()
    if (error != GL_NO_ERROR) {

        val message = when (error) {
            GL_INVALID_OPERATION             -> "GL_INVALID_OPERATION"
            GL_INVALID_VALUE                 -> "GL_INVALID_VALUE"
            GL_INVALID_ENUM                  -> "GL_INVALID_ENUM"
            GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION"
            GL_OUT_OF_MEMORY                 -> "GL_OUT_OF_MEMORY"
            GL_STACK_UNDERFLOW               -> "GL_STACK_UNDERFLOW"
            GL_STACK_OVERFLOW                -> "GL_STACK_OVERFLOW"
            else                             -> "<untranslated: $error>"
        }

        throw GL3Exception("GL ERROR: $message ${errorFunction?.invoke(error)}" )
    }
}

fun debugGLErrors(errorFunction: ((Int)->String?)?=null) {
    //checkGLErrors(errorFunction)
}

