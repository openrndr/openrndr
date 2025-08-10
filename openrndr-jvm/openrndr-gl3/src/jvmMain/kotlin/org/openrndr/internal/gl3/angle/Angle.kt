package org.openrndr.internal.gl3.angle

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.internal.gl3.DriverGL3Configuration
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformArchitecture
import java.io.File

private class Angle

private val logger = KotlinLogging.logger { }

/**
 * Extracts ANGLE libraries from resources to the current working directory.
 */
fun extractAngleLibraries() {

    val platform = "macos"
    val arch = when (Platform.architecture) {
        PlatformArchitecture.X86_64 -> "x64"
        PlatformArchitecture.AARCH64 -> "arm64"
        else -> error("architecture not supported")
    }

    val libraries = listOf("libEGL.dylib", "libGLESv2.dylib", "libMoltenVK.dylib")

    logger.debug { "Loading ANGLE libraries from resources" }
    val targetDirectory = File(".")

    for (library in libraries) {
        val resource = Angle::class.java.getResourceAsStream("/org/openrndr/internal/gl3/angle/$platform/$arch/$library")
            ?: error("$library not found in resources")
        val target = File(targetDirectory, library)

        val preExists = target.exists()
        if (!target.exists() || DriverGL3Configuration.overwriteExistingAngle) {
            target.outputStream().use {
                resource.copyTo(it)
            }
        }

        if (!preExists && DriverGL3Configuration.deleteAngleOnExit) {
            target.deleteOnExit()
        }
    }
}

