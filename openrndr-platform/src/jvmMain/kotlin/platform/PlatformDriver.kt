package org.openrndr.platform

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * The conventions of an operating system for where programs keep their files
 */
internal actual interface PlatformDriver {
    /**
     * The temporary directory of this process. It is created when it doesn't exist and is deleted with everything in
     * it when the JVM exits.
     */
    fun temporaryDirectory(): File

    /**
     * The directory in which [programName] keeps files it can recreate, such as downloads and derived data. The
     * operating system or the user may delete its contents at any time. It is created when it doesn't exist.
     */
    fun cacheDirectory(programName: String): File

    /**
     * The directory in which [programName] keeps files that should persist, such as settings and user data. It is
     * created when it doesn't exist.
     */
    fun supportDirectory(programName: String): File

    /**
     * The directories in the `PATH` environment variable, in the order they are searched
     */
    fun path(): List<File>
}

/**
 * A random identifier of 8 lowercase letters, which keeps the temporary directories of processes apart
 */
internal fun randomTemporaryID(): String {
    val alphabet = "abcdefghijklmnopqrstuvwxyz"
    return (0 until 8).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
}

private val deletedOnExit = ConcurrentHashMap.newKeySet<File>()

/**
 * The directory [name] in `java.io.tmpdir`, created when it doesn't exist. The directory and everything in it is
 * deleted when the JVM exits.
 */
internal fun processTemporaryDirectory(name: String): File {
    val file = File(System.getProperty("java.io.tmpdir"), name)
    if (!file.exists()) {
        file.mkdirs()
    }
    if (deletedOnExit.add(file)) {
        Runtime.getRuntime().addShutdownHook(Thread({ file.deleteRecursively() }, "delete $name"))
    }
    return file
}
