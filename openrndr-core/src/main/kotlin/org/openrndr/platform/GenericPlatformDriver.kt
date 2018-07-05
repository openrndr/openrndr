package org.openrndr.platform

import java.io.File

class GenericPlatformDriver : PlatformDriver {

    override fun temporaryDirectory(): File {
        val directoryName = System.getProperty("java.io.tmpdir") + "RNDR-" + randomID
        val file = File(directoryName)
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    override fun cacheDirectory(programName: String): File {
        val f = File("./cache")
        if (f.exists()) {
            val result = f.mkdirs()
            if (!result) {
                throw RuntimeException("could not create cache directory")
            }
        }
        return f
    }

    override fun supportDirectory(programName: String): File {
        return File(".")
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