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

    val bounds: Rectangle

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