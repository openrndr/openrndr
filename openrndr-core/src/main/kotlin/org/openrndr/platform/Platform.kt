package org.openrndr.platform

import java.io.File

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