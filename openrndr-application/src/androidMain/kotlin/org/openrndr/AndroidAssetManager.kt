package org.openrndr

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object AndroidAssetManager {

    fun copyAssetsToFilesDir(context: Context) {
        copyAssetFolder(context, "", context.filesDir.absolutePath)
    }

    private fun copyAssetFolder(context: Context, assetPath: String, destinationPath: String): Boolean {
        return try {
            val assets = context.assets
            val files = assets.list(assetPath) ?: return false

            if (files.isEmpty()) {
                // It's a file
                copyAssetFile(context, assetPath, destinationPath)
            } else {
                // It's a folder
                val dir = if (assetPath.isEmpty())
                    File(destinationPath)
                else
                    File(destinationPath, assetPath)

                if (!dir.exists()) dir.mkdirs()

                for (file in files) {
                    val childAssetPath = if (assetPath.isEmpty()) file else "$assetPath/$file"
                    copyAssetFolder(context, childAssetPath, destinationPath)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun copyAssetFile(context: Context, assetPath: String, destinationPath: String) {
        val outFile = File(destinationPath, assetPath)
        outFile.parentFile?.mkdirs()

        context.assets.open(assetPath).use { inputStream ->
            FileOutputStream(outFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
}