package org.openrndr.draw.font

import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.utils.buffer.MPPBuffer
import kotlin.jvm.JvmRecord

/**
 * Glyph representation
 * @since 0.4.3
 */
interface Glyph {

    val index: Int

    val code: Int

    /**
     * Generate a [Shape] for this glyph
     * @param scale the scale at which to generate the shape
     * @since 0.4.3
     */
    fun shape(): Shape

    fun advanceWidth(): Double

    fun leftSideBearing(): Double

    fun topSideBearing(): Double

    /**
     * Compute the glyph bounds in up=+y space
     */
    fun bounds(): Rectangle

    /**
     * Compute the bitmap bounds of the glyph
     * @param scale the size for which the bounds should be found
     * @since 0.4.3
     */
    fun bitmapBounds(subpixel:Boolean = true): IntRectangle

    /**
     * Rasterize the glyph at the given size
     * @param bitmap a MPPBuffer positioned at the top left corner of the glyph
     * @param stride the stride (width) of the bitmap buffer
     * @param subpixel should subpixel rendering be used?
     * @since 0.4.3
     */
    fun rasterize(
                  bitmap: MPPBuffer,
                  stride: Int,
                  subpixel: Boolean)

}

/**
 * Represents the master configuration for a font face, allowing manipulation of axis values
 * within specified ranges for variable font adjustments.
 *
 * This class acts as a wrapper around a mutable map of axes, providing functionality to enforce
 * valid axis ranges and copy the current instance.
 *
 * @property axes A map of axis names to their current values. This map is mutable and represents
 *                the active state of the font's variable axis configurations.
 * @property ranges A map of axis names to their valid floating-point ranges.
 */

@JvmRecord
data class FaceMaster(val axes: MutableMap<String, Double>, val ranges: Map<String, ClosedFloatingPointRange<Double>>) : MutableMap<String, Double> by axes {
    override fun hashCode(): Int {
        return axes.hashCode()
    }

    override fun put(key: String, value: Double): Double? {
        require(key in axes.keys) {
            "axis $key is not defined for this font"
        }

        return axes.put(key, value.coerceIn(ranges[key]!!))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FaceMaster

        if (axes != other.axes) return false
        return true
    }

    /**
     * Creates and returns a copy of the current `FaceMaster` instance.
     *
     * The copied instance has a new mutable map for the axes that is initialized
     * with the current axes' key-value pairs, and retains the same ranges map
     * as the original instance.
     *
     * @return A new `FaceMaster` instance with the same data as the original.
     */
    fun copy(): FaceMaster {
        return FaceMaster(axes.toMutableMap(), ranges)
    }

}

/**
 * A face (font) representation
 */
interface Face: AutoCloseable {

    val sizeInPoints: Double
    val contentScale: Double


    /**
     * Retrieves a sequence of all code points supported by the font face.
     *
     * @return a sequence of integers representing all supported Unicode code points.
     */
    fun allCodePoints(): Sequence<Int>

    fun ascentMetrics() : Int

    fun descentMetrics(): Int

    fun lineGapMetrics(): Int

    fun unitsPerEm() : Int


    val emWidth: Double

    val xHeight: Double

    val capHeight: Double

    val height: Double

    val ascent: Double

    val descent: Double

    val lineGap: Double

    /**
     * Calculates the horizontal kerning adjustment between two characters.
     *
     * The kerning adjustment is the additional or reduced horizontal spacing
     * that should be applied when rendering the two characters next to each other
     * to improve the visual balance and appearance of the text.
     *
     * @param left the first character in the pair.
     * @param right the second character in the pair.
     * @return the horizontal kerning adjustment in font units. A positive value increases spacing,
     *         while a negative value decreases spacing.
     */
    fun kernAdvance(left: Char, right: Char): Double

    /**
     * Return the glyph for a given character
     */
    fun glyphForCharacter(character: Char): Glyph


    /**
     * Retrieves the glyph associated with the specified Unicode code point.
     *
     * @param codePoint the Unicode code point for which the glyph is to be retrieved.
     * @return the corresponding [Glyph] object for the given code point.
     */
    fun glyphForCodePoint(codePoint: Int): Glyph


    /**
     * Retrieves the glyph corresponding to the specified glyph index.
     * 
     * Note: Glyph indices are face-specific and may differ between fonts, even for the same character.
     * They represent the internal indexing scheme used by the particular font face and should not be
     * assumed to be portable across different font files.
     * 
     * @param glyphIndex the index of the glyph to retrieve.
     * @return the [Glyph] object associated with the given glyph index.
     */
    fun glyphForIndex(glyphIndex: Int): Glyph

    val bounds: Rectangle

    /**
     * Indicates whether the font face is a variable font.
     *
     * A variable font allows for interpolation between multiple font styles or weights,
     * providing extended flexibility in typography. When set to `true`, the font face
     * is capable of such dynamic variations. Otherwise, it represents a static font.
     */
    val isVariable: Boolean

    /**
     * Represents the master configuration of a font face.
     *
     * This property encapsulates customizable variations of the font face,
     * defined by its axes and ranges, which determine the adjustable properties of the font.
     * It provides access to these variations in order to manipulate or query specific font characteristics.
     *
     * Note: This property returns a mutable copy of this Face's master configuration. Changing the values
     * in the returned master does not affect this Face instance. To create a new Face with modified master
     * settings, use the returned master in combination with [withMaster].
     */
    val master: FaceMaster


    /**
     * Creates a new instance of the font face by applying the provided master parameters.
     *
     * The `master` parameter specifies a set of axis values that override the current
     * variation settings of the font face, allowing customization based on the specified
     * axis values.
     *
     * @param master an instance of [FaceMaster] containing axis values and their ranges
     *               to adjust the font variation settings.
     * @return a new [Face] instance with the adjustments from the provided [FaceMaster].
     */
    fun withMaster(master: FaceMaster): Face
}

/**
 * Load a face
 * @param fileOrUrl a file or url to load the face from
 * @since 0.4.3
 */
fun loadFace(fileOrUrl: String, sizeInPoints: Double, contentScale: Double): Face {
    return FontDriver.instance.loadFace(fileOrUrl, sizeInPoints, contentScale)
}

