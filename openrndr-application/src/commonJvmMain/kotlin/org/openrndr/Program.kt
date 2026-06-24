package org.openrndr

import java.time.LocalDateTime

actual fun Program.namedTimestamp(extension: String, path: String?):
        String {
    val now = LocalDateTime.now()
    val basename = this.name.ifBlank { this.window.title.ifBlank { "untitled" } }
    val computedPath = when {
        path.isNullOrBlank() -> ""
        path.endsWith("/") -> path
        else -> "$path/"
    }
    val ext = when {
        extension.isEmpty() -> ""
        extension.startsWith(".") -> extension
        else -> ".$extension"
    }

    return "$computedPath$basename-%04d-%02d-%02d-%02d.%02d.%02d$ext".format(
        now.year, now.month.value, now.dayOfMonth,
        now.hour, now.minute, now.second)
}