package org.openrndr.platform

import java.io.File
import java.nio.file.Path
import java.util.*

actual object Platform {

    actual fun property(key: String): String? {
        return System.getProperty(key)
    }

    private val driver: PlatformDriver = instantiateDriver()
    private fun instantiateDriver(): PlatformDriver {
        val os = System.getProperty("os.name").lowercase(Locale.getDefault())
        return when {
            os.startsWith("windows") -> WindowsPlatformDriver()
            os.startsWith("mac") -> MacOSPlatformDriver()
            else -> GenericPlatformDriver()
        }
    }

    actual val type: PlatformType by lazy {
        val os = System.getProperty("os.name").lowercase(Locale.getDefault())
        when {
            os.startsWith("windows") -> PlatformType.WINDOWS
            os.startsWith("mac") -> PlatformType.MAC
            else -> PlatformType.GENERIC
        }
    }

    actual val architecture: PlatformArchitecture by lazy {
        val arch = System.getProperty("os.arch").lowercase()
        when (arch) {
            "amd64" -> PlatformArchitecture.X86_64
            "aarch64" -> PlatformArchitecture.AARCH64
            else -> PlatformArchitecture.UNKNOWN
        }
    }

    fun tempDirectory(): File {
        return driver.temporaryDirectory()
    }

    fun cacheDirectory(programName: String): File {
        return driver.cacheDirectory(programName)
    }

    fun supportDirectory(programName: String): File {
        return driver.supportDirectory(programName)
    }

    fun path(): List<File> {
        return driver.path()
    }
}