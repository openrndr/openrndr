package org.openrndr.draw

import org.openrndr.internal.Driver
import org.openrndr.shape.IntRectangle
import kotlin.jvm.JvmRecord

private val standard = charArrayOf(
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'ë', 'ä', 'ö', 'ü', 'ï', 'ÿ', 'Ë', 'Ä', 'Ö', 'Ü', 'Ï', 'Ÿ', 'ñ', 'Ñ', 'ç', 'Ç', 'ø', 'Ø', 'é', 'á', 'ó', 'í', 'ú', 'É', 'Á', 'Ó',
        'Í', 'Ú', 'è', 'à', 'ò', 'ì', 'ù', 'È', 'À', 'Ò', 'Ì', 'Ù', 'â', 'ê', 'î', 'û', 'ô', 'Â', 'Ê', 'Î', 'Û', 'Ô', 'œ', 'Œ', 'æ', 'Æ',
        'Ą', 'Ć', 'Ę', 'Ł', 'Ń', 'Ó', 'Ś', 'Ż', 'Ź', 'ą', 'ć', 'ę', 'ł', 'ń', 'ó', 'ś', 'ż', 'ź', 'ß', 'ẞ',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '!', '?', '¿', '¡', '…', '.', ',', ' ', ':', ';', '&', '#', '№', '“', '”', '‘', '’', '`',
        '¤', '€', '$', '£', '‒', '-', '—', '–', '_', '·', '•', '°', '@', '^', '*', '«', '»', '/',
        '\\', '"', '\'', '+', '=', '÷', '~', '%', '(', ')', '[', ']', '{', '}', '<', '>', '|', '✕', '²', '³')


private val cyrillic = charArrayOf(
        'А', 'а', 'Б', 'б', 'В', 'в', 'Г', 'г', 'Д', 'д', 'Е', 'е', 'Ё', 'ё', 'Ж', 'ж', 'З', 'з', 'И', 'и', 'Й', 'й',
        'К', 'к', 'Л', 'л', 'М', 'м', 'Н', 'н', 'О', 'о', 'П', 'п', 'Р', 'р', 'С', 'с', 'Т', 'т', 'У', 'у', 'Ф', 'ф',
        'Х', 'х', 'Ц', 'ц', 'Ч', 'ч', 'Ш', 'ш', 'Щ', 'щ', 'Ъ', 'ъ', 'Ы', 'ы', 'Ь', 'ь', 'Э', 'э', 'Ю', 'ю', 'Я', 'я',
        'І', 'і', 'Ў', 'ў', 'Ґ', 'ґ', 'Ї', 'ї',	'Й', 'й'
)

val defaultFontmapCharacterSet by lazy { (standard + cyrillic).toSet() }


/**
 * Represents an abstract mapping of font characteristics and metrics.
 *
 * This class provides essential information about a font, such as its size,
 * ascender length, descender length, total height, leading, and name.
 * It serves as a base to define font-related properties for other implementations.
 *
 * @property size The size of the font.
 * @property ascenderLength The length of the ascender part of the font, which is the height above the baseline.
 * @property descenderLength The length of the descender part of the font, which is the height below the baseline.
 * @property height The total height of the font, combining ascender, descender, and other elements.
 * @property leading The vertical spacing between lines of text.
 * @property name The name of the font.
 */
abstract class FontMap {
    abstract val size: Double
    abstract val ascenderLength: Double
    abstract val descenderLength: Double
    abstract val height: Double
    abstract val leading: Double
    abstract val name: String
}

/**
 * The default font map used for rendering text in the system. This variable represents
 * an optional instance of [FontImageMap], which contains the pre-rendered texture and
 * associated metadata for specific font settings like size, character set, and metrics.
 *
 * The actual value of this variable is platform-specific and may vary depending
 * on the implementation and the environment in which the code runs.
 */
expect val defaultFontMap: FontImageMap?

/**
 * Represents the metrics of a glyph, which define its visual and positional characteristics
 * in a font rendering context.
 *
 * @property advanceWidth The horizontal distance the pen moves after rendering this glyph.
 * @property leftSideBearing The horizontal space between the glyph’s left edge and its origin.
 * @property xBitmapShift The horizontal offset applied to the glyph's bitmap representation.
 * @property yBitmapShift The vertical offset applied to the glyph's bitmap representation.
 */
@JvmRecord
data class GlyphMetrics(val advanceWidth: Double, val leftSideBearing: Double, val xBitmapShift: Double, val yBitmapShift: Double)

/**
 * A data class representing the descriptor for a font image map.
 *
 * This class provides the necessary information to define the mapping of characters
 * to their graphical representation based on a font.
 *
 * @property fontUrl The URL of the font to be used for creating the image map.
 * @property size The size of the font to be rendered.
 * @property alphabet The set of characters to be included in the image map.
 * @property contentScale The scaling factor for rendering the font.
 */
@JvmRecord
data class FontImageMapDescriptor(val fontUrl: String, val size: Double, val alphabet:Set<Char>, val contentScale: Double)

private val fontImageMaps: MutableMap<FontImageMapDescriptor, FontImageMap> = mutableMapOf()

/**
 * A data class representing a pair of characters.
 *
 * This class is implemented as a record in the JVM to optimize memory usage
 * and ensure immutability. Each instance of this class stores two characters,
 * referred to as "left" and "right".
 *
 * @constructor Creates an immutable pair of characters.
 * @property left The first character in the pair.
 * @property right The second character in the pair.
 */
@JvmRecord
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
    /**
     * A mutable map that stores kerning values for pairs of characters.
     *
     * The keys in this map represent pairs of characters as instances of [CharacterPair],
     * while the values represent the kerning adjustment between the two characters
     * in the pair, measured as a `Double`. Positive kerning values increase spacing
     * between characters, while negative values reduce spacing.
     */
    val kerningTable = mutableMapOf<CharacterPair, Double>()

    companion object {
        /**
         * Creates a `FontImageMap` instance from a font file URL with the specified parameters.
         *
         * @param fontUrl The URL of the font file to be used.
         * @param size The size of the font to be rendered.
         * @param characterSet The set of characters to include in the font image map. Defaults to `defaultFontmapCharacterSet`.
         * @param contentScale The scaling factor to apply to the font rendering. Defaults to `1.0`.
         * @return A `FontImageMap` generated based on the given parameters.
         */
        fun fromUrl(fontUrl: String, size: Double, characterSet: Set<Char> = defaultFontmapCharacterSet, contentScale: Double = 1.0): FontImageMap =
                fontImageMaps.getOrPut(FontImageMapDescriptor(fontUrl, size,  characterSet, contentScale)) {
                    Driver.instance.fontImageMapManager.fontMapFromUrl(fontUrl, size, characterSet, contentScale)
                }

        /**
         * Creates or retrieves a cached `FontImageMap` instance from a file source.
         * The `FontImageMap` is generated or identified based on the provided parameters
         * such as file path, font size, character set, and content scaling.
         *
         * @param file The path to the file containing the font resource.
         * @param size The size of the font to be used for the image map.
         * @param characterSet The set of characters to include in the image map. Defaults to `defaultFontmapCharacterSet`.
         * @param contentScale The scaling factor applied during rendering. Defaults to `1.0`.
         * @return A `FontImageMap` representing the graphical mapping of characters based on the specified configurations.
         */
        fun fromFile(file: String, size: Double, characterSet: Set<Char> = defaultFontmapCharacterSet, contentScale: Double = 1.0): FontImageMap =
                fontImageMaps.getOrPut(FontImageMapDescriptor("file:$file", size, characterSet, contentScale)) {
                    Driver.instance.fontImageMapManager.fontMapFromUrl("file:$file", size, characterSet, contentScale)
                }
    }

    /**
     * Calculates the width of a given character based on its metrics in the font image map.
     *
     * @param character The character whose width is being calculated.
     * @return The width of the given character as a Double. Returns 0.0 if the character is not found in the map.
     */
    fun characterWidth(character: Char): Double = map[character]?.width?.toDouble() ?: 0.0

    /**
     * Retrieves the kerning value between two characters.
     *
     * Kerning defines the spacing adjustment between two specific characters
     * to improve the overall appearance and readability of the text. If no
     * kerning value is defined for the given pair, it defaults to 0.0.
     *
     * @param left The first character in the character pair.
     * @param right The second character in the character pair.
     * @return The kerning value for the specified character pair as a Double. Defaults to 0.0 if the pair is not found.
     */
    fun kerning(left: Char, right: Char): Double = kerningTable.getOrElse(CharacterPair(left, right)) { 0.0 }
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

