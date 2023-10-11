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
     * @param size the size at which to generate the shape
     * @since 0.4.3
     */
    fun shape(size: Double): Shape

    fun advanceWidth(size: Double): Double

    fun leftSideBearing(size: Double): Double

    fun topSideBearing(size: Double): Double

    /**
     * Compute the glyph bounds in up=+y space
     */
    fun bounds(size: Double): Rectangle

    /**
     * Compute the bitmap bounds of the glyph
     * @param size the size for which the bounds should be found
     * @since 0.4.3
     */
    fun bitmapBounds(size: Double, subpixel:Boolean = true): IntRectangle

    /**
     * Rasterize the glyph at the given size
     * @param bitmap a MPPBuffer positioned at the top left corner of the glyph
     * @param stride the stride (width) of the bitmap buffer
     * @param subpixel should subpixel rendering be used?
     * @since 0.4.3
     */
    fun rasterize(size: Double,
                  bitmap: MPPBuffer,
                  stride: Int,
                  subpixel: Boolean)

}

/**
 * A face (font) representation
 */
@OptIn(ExperimentalStdlibApi::class)
interface Face: AutoCloseable {

    fun ascent(size: Double): Double

    fun descent(size: Double): Double

    fun lineGap(size: Double): Double

    fun lineSpace(size: Double): Double = ascent(size) - descent(size) + lineGap(size)

    fun kernAdvance(size: Double, left: Char, right: Char): Double

    /**
     * Return the glyph for a given character
     */
    fun glyphForCharacter(character: Char): Glyph
    fun bounds(size: Double): Rectangle
}

/**
 * Load a face
 * @param fileOrUrl a file or url to load the face from
 * @since 0.4.3
 */
fun loadFace(fileOrUrl: String): Face {
    return FontDriver.instance.loadFace(fileOrUrl)
}