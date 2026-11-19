package org.openrndr.platform

import java.io.File

class WindowsPlatformDriver : PlatformDriver {

    override fun temporaryDirectory(): File {
        return processTemporaryDirectory("OPENRNDR-$randomID")
    }

    /**
     * The cache directory for [programName] in `%LOCALAPPDATA%\<programName>\Cache`. Caches are local to the machine,
     * they don't roam with the user's profile.
     */
    override fun cacheDirectory(programName: String): File {
        val file = File(File(knownFolder("LOCALAPPDATA", "AppData\\Local"), programName), "Cache")
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    /**
     * The support directory for [programName] in `%APPDATA%\<programName>`, which roams with the user's profile
     */
    override fun supportDirectory(programName: String): File {
        val file = File(knownFolder("APPDATA", "AppData\\Roaming"), programName)
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    /**
     * The known folder in the environment variable [variable], or [default] in the home directory when it is not set
     */
    private fun knownFolder(variable: String, default: String): File {
        val value = System.getenv(variable)
        return if (!value.isNullOrBlank() && File(value).isAbsolute) {
            File(value)
        } else {
            File(System.getProperty("user.home"), default)
        }
    }

    companion object {
        val randomID: String = randomTemporaryID()
    }

    override fun path(): List<File> {
        return System.getenv("PATH").split(File.pathSeparator).filter { it.isNotBlank() }.map { File(it) }
    }

}