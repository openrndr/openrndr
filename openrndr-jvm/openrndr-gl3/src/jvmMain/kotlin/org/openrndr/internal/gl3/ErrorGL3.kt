package org.openrndr.internal.gl3

import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL33C.*
import org.openrndr.internal.Driver

class GL3Exception(message: String) : Exception(message)

inline fun checkGLErrors(crossinline errorFunction: ((Int) -> String?) = { null }) {
    val error = glGetError()
    if (error != GL_NO_ERROR) {
        val message = when (error) {
            GL_INVALID_OPERATION -> "GL_INVALID_OPERATION"
            GL_INVALID_VALUE -> "GL_INVALID_VALUE"
            GL_INVALID_ENUM -> "GL_INVALID_ENUM"
            GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION"
            GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY"
            GL_STACK_UNDERFLOW -> "GL_STACK_UNDERFLOW"
            GL_STACK_OVERFLOW -> "GL_STACK_OVERFLOW"
            else -> "<untranslated: $error>"
        }
        throw GL3Exception("[context=${Driver.instance.contextID}] GL ERROR: $message ${errorFunction.invoke(error)}")
    }
}

inline fun debugGLErrors(crossinline errorFunction: ((Int) -> String?) = { null }) {
    if (DriverGL3Configuration.useDebugContext) {
        checkGLErrors(errorFunction)
    }
}