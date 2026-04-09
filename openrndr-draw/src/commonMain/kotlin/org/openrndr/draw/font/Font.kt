package org.openrndr.draw.font

import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.utils.buffer.MPPBuffer

/**
 * Glyph representation
 * @since 0.4.3
 */
interface Glyph {
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


    val height: Double

    val ascent: Double

    val descent: Double

    val lineGap: Double

    fun kernAdvance(left: Char, right: Char): Double

    /**
     * Return the glyph for a given character
     */
    fun glyphForCharacter(character: Char): Glyph

    fun glyphForCodePoint(codePoint: Int): Glyph

    fun glyphForIndex(glyphIndex: Int, character: Char = Char(0)): Glyph

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
     * A list of strings representing the names of variable font axes supported by this font face.
     *
     * Variable font axes describe adjustable properties of a variable font, such as weight, width,
     * or slant, which can be dynamically modified to produce a range of styles or characteristics.
     * Each axis is identified by a unique name.
     *
     * This property is only populated for variable fonts. For static fonts, this list will be empty.
     */
    val axes: List<String>

    /**
     * Retrieves the value associated with a specific variable font axis.
     *
     * Variable font axes represent adjustable properties of a variable font, such as weight, width, or slant.
     * This method fetches the current value for the specified axis name. The axis name must match one of
     * the entries in the `axes` property of the `Face` interface.
     *
     * @param axis the name of the variable font axis for which the value is to be retrieved.
     * @return the current value of the specified axis as a Double.
     *         Returns 0.0 if the axis is not supported or if the face is not a variable font.
     */
    fun getAxisValue(axis: String): Double

    /**
     * Sets the value for a specified variable font axis.
     *
     * Variable font axes represent adjustable properties of a variable font, such as weight, width, or slant.
     * This method allows modifying the value of a specific axis, and the axis name must correspond
     * to one of the entries in the `axes` property of the `Face` class.
     *
     * @param axis the name of the variable font axis to modify.
     * @param value the new value to set for the specified axis.
     */
    fun setAxisValue(axis: String, value: Double)

    /**
     * Retrieves the valid range of values for a specified variable font axis.
     *
     * Variable font axes define adjustable properties of fonts, such as weight, width, or slant.
     * This method returns the minimum and maximum allowable values for the given axis.
     *
     * @param axis the name of the variable font axis for which the range is to be retrieved.
     * @return a closed floating-point range representing the minimum and maximum allowable values for the specified axis.
     *         Returns an empty range (e.g., 0.0..0.0) if the axis is not supported or if the font is not a variable font.
     */
    fun axisRange(axis: String): ClosedFloatingPointRange<Double>

}

/**
 * Load a face
 * @param fileOrUrl a file or url to load the face from
 * @since 0.4.3
 */
fun loadFace(fileOrUrl: String, sizeInPoints: Double, contentScale: Double): Face {
    return FontDriver.instance.loadFace(fileOrUrl, sizeInPoints, contentScale)
}

/**
 * Scale font based on ascender + descender height
 * @since 0.4.5
 */
fun fontHeightScaler(font: Face) : Double = 1.0 / (font.ascentMetrics() - font.descentMetrics())

/**
 * Scale font based on units per em
 * @since 0.4.5
 */
fun fontEmScaler(font: Face) = 1.0 / font.unitsPerEm()