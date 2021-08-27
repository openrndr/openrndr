package org.openrndr.platform

import java.io.File
import java.util.*

actual object Platform {
    private val driver : PlatformDriver = instantiateDriver()
    private fun instantiateDriver(): PlatformDriver {
        val os = System.getProperty("os.name").lowercase(Locale.getDefault())
        return when {
            os.startsWith("windows") -> WindowsPlatformDriver()
            os.startsWith("mac") -> MacOSPlatformDriver()
            else -> GenericPlatformDriver()
        }
    }

    actual val type: PlatformType
        get() {
            val os = System.getProperty("os.name").lowercase(Locale.getDefault())
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