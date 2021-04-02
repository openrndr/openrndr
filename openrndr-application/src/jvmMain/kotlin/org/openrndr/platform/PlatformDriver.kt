package org.openrndr.platform

import java.io.File

internal actual interface PlatformDriver {
    fun temporaryDirectory(): File
    fun cacheDirectory(programName: String): File
    fun supportDirectory(programName: String): File

}