package org.openrndr.utils

import org.openrndr.Program
import java.time.LocalDateTime

/**
 * Use to automatically generate a String like
 * `path/basename-date.extension`
 * when saving files like videos, images, json, etc.
 * `basename` defaults to the current program or window name.
 * Used by ScreenRecorder, Screenshots and available to the user.
 */
fun Program.namedTimestamp(extension: String, folder: String? = null): String {
    val now = LocalDateTime.now()
    val basename = this.name.ifBlank { this.window.title.ifBlank { "untitled" } }
    val path = when {
        folder.isNullOrBlank() -> ""
        folder.endsWith("/") -> folder
        else -> "$folder/"
    }

    return "$path$basename-%04d-%02d-%02d-%02d.%02d.%02d.$extension".format(
            now.year, now.month.value, now.dayOfMonth,
            now.hour, now.minute, now.second)
}