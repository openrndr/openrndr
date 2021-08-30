package org.openrndr

actual fun rootClassName(): String {
    return "unknown"
}

actual fun Program.namedTimestamp(extension: String, path: String?): String {
    val ext = when {
        extension.isEmpty() -> ""
        extension.startsWith(".") -> extension
        else -> ".$extension"
    }
    val basename = this.name.ifBlank { this.window.title.ifBlank { "untitled" } }

    return "$path$basename$ext"
}