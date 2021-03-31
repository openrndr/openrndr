package org.openrndr.draw

import org.openrndr.Program
import java.io.File
import java.net.MalformedURLException
import java.net.URL

fun Program.loadFont(fileOrUrl: String, size: Double, characterSet: Set<Char> = org.openrndr.draw.defaultFontmapCharacterSet, contentScale: Double = this.drawer.context.contentScale): FontImageMap {

    val activeSet = if (characterSet.contains(' ')) characterSet else (characterSet + ' ')

    return try {
        URL(fileOrUrl)
        FontImageMap.fromUrl(fileOrUrl, size, activeSet, contentScale)
    } catch (e: MalformedURLException) {
        val file = File(fileOrUrl)
        require(file.exists()) {
            "failed to load font: file '${file.absolutePath}' does not exist."
        }
        require(file.extension.toLowerCase() in setOf("ttf", "otf")) {
            "failed to load font: file '${file.absolutePath}' is not a .ttf or .otf file"
        }
        FontImageMap.fromFile(fileOrUrl, size, activeSet, contentScale)
    }
}
