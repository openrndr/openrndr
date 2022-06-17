package org.openrndr.internal.gl3

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryStack.*
import org.openrndr.*

class ApplicationBaseGLFWGL3 : ApplicationBase() {
    init {
        if (!glfwInit()) {
            throw IllegalStateException("Unable to initialize GLFW")
        }
    }

    override val displays: List<DisplayGLFWGL3> by lazy {
        val detectedMonitors = glfwGetMonitors()
        if (detectedMonitors != null && detectedMonitors.limit() > 0) {
            stackPush().use {
                val x = it.mallocInt(1)
                val y = it.mallocInt(1)
                val contentScale = it.mallocFloat(1)
                return@lazy List(detectedMonitors.limit()) { i ->
                    val monitor = detectedMonitors[i]
                    val videoMode = glfwGetVideoMode(monitor)
                    glfwGetMonitorPos(monitor, x, y)
                    // vertical scale is reportedly flaky, so we disregard it
                    glfwGetMonitorContentScale(monitor, contentScale, null)
                    DisplayGLFWGL3(
                        monitor, glfwGetMonitorName(monitor), x[0], y[0],
                        videoMode?.width(), videoMode?.height(), contentScale[0].toDouble()
                    )
                }
            }
        } else {
            emptyList()
        }
    }

    override fun build(program: Program, configuration: Configuration): Application {
        return ApplicationGLFWGL3(program, configuration)
    }
}