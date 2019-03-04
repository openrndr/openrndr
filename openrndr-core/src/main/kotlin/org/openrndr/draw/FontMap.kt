package org.openrndr.draw

import org.openrndr.internal.Driver
import org.openrndr.shape.IntRectangle

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
    }

    fun characterWidth(character: Char): Double {
        return map[character]?.width?.toDouble() ?: 0.0
    }

    fun kerning(left: Char, right: Char): Double {
        return kerningTable.getOrDefault(CharacterPair(left, right), 0.0)
    }
}

abstract class FontVectorMap : FontMap() {
    companion object {
        fun fromUrl(fontUrl: String, size: Double): FontImageMap {
            return Driver.instance.fontVectorMapManager.fontMapFromUrl(fontUrl, size)
        }
    }
}