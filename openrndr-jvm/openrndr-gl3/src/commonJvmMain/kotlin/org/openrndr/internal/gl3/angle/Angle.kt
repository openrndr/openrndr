package org.openrndr.internal.gl3.angle

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.system.Configuration
import org.openrndr.internal.gl3.DriverGL3Configuration
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformArchitecture
import java.io.File
import java.nio.file.Files

private class Angle

private val logger = KotlinLogging.logger { }

/**
 * The directory the ANGLE libraries were extracted to by [extractAngleLibraries], null before
 */
var angleLibraryDirectory: File? = null
    private set

/** ANGLE's libEGL, null before [extractAngleLibraries] */
val angleEGLLibrary: File? get() = angleLibraryDirectory?.let { File(it, "libEGL.dylib") }

/** ANGLE's libGLESv2, null before [extractAngleLibraries] */
val angleGLESLibrary: File? get() = angleLibraryDirectory?.let { File(it, "libGLESv2.dylib") }

/**
 * Extracts the ANGLE libraries from resources.
 *
 * Without [directory] the libraries are extracted to a new temporary directory, or to the directory set with the
 * `org.openrndr.gl3.angle_directory` property, and LWJGL loads them by their absolute paths. The working directory
 * can be read-only, like the working directory `/` of applications opened from the Finder. Application back-ends
 * load the libraries from [angleEGLLibrary] and [angleGLESLibrary]; ANGLE's libEGL finds libGLESv2 and libMoltenVK
 * next to itself.
 *
 * @param directory the directory to extract to, for back-ends that load the libraries by name from the working
 * directory
 * @return the directory the libraries were extracted to
 */
fun extractAngleLibraries(directory: File? = null): File {
    angleLibraryDirectory?.let { return it }

    val platform = "macos"
    val arch = when (Platform.architecture) {
        PlatformArchitecture.X86_64 -> "x64"
        PlatformArchitecture.AARCH64 -> "arm64"
        else -> error("architecture not supported")
    }

    val libraries = listOf("libEGL.dylib", "libGLESv2.dylib", "libMoltenVK.dylib")

    val configuredDirectory = directory ?: Platform.property("org.openrndr.gl3.angle_directory")?.let { File(it) }
    val targetDirectory = configuredDirectory?.also { it.mkdirs() }
        ?: Files.createTempDirectory("openrndr-angle").toFile().also {
            if (DriverGL3Configuration.deleteAngleOnExit) {
                // files are deleted in reverse order of registration, the directory after its libraries
                it.deleteOnExit()
            }
        }
    logger.debug { "extracting ANGLE libraries to $targetDirectory" }

    for (library in libraries) {
        val resource = Angle::class.java.getResourceAsStream("/org/openrndr/internal/gl3/angle/$platform/$arch/$library")
            ?: error("$library not found in resources")
        val target = File(targetDirectory, library)

        val preExists = target.exists()
        if (!preExists || DriverGL3Configuration.overwriteExistingAngle) {
            resource.use { input ->
                target.outputStream().use { input.copyTo(it) }
            }
        } else {
            resource.close()
        }

        if (!preExists && DriverGL3Configuration.deleteAngleOnExit) {
            target.deleteOnExit()
        }
    }

    val extracted = targetDirectory.absoluteFile
    angleLibraryDirectory = extracted
    if (directory == null) {
        // LWJGL's EGL and GLES bindings load the libraries by these names instead of from the working directory
        Configuration.EGL_LIBRARY_NAME.set(File(extracted, "libEGL.dylib").absolutePath)
        Configuration.OPENGLES_LIBRARY_NAME.set(File(extracted, "libGLESv2.dylib").absolutePath)
    }
    return extracted
}
