package org.openrndr.dialogs

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.util.nfd.NFDFilterItem
import org.lwjgl.util.nfd.NFDPathSetEnum
import org.lwjgl.util.nfd.NativeFileDialog.*
import org.openrndr.exceptions.stackRootClassName
import org.openrndr.platform.Platform
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Paths
import java.util.*

private val logger = KotlinLogging.logger {}

val nfd by lazy {
    NFD_Init()
}

/**
 * Returns the default path for a context
 * @param programName optional name of the program, default is guessed from a stack trace on the current thread
 * @param contextID optional context identifier, the default is "global"
 * @return String encoded path or null
 * @see setDefaultPathForContext
 * @since 0.3.40
 */
fun getDefaultPathForContext(programName: String = stackRootClassName(), contextID: String = "global"): String? {
    val props = Properties()
    return try {
        val f = File(Platform.supportDirectory(programName), ".file-dialog.properties")
        if (f.exists()) {
            val `is` = FileInputStream(f)
            props.load(`is`)
            `is`.close()
            val path = props.getProperty("$programName.$contextID", null)
            logger.debug {
                "Resolved default path for '$programName::$contextID' to '$path'"
            }
            path
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Set the default path for a context
 * This is an internal function, but it can be used to set a default path before calling any of the file dialog functions
 * @param programName optional name of the program, default is guessed from a stack trace on the current thread
 * @param contextID optional context identifier, the default is "global"
 * @param file the path to set as a default
 * @see getDefaultPathForContext
 * @since 0.3.40
 */
fun setDefaultPathForContext(programName: String = stackRootClassName(), contextID: String = "global", file: File) {
    logger.debug {
        "Setting default path for '$programName::$contextID' to '${file.absolutePath}'"
    }
    val props = Properties()
    val defaultPath = file.let { if (it.isDirectory) it.absolutePath else it.parentFile.absolutePath }

    try {
        val f = File(Platform.supportDirectory(programName), ".file-dialog.properties")

        if (!f.exists()) {
            f.createNewFile()
        }
        val `is` = FileInputStream(f)
        props.load(`is`)
        props.setProperty("$programName.$contextID", defaultPath)
        `is`.close()
        val os = FileOutputStream(f)
        props.store(os, "File dialog properties for $programName")
        os.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Creates a file dialog that can be used to open a single file
 * @param programName optional name of the program, this is guessed from a stack trace by default
 * @param contextID optional context identifier, default is "global"
 * @param supportedExtensions a list with supported/allowed
 * extensions sets. Usage: listOf("vector" to listOf("svg", "ai"),
 *                                "bitmap" to listOf("jpg", "JPG"))
 * @param function the function to be invoked when a file has been picked
 * @see openFilesDialog
 * @see saveFileDialog
 */
fun openFileDialog(
    programName: String = stackRootClassName(),
    contextID: String = "global",
    defaultPath: String? = getDefaultPathForContext(programName, contextID),
    supportedExtensions: List<Pair<String, List<String>>> = emptyList(),
    function: (File) -> Unit
) {
    nfd
    stackPush().use { stack ->
        val filterList = filterItems(stack, supportedExtensions)
        val out = stack.mallocPointer(1)

        val r = NFD_OpenDialog(out, filterList, defaultPath)
        if (r == NFD_OKAY) {
            val ptr = out.get(0)
            val str = memUTF8(ptr)
            val f = File(str)
            setDefaultPathForContext(programName, contextID, f)
            function(f)
        }
    }
}

/**
 * Creates a file dialog that can be used to open multiple files
 * @param programName optional name of the program, this is guessed from a stack trace by default
 * @param contextID optional context identifier, default is "global"
 * @param supportedExtensions a list with supported/allowed
 * extensions sets. Usage: listOf("vector" to listOf("svg", "ai"),
 *                                "bitmap" to listOf("jpg", "JPG"))
 * @param function the function to be invoked when a file has been picked
 * @see openFileDialog
 * @see saveFileDialog
 */
fun openFilesDialog(
    programName: String = stackRootClassName(),
    contextID: String = "global",
    defaultPath: String? = getDefaultPathForContext(programName, contextID),
    supportedExtensions: List<Pair<String, List<String>>> = emptyList(),
    function: (List<File>) -> Unit
) {
    nfd
    stackPush().use { stack ->
        val filterList = filterItems(stack, supportedExtensions)
        val pp = stack.mallocPointer(1)

        val r = NFD_OpenDialogMultiple(pp, filterList, defaultPath)
        val files = mutableListOf<File>()

        if (r == NFD_OKAY) {
            val pathSet: Long = pp.get(0)
            val psEnum = NFDPathSetEnum.calloc(stack)
            NFD_PathSet_GetEnum(pathSet, psEnum)

            while (NFD_PathSet_EnumNext(psEnum, pp) == NFD_OKAY && pp.get(0) != NULL) {
                files.add(File(pp.getStringUTF8(0)))
                NFD_PathSet_FreePath(pp.get(0))
            }

            NFD_PathSet_FreeEnum(psEnum)
            NFD_PathSet_Free(pathSet)

        }
        if (files.isNotEmpty()) {
            setDefaultPathForContext(programName, contextID, files[0])
            function(files)
        }

    }
}

/**
 * Creates a file dialog that can be used to open multiple files
 * @param programName optional name of the program, this is guessed from a stack trace by default
 * @param contextID optional context identifier, default is "global"
 * @param function the function to be invoked when a file has been picked
 */
fun openFolderDialog(
    programName: String = stackRootClassName(),
    contextID: String = "global",
    defaultPath: String? = getDefaultPathForContext(programName, contextID),
    function: (File) -> Unit
) {
    nfd
    stackPush().use { stack ->
        val out = stack.mallocPointer(1)
        val r = NFD_PickFolder(out, defaultPath)
        if (r == NFD_OKAY) {
            val ptr = out.get(0)
            val str = memUTF8(ptr)
            val f = File(str)
            setDefaultPathForContext(programName, contextID, f)
            function(f)
        }
    }
}

/**
 * Creates a file dialog that can be used to save a single file files
 * @param programName optional name of the program, this is guessed from a stack trace by default
 * @param contextID optional context identifier, default is "global"
 * @param suggestedFilename an optional suggestion for a filename
 * @param supportedExtensions an optional list with supported/allowed
 * extensions sets. Usage: listOf("vector" to listOf("svg", "ai"),
 *                                "bitmap" to listOf("jpg", "JPG"))
 * The first extension in the list is applied when none provided.
 * @param function the function to be invoked when a file has been picked
 */
fun saveFileDialog(
    programName: String = stackRootClassName(),
    contextID: String = "global",
    defaultPath: String? = getDefaultPathForContext(programName, contextID),
    suggestedFilename: String? = null,
    supportedExtensions: List<Pair<String, List<String>>> = emptyList(),
    function: (File) -> Unit
) {

    nfd
    stackPush().use { stack ->
        val filterList = filterItems(stack, supportedExtensions)
        val defaultPathBase = Paths.get(
            defaultPath ?: getDefaultPathForContext(programName, contextID)
            ?: "."
        ).normalize().toString()


        logger.debug { "Default path is $defaultPathBase" }

        val out = stack.mallocPointer(1)
        when (NFD_SaveDialog(out, filterList, defaultPathBase, suggestedFilename)) {
            NFD_OKAY -> {
                val ptr = out.get(0)
                val pickedFilename = memUTF8(ptr)
                val pickedFile = File(pickedFilename)
                val finalFile =
                    if (supportedExtensions.isNotEmpty() && supportedExtensions.none { pickedFile.extension in it.second }) {
                        val fixedFilename = "$pickedFilename.${supportedExtensions.first().second.first()}"
                        logger.warn { "User has picked either no or an unsupported extension, fixed filename to '$fixedFilename'" }
                        File(fixedFilename)
                    } else {
                        pickedFile
                    }
                try {
                    function(finalFile)
                } catch (e: Throwable) {
                    logger.error { "Caught exception while saving to file '${finalFile.absolutePath}" }
                    throw e
                }
                setDefaultPathForContext(programName, contextID, finalFile)
            }

            NFD_ERROR -> {
                logger.error { "error NFD_SaveDialog returned NFD_ERROR" }
            }
        }
    }
}

/**
 * Helper function to join the provided extensions as one string with
 * separator characters
 */
private fun filterItems(stack: MemoryStack, extensions: List<Pair<String, List<String>>>): NFDFilterItem.Buffer? {
    return if (extensions.isNotEmpty()) {
        val filters = NFDFilterItem.malloc(extensions.size)
        for (i in extensions.indices) {
            filters.get(i)
                .name(stack.UTF8(extensions[i].first))
                .spec(stack.UTF8(extensions[i].second.joinToString(",")))
        }
        filters
    } else {
        null
    }
}