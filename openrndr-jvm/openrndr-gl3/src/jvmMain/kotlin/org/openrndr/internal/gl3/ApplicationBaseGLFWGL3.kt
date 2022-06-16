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

    override val displays: List<Display> by lazy {
        val detectedMonitors = glfwGetMonitors()
        if (detectedMonitors != null && detectedMonitors.limit() > 0) {
            stackPush().use {
                val x = it.mallocInt(1)
                val y = it.mallocInt(1)
                val xScale = it.mallocFloat(1)
                val yScale = it.mallocFloat(1)
                return@lazy List(detectedMonitors.limit()) { i ->
                    val monitor = detectedMonitors[i]
                    val videoMode = glfwGetVideoMode(monitor)
                    glfwGetMonitorPos(monitor, x, y)
                    glfwGetMonitorContentScale(monitor, xScale, yScale)
                    Display(
                        monitor, glfwGetMonitorName(monitor),
                        x[0], y[0],
                        videoMode?.width(), videoMode?.height(),
                        xScale[0].toDouble(), yScale[0].toDouble()
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