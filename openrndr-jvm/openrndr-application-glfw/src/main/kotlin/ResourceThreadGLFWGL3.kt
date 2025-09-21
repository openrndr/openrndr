package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL43C
import org.lwjgl.opengles.GLES
import org.lwjgl.system.MemoryUtil
import org.openrndr.color.ColorRGBa
import org.openrndr.internal.Driver
import org.openrndr.internal.ResourceThread
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {  }

/**
 * A specialized implementation of the `ResourceThread` interface tailored for use with OpenGL
 * or OpenGL ES.
 *
 * `ResourceThreadGL3` is used to initialize and manage a dedicated rendering context in a separate
 * thread. This ensures that OpenGL-related resources can be managed independently without interfering
 * with the primary graphics thread.
 *
 * This class manages the creation of a hidden GLFW window and sets up an OpenGL or OpenGL ES context
 * depending on the driver's configuration. Resources are processed within the thread, and a default
 * render target (`NullRenderTargetGL3`) is used for binding. This ensures a consistent and isolated
 * environment for resource operations.
 *
 * The `create` function sets thread-specific properties, initializes OpenGL/GLES capabilities, and
 * invokes the provided lambda (`f`) within the resource processing thread. Cleanup of the created
 * context and GLFW window is handled automatically after the lambda finishes execution.
 */
class ResourceThreadGLFWGL3 : ResourceThread {
    companion object {
        fun create(primaryWindow: Long, f: () -> Unit): ResourceThreadGLFWGL3 {
            glfwDefaultWindowHints()
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, Driver.glVersion.majorVersion)
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, Driver.glVersion.minorVersion)

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
            glfwWindowHint(GLFW_RED_BITS, 8)
            glfwWindowHint(GLFW_GREEN_BITS, 8)
            glfwWindowHint(GLFW_BLUE_BITS, 8)
            glfwWindowHint(GLFW_STENCIL_BITS, 8)
            glfwWindowHint(GLFW_DEPTH_BITS, 24)
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)

            val contextWindow = glfwCreateWindow(1,
                    1,
                    "", MemoryUtil.NULL, primaryWindow)

            thread(isDaemon = true, name = "ResourceThread") {
                logger.debug { "Context thread starting" }
                glfwMakeContextCurrent(contextWindow)

                when (Driver.glType) {
                    DriverTypeGL.GL -> GL.createCapabilities()
                    DriverTypeGL.GLES -> GLES.createCapabilities()
                }
                val n = NullRenderTargetGL3()
                n.bind()
                Driver.instance.clear(ColorRGBa.BLACK)
                try {
                    f()
                } catch(e: Throwable) {
                    logger.error(e) { "Caught exception in resource thread." }
                } finally {
                    logger.debug { "Context thread exiting" }
                    (Driver.instance as DriverGL3).executeOnMainThread {
                        logger.debug { "Destroying resource thread context $contextWindow" }
                        Driver.instance.destroyContext(contextWindow)
                        glfwDestroyWindow(contextWindow)
                    }
                }
            }
            return ResourceThreadGLFWGL3()
        }
    }
}