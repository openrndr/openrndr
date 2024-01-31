package org.openrndr.internal.gl3.angle

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.platform.Platform
import java.io.File

private class Angle

private val logger = KotlinLogging.logger { }
fun loadAngleLibraries() {
    logger.info { "Loading ANGLE libraries from resources" }
    val targetDirectory = File(".")// Platform.tempDirectory()
    val egl = Angle::class.java.getResourceAsStream("/org/openrndr/internal/gl3/angle/libEGL.dylib") ?: error("libEGL.dylib not found in resources")
    File(targetDirectory,"libEGL.dylib").outputStream().use {
        egl.copyTo(it)
    }

    val glesv2 = Angle::class.java.getResourceAsStream("/org/openrndr/internal/gl3/angle/libGLESv2.dylib") ?: error("libGLESv2.dylib not found in resources")
    File(targetDirectory,"libGLESv2.dylib").outputStream().use {
        glesv2.copyTo(it)
    }
    File(targetDirectory, "libGLESv2.dylib").deleteOnExit()
    File(targetDirectory, "libEGL.dylib").deleteOnExit()
}

