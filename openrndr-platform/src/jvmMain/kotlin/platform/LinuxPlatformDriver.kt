package org.openrndr.platform

import java.io.File

class LinuxPlatformDriver : PlatformDriver {

    override fun temporaryDirectory(): File {
        return processTemporaryDirectory("OPENRNDR-$randomID")
    }

    /**
     * The cache directory for [programName] in `$XDG_CACHE_HOME`, or in `~/.cache` when it is not set
     */
    override fun cacheDirectory(programName: String): File {
        val file = File(xdgDirectory("XDG_CACHE_HOME", ".cache"), programName)
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    /**
     * The support directory for [programName] in `$XDG_DATA_HOME`, or in `~/.local/share` when it is not set
     */
    override fun supportDirectory(programName: String): File {
        val file = File(xdgDirectory("XDG_DATA_HOME", ".local/share"), programName)
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    /**
     * The base directory in the environment variable [variable], or [default] in the home directory when it is not
     * set. The XDG base directory specification says that relative paths are invalid and should be ignored.
     */
    private fun xdgDirectory(variable: String, default: String): File {
        val value = System.getenv(variable)
        return if (!value.isNullOrBlank() && File(value).isAbsolute) {
            File(value)
        } else {
            File(System.getProperty("user.home"), default)
        }
    }

    companion object {
        var randomID: String = randomTemporaryID()
    }

    override fun path(): List<File> {
        return System.getenv("PATH").split(":").map { File(it) }
    }
}