package org.openrndr.platform

import java.io.File

class MacOSPlatformDriver : PlatformDriver {
    override fun temporaryDirectory(): File {
        return processTemporaryDirectory("OPENRNDR-$randomID")
    }

    /**
     * The cache directory for [programName] in `~/Library/Caches`
     */
    override fun cacheDirectory(programName: String): File {
        val file = File(libraryDirectory("Caches"), programName)
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    /**
     * The support directory for [programName] in `~/Library/Application Support`
     */
    override fun supportDirectory(programName: String): File {
        val file = File(libraryDirectory("Application Support"), programName)
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    override fun path(): List<File> {
        return System.getenv("PATH").split(File.pathSeparator).map { File(it) }
    }

    private fun libraryDirectory(name: String): File {
        return File(File(System.getProperty("user.home"), "Library"), name)
    }

    companion object {
        var randomID: String = randomTemporaryID()
    }
}