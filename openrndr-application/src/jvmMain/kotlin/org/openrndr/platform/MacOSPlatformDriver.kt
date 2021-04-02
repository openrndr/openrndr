package org.openrndr.platform

import java.io.File

class MacOSPlatformDriver : PlatformDriver {
    override fun temporaryDirectory(): File {
        val directoryName = System.getProperty("java.io.tmpdir") + "OPENRNDR-" + randomID
        val file = File(directoryName)
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    override fun cacheDirectory(programName: String): File {
        val directoryName = System.getProperty("user.home") + "/Library/Caches/" + programName
        val file = File(directoryName)
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    override fun supportDirectory(programName: String): File {
        val directoryName = System.getProperty("user.home") + "/Library/Application Support/" + programName
        val file = File(directoryName)
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    companion object {

        var randomID: String
        init {
            val alphabet = charArrayOf('a', 'b', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z')
            var id = ""
            for (i in 0..7) {
                id += alphabet[i]
            }
            randomID = id
        }

    }
}