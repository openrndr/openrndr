package org.openrndr.platform

import java.io.File

enum class PlatformType {
    GENERIC,
    WINDOWS,
    MAC
}

object Platform {
    private val driver = instantiateDriver()
    private fun instantiateDriver(): PlatformDriver {
        val os = System.getProperty("os.name").toLowerCase()
        return when {
            os.startsWith("windows") -> WindowsPlatformDriver()
            os.startsWith("mac") -> MacOSPlatformDriver()
            else -> GenericPlatformDriver()
        }
    }

    val type: PlatformType
    get() {
        val os = System.getProperty("os.name").toLowerCase()
        return when {
            os.startsWith("windows") -> PlatformType.WINDOWS
            os.startsWith("mac") -> PlatformType.MAC
            else -> PlatformType.GENERIC
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
}