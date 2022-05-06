package org.openrndr.draw

import org.openrndr.internal.Driver
import org.openrndr.shape.IntRectangle

private val standard = charArrayOf(
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'ë', 'ä', 'ö', 'ü', 'ï', 'ÿ', 'Ë', 'Ä', 'Ö', 'Ü', 'Ï', 'Ÿ', 'ñ', 'Ñ', 'ç', 'Ç', 'ø', 'Ø', 'é', 'á', 'ó', 'í', 'ú', 'É', 'Á', 'Ó',
        'Í', 'Ú', 'è', 'à', 'ò', 'ì', 'ù', 'È', 'À', 'Ò', 'Ì', 'Ù', 'â', 'ê', 'î', 'û', 'ô', 'Â', 'Ê', 'Î', 'Û', 'Ô', 'œ', 'Œ', 'æ', 'Æ',
        'Ą', 'Ć', 'Ę', 'Ł', 'Ń', 'Ó', 'Ś', 'Ż', 'Ź', 'ą', 'ć', 'ę', 'ł', 'ń', 'ó', 'ś', 'ż', 'ź',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
        '!', '?', '¿', '¡', '…', '.', ',', ' ', ':', ';', '&', '#', '№', '“', '”', '‘', '’', '`',
        '¤', '€', '$', '£', '‒', '-', '—', '–', '_', '·', '•', '°', '@', '^', '*', '«', '»', '/',
        '\\', '"', '\'', '+', '=', '÷', '~', '%', '(', ')', '[', ']', '{', '}', '<', '>', '|', '✕')


private val cyrillic = charArrayOf(
        'А', 'а', 'Б', 'б', 'В', 'в', 'Г', 'г', 'Д', 'д', 'Е', 'е', 'Ё', 'ё', 'Ж', 'ж', 'З', 'з', 'И', 'и', 'Й', 'й',
        'К', 'к', 'Л', 'л', 'М', 'м', 'Н', 'н', 'О', 'о', 'П', 'п', 'Р', 'р', 'С', 'с', 'Т', 'т', 'У', 'у', 'Ф', 'ф',
        'Х', 'х', 'Ц', 'ц', 'Ч', 'ч', 'Ш', 'ш', 'Щ', 'щ', 'Ъ', 'ъ', 'Ы', 'ы', 'Ь', 'ь', 'Э', 'э', 'Ю', 'ю', 'Я', 'я',
        'І', 'і', 'Ў', 'ў', 'Ґ', 'ґ', 'Ї', 'ї',	'Й', 'й'
)

val defaultFontmapCharacterSet by lazy { (standard + cyrillic).toSet() }

/**
 * Holds properties common to both vector-based and image-based [FontMap]s.
 */
abstract class FontMap {
    abstract val size: Double
    abstract val ascenderLength: Double
    abstract val descenderLength: Double
    abstract val height: Double
    abstract val leading: Double
    abstract val name: String
}

expect val defaultFontMap: FontImageMap?

data class GlyphMetrics(val advanceWidth: Double, val leftSideBearing: Double, val xBitmapShift: Double, val yBitmapShift: Double)

data class FontImageMapDescriptor(val fontUrl: String, val size: Double, val alphabet:Set<Char>, val contentScale: Double)

private val fontImageMaps: MutableMap<FontImageMapDescriptor, FontImageMap> = mutableMapOf()

data class CharacterPair(val left: Char, val right: Char)

/**
 * A type of [FontMap] which keeps characters pre-rendered in a [ColorBuffer]
 * texture at a specific font [size].
 */
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
        fun fromUrl(fontUrl: String, size: Double, characterSet: Set<Char> = defaultFontmapCharacterSet, contentScale: Double = 1.0): FontImageMap =
                fontImageMaps.getOrPut(FontImageMapDescriptor(fontUrl, size,  characterSet, contentScale)) {
                    Driver.instance.fontImageMapManager.fontMapFromUrl(fontUrl, size, characterSet, contentScale)
                }

        fun fromFile(file: String, size: Double, characterSet: Set<Char> = defaultFontmapCharacterSet, contentScale: Double = 1.0): FontImageMap =
                fontImageMaps.getOrPut(FontImageMapDescriptor("file:$file", size, characterSet, contentScale)) {
                    Driver.instance.fontImageMapManager.fontMapFromUrl("file:$file", size, characterSet, contentScale)
                }
    }

    fun characterWidth(character: Char): Double = map[character]?.width?.toDouble() ?: 0.0

    fun kerning(left: Char, right: Char): Double = kerningTable.getOrElse(CharacterPair(left, right), { 0.0 })
}


/**
 * A type of [FontMap] which keeps characters stored as vector data. Good for
 * displaying very large text and for displaying text at different scales, but
 * in general less performant than [FontImageMap] because it is not
 * pre-rendered.
 */
abstract class FontVectorMap : FontMap() {
    companion object {
        fun fromUrl(fontUrl: String, size: Double, characterSet: Set<Char> = defaultFontmapCharacterSet): FontImageMap {
            return Driver.instance.fontVectorMapManager.fontMapFromUrl(fontUrl, size, characterSet)
        }
    }
}

