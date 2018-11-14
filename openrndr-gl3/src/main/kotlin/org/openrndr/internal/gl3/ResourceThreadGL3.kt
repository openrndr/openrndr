package org.openrndr.internal.gl3

import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33C.GL_TRUE
import org.lwjgl.system.MemoryUtil
import org.openrndr.color.ColorRGBa
import org.openrndr.internal.Driver
import org.openrndr.internal.ResourceThread
import kotlin.concurrent.thread

class ResourceThreadGL3 : ResourceThread {
    companion object {
        fun create(f: () -> Unit): ResourceThreadGL3 {
            GLFW.glfwDefaultWindowHints()
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3)
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE)
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
            GLFW.glfwWindowHint(GLFW.GLFW_RED_BITS, 8)
            GLFW.glfwWindowHint(GLFW.GLFW_GREEN_BITS, 8)
            GLFW.glfwWindowHint(GLFW.GLFW_BLUE_BITS, 8)
            GLFW.glfwWindowHint(GLFW.GLFW_STENCIL_BITS, 8)
            GLFW.glfwWindowHint(GLFW.GLFW_DEPTH_BITS, 24)
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)

            val contextWindow = GLFW.glfwCreateWindow(1,
                    1,
                    "", MemoryUtil.NULL, primaryWindow)

            thread(isDaemon = true, name = "ResourceThread") {
                GLFW.glfwMakeContextCurrent(contextWindow)
                GL.createCapabilities()
                Driver.instance.clear(ColorRGBa.BLACK)
                f()
                GLFW.glfwDestroyWindow(contextWindow)
            }
            return ResourceThreadGL3()
        }
    }

}