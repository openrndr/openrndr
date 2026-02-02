package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryStack.*
import org.openrndr.*
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.fontdriver.stb.FontDriverStbTt
import org.openrndr.internal.ImageDriver
import org.openrndr.internal.KeyboardDriver
import org.openrndr.internal.gl3.angle.extractAngleLibraries

private val logger = KotlinLogging.logger { }

class ApplicationBaseGLFWGL3 : ApplicationBase() {
    init {
        logger.debug { "initializing ApplicationBaseGLFWGL3" }
        if (!ApplicationConfiguration.checkThread0) {
            org.lwjgl.system.Configuration.GLFW_CHECK_THREAD0.set(false)
        }

        if (DriverGL3Configuration.driverType == DriverTypeGL.GLES && DriverGL3Configuration.glesBackend == GlesBackend.ANGLE) {
            extractAngleLibraries()
        }

        if (!glfwInit()) {
            throw IllegalStateException("Unable to initialize GLFW")
        }

        KeyboardDriver.driver = KeyboardDriverGLFW()
        ImageDriver.driver = ImageDriverStbImage()
        FontDriver.driver = FontDriverStbTt()
    }

    override val displays: List<DisplayGLFW> by lazy {
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
                    videoMode?.refreshRate()
                    // vertical scale is reportedly flaky, so we disregard it
                    glfwGetMonitorContentScale(monitor, contentScale, null)

                    DisplayGLFW(
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