package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33C.*
import org.lwjgl.system.MemoryUtil
import org.openrndr.Dispatcher
import org.openrndr.draw.DrawThread
import org.openrndr.draw.Drawer
import org.openrndr.internal.Driver
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {  }

class DrawThreadGL3(private val contextWindow: Long) : DrawThread {
    companion object {
        fun create(): DrawThreadGL3 {

            GLFW.glfwDefaultWindowHints()
            val version = (Driver.instance as DriverGL3).version
            logger.debug { "creating new GL context (version ${version.majorVersion}.${version.minorVersion}" }
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, version.majorVersion)
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, version.minorVersion)
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

            require(contextWindow != 0L) { "context creation failed" }

            return DrawThreadGL3(contextWindow)
        }
    }

    private lateinit var realDrawer: Drawer
    private lateinit var realDispatcher: Dispatcher

    override val dispatcher get() = realDispatcher

    override val drawer: Drawer
        get() = realDrawer

    init {
        thread(isDaemon = true, name = "DrawThreadGL3") {
            realDispatcher = Dispatcher()
            GLFW.glfwMakeContextCurrent(contextWindow)
            GL.createCapabilities()
            realDrawer = Drawer(Driver.instance)
            val vaos = IntArray(1)
            glGenVertexArrays(vaos)
            glBindVertexArray(vaos[0])
            val renderTarget = NullRenderTargetGL3()
            renderTarget.bind()
            while (true) {
                dispatcher.execute()
                Thread.sleep(1)
            }
        }
    }
}