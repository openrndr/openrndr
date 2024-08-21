package org.openrndr.internal.gl3.angle

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.internal.gl3.DriverGL3Configuration
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformArchitecture
import java.io.File

private class Angle

private val logger = KotlinLogging.logger { }
fun extractAngleLibraries() {

    val platform = "macos"
    val arch = when (Platform.architecture) {
        PlatformArchitecture.X86_64 -> "x64"
        PlatformArchitecture.AARCH64 -> "arm64"
        else -> error("architecture not supported")
    }


    logger.info { "Loading ANGLE libraries from resources" }
    val targetDirectory = File(".")// Platform.tempDirectory()
    val egl = Angle::class.java.getResourceAsStream("/org/openrndr/internal/gl3/angle/$platform/$arch/libEGL.dylib")
        ?: error("libEGL.dylib not found in resources")
    val targetEgl = File(targetDirectory, "libEGL.dylib")

    if (!targetEgl.exists() || DriverGL3Configuration.overwriteExistingAngle) {
        targetEgl.outputStream().use {
            egl.copyTo(it)
        }
    }


    val targetGlesv2 = File(targetDirectory, "libGLESv2.dylib")
    if (!targetGlesv2.exists() || DriverGL3Configuration.overwriteExistingAngle) {
        val glesv2 = Angle::class.java.getResourceAsStream("/org/openrndr/internal/gl3/angle/$platform/$arch/libGLESv2.dylib") ?: error(
            "libGLESv2.dylib not found in resources"
        )
        targetGlesv2.outputStream().use {
            glesv2.copyTo(it)
        }
    }

    if (DriverGL3Configuration.deleteAngleOnExit) {
        File(targetDirectory, "libGLESv2.dylib").deleteOnExit()
        File(targetDirectory, "libEGL.dylib").deleteOnExit()
    }
}

