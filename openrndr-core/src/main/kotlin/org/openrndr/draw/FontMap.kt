package org.openrndr.draw

import org.openrndr.Program
import org.openrndr.internal.Driver
import org.openrndr.shape.IntRectangle
import java.io.File
import java.net.MalformedURLException
import java.net.URL

abstract class FontMap {
    abstract val size: Double
    abstract val ascenderLength: Double
    abstract val descenderLength: Double
    abstract val height: Double
    abstract val leading: Double
    abstract val name: String
}

class GlyphMetrics(val advanceWidth: Double, val leftSideBearing: Double, val xBitmapShift: Double, val yBitmapShift: Double)

data class FontImageMapDescriptor(val fontUrl: String, val size: Double, val contentScale: Double)

private val fontImageMaps: MutableMap<FontImageMapDescriptor, FontImageMap> = mutableMapOf()

data class CharacterPair(val left: Char, val right: Char)

class FontImageMap(val texture: ColorBuffer,
                   val map: Map<Char, IntRectangle>,
                   val glyphMetrics: Map<Char, GlyphMetrics>,
                   override val size: Double,
                   val contentScale: Double,
                   override val ascenderLength: Double,
                   override val descenderLength: Double,
                   override val height: Double,
                   override val leading: Double,
                   override val name: String
) : FontMap() {
    val kerningTable = mutableMapOf<CharacterPair, Double>()

    companion object {
        fun fromUrl(fontUrl: String, size: Double, contentScale: Double = 1.0): FontImageMap =
                fontImageMaps.getOrPut(FontImageMapDescriptor(fontUrl, size, contentScale)) {
                    Driver.instance.fontImageMapManager.fontMapFromUrl(fontUrl, size, contentScale)
                }

        fun fromFile(file: String, size: Double, contentScale: Double = 1.0): FontImageMap =
                fontImageMaps.getOrPut(FontImageMapDescriptor("file:$file", size, contentScale)) {
                    Driver.instance.fontImageMapManager.fontMapFromUrl("file:$file", size, contentScale)
                }
    }

    fun characterWidth(character: Char): Double {
        return map[character]?.width?.toDouble() ?: 0.0
    }

    fun kerning(left: Char, right: Char): Double {
        return kerningTable.getOrDefault(CharacterPair(left, right), 0.0)
    }
}

fun Program.loadFont(fileOrUrl: String, size: Double, contentScale: Double = this.drawer.context.contentScale): FontImageMap {
    return try {
        URL(fileOrUrl)
        FontImageMap.fromUrl(fileOrUrl, size, contentScale)
    } catch (e: MalformedURLException) {
        val file = File(fileOrUrl)
        require(file.exists()) {
            "failed to load font: file '${file.absolutePath}' does not exist."
        }
        require(file.extension.toLowerCase() in setOf("ttf", "otf")) {
            "failed to load font: file '${file.absolutePath}' is not a .ttf or .otf file"
        }
        FontImageMap.fromFile(fileOrUrl, size, contentScale)
    }
}

fun loadFont(fileOrUrl: String, size: Double, contentScale: Double = 1.0): FontImageMap {
    return try {
        URL(fileOrUrl)
        FontImageMap.fromUrl(fileOrUrl, size, contentScale)
    } catch (e: MalformedURLException) {
        val file = File(fileOrUrl)
        require(file.exists()) {
            "failed to load font: file '${file.absolutePath}' does not exist."
        }
        require(file.extension.toLowerCase() in setOf("ttf", "otf")) {
            "failed to load font: file '${file.absolutePath}' is not a .ttf or .otf file"
        }
        FontImageMap.fromFile(fileOrUrl, size, contentScale)
    }
}

abstract class FontVectorMap : FontMap() {
    companion object {
        fun fromUrl(fontUrl: String, size: Double): FontImageMap {
            return Driver.instance.fontVectorMapManager.fontMapFromUrl(fontUrl, size)
        }
    }
}