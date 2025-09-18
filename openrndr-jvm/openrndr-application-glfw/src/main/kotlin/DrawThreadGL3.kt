package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.GLFW_CLIENT_API
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_CREATION_API
import org.lwjgl.glfw.GLFW.GLFW_EGL_CONTEXT_API
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_ES_API
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE
import org.lwjgl.glfw.GLFW.glfwWindowHint
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL43C
import org.lwjgl.opengles.GLES
import org.lwjgl.system.MemoryUtil
import org.openrndr.Dispatcher
import org.openrndr.draw.DrawThread
import org.openrndr.draw.Drawer
import org.openrndr.internal.Driver
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {  }

class DrawThreadGL3(private val contextWindow: Long) : DrawThread {
    companion object {
        fun create(primaryWindow: Long): DrawThreadGL3 {

            GLFW.glfwDefaultWindowHints()
            val version = (Driver.instance as DriverGL3).version
            logger.debug { "creating new GL context (version ${version.majorVersion}.${version.minorVersion}" }
            glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, version.majorVersion)
            glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, version.minorVersion)
            when (Driver.glType) {
                DriverTypeGL.GL -> {
                    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL43C.GL_TRUE)
                    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
                }

                DriverTypeGL.GLES -> {
                    glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_ES_API)
                    glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_EGL_CONTEXT_API)
                }
            }
            glfwWindowHint(GLFW.GLFW_RED_BITS, 8)
            glfwWindowHint(GLFW.GLFW_GREEN_BITS, 8)
            glfwWindowHint(GLFW.GLFW_BLUE_BITS, 8)
            glfwWindowHint(GLFW.GLFW_STENCIL_BITS, 8)
            glfwWindowHint(GLFW.GLFW_DEPTH_BITS, 24)
            glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
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

            when (Driver.glType) {
                DriverTypeGL.GL -> GL.createCapabilities()
                DriverTypeGL.GLES -> GLES.createCapabilities()
            }

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