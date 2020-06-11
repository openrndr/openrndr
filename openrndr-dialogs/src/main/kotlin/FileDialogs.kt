package org.openrndr.dialogs

import mu.KotlinLogging
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.util.nfd.NFDPathSet
import org.lwjgl.util.nfd.NativeFileDialog
import org.lwjgl.util.nfd.NativeFileDialog.*
import org.openrndr.exceptions.stackRootClassName
import org.openrndr.platform.Platform
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Paths
import java.util.*

private val logger = KotlinLogging.logger {}

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
 * This is an internal function but it can be used to set a default path before calling any of the file dialog functions
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
 * Creates a file dialog that can be used to open a single
 * @param programName optional name of the program, this is guessed from a stack trace by default
 * @param contextID optional context identifier, default is "global"
 * @see openFilesDialog
 * @see saveFileDialog
 */
fun openFileDialog(programName: String = stackRootClassName(), contextID: String = "global", function: (File) -> Unit) {
    openFileDialog(programName, contextID, emptyList(), function)
}

/**
 * Creates a file dialog that can be used to open a single
 * @param programName optional name of the program, this is guessed from a stack trace by default
 * @param contextID optional context identifier, default is "global"
 * @param supportedExtensions a list of supported/allowed extensions
 * @param function the function to be invoked when a file has been picked
 * @see openFilesDialog
 * @see saveFileDialog
 */
fun openFileDialog(programName: String = stackRootClassName(), contextID: String = "global", supportedExtensions: List<String>, function: (File) -> Unit) {
    val filterList: CharSequence? = if (supportedExtensions.isEmpty()) null else supportedExtensions.joinToString(";")
    val defaultPath = Paths.get(getDefaultPathForContext(programName, contextID) ?: ".").normalize().toString()

    val out = memAllocPointer(1)

    val r = NativeFileDialog.NFD_OpenDialog(filterList, defaultPath, out)
    if (r == NFD_OKAY) {
        val ptr = out.get(0)
        val str = memUTF8(ptr)
        val f = File(str)
        setDefaultPathForContext(programName, contextID, f)
        function(f)
    }
    memFree(out)
}

/**
 * Creates a file dialog that can be used to open multiple files
 * @param programName optional name of the program, this is guessed from a stack trace by default
 * @param contextID optional context identifier, default is "global"
 * @param supportedExtensions a list of supported/allowed extensions
 * @param function the function to be invoked when a file has been picked
 * @see openFileDialog
 * @see saveFileDialog
 */
fun openFilesDialog(programName: String = stackRootClassName(), contextID: String = "global", supportedExtensions: List<String>, function: (List<File>) -> Unit) {
    val filterList: CharSequence? = if (supportedExtensions.isEmpty()) null else supportedExtensions.joinToString(";")
    val defaultPath = Paths.get(getDefaultPathForContext(programName, contextID) ?: ".").normalize().toString()

    val pathSet = NFDPathSet.calloc()

    val r = NativeFileDialog.NFD_OpenDialogMultiple(filterList, defaultPath, pathSet)
    val files = mutableListOf<File>()
    if (r == NFD_OKAY) {
        for (i in 0 until NFD_PathSet_GetCount(pathSet)) {
            val result = NFD_PathSet_GetPath(pathSet, i)
            if (result != null) {
                files.add(File(result))
            }
        }
    }
    NFD_PathSet_Free(pathSet)
    if (files.isNotEmpty()) {
        setDefaultPathForContext(programName, contextID, files[0])
        function(files)
    }
}

/**
 * Creates a file dialog that can be used to open multiple files
 * @param programName optional name of the program, this is guessed from a stack trace by default
 * @param contextID optional context identifier, default is "global"
 * @param function the function to be invoked when a file has been picked
 */
fun openFilesDialog(programName: String = stackRootClassName(), contextID: String = "global", function: (List<File>) -> Unit) {
    openFilesDialog(programName, contextID, emptyList(), function)
}

/**
 * Creates a file dialog that can be used to open multiple files
 * @param programName optional name of the program, this is guessed from a stack trace by default
 * @param contextID optional context identifier, default is "global"
 * @param function the function to be invoked when a file has been picked
 */
fun openFolderDialog(programName: String = stackRootClassName(), contextID: String = "global", function: (File) -> Unit) {
    val defaultPath: CharSequence? = getDefaultPathForContext(programName, contextID)
    val out = memAllocPointer(1)

    val r = NativeFileDialog.NFD_PickFolder(defaultPath, out)
    if (r == NFD_OKAY) {
        val ptr = out.get(0)
        val str = memUTF8(ptr)
        val f = File(str)
        setDefaultPathForContext(programName, contextID, f)
        function(f)
    }
    memFree(out)
}

/**
 * Creates a file dialog that can be used to save a single file files
 * @param programName optional name of the program, this is guessed from a stack trace by default
 * @param contextID optional context identifier, default is "global"
 * @param suggestedFilename an optional suggestion for a filename
 * @param supportedExtensions an optional list of supported/allowed extensions
 * @param function the function to be invoked when a file has been picked
 */
fun saveFileDialog(
        programName: String = stackRootClassName(),
        contextID: String = "global",
        suggestedFilename: String? = null,
        supportedExtensions: List<String> = emptyList(),
        function: (File) -> Unit
) {
    val filterList = if (supportedExtensions.isEmpty()) null else supportedExtensions.joinToString(";")
    val defaultPathBase = Paths.get(getDefaultPathForContext(programName, contextID) ?: ".").normalize().toString()

    val defaultPath = if (suggestedFilename == null) {
        defaultPathBase
    } else {
        if (defaultPathBase == null) {
            suggestedFilename
        } else {
            File(defaultPathBase, suggestedFilename).absolutePath
        }
    }
    logger.debug { "Default path is $defaultPath" }

    val out = memAllocPointer(1)
    val r = NFD_SaveDialog(filterList, defaultPath, out)
    if (r == NFD_OKAY) {
        val ptr = out.get(0)
        val pickedFilename = memUTF8(ptr)
        val pickedFile = File(pickedFilename)

        val finalFile = if (supportedExtensions.isNotEmpty() && pickedFile.extension !in supportedExtensions) {
            val fixedFilename = pickedFilename + ".${supportedExtensions.first()}"
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
    } else if (r == NFD_ERROR) {
        logger.error { "error NFD_SaveDialog returned NFD_ERROR" }
    }
    memFree(out)
}