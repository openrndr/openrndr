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
    fun shape(scale: Double): Shape

    fun advanceWidth(scale: Double): Double

    fun leftSideBearing(scale: Double): Double

    fun topSideBearing(scale: Double): Double

    /**
     * Compute the glyph bounds in up=+y space
     */
    fun bounds(scale: Double): Rectangle

    /**
     * Compute the bitmap bounds of the glyph
     * @param scale the size for which the bounds should be found
     * @since 0.4.3
     */
    fun bitmapBounds(scale: Double, subpixel:Boolean = true): IntRectangle

    /**
     * Rasterize the glyph at the given size
     * @param bitmap a MPPBuffer positioned at the top left corner of the glyph
     * @param stride the stride (width) of the bitmap buffer
     * @param subpixel should subpixel rendering be used?
     * @since 0.4.3
     */
    fun rasterize(scale: Double,
                  bitmap: MPPBuffer,
                  stride: Int,
                  subpixel: Boolean)

}

/**
 * A face (font) representation
 */
interface Face: AutoCloseable {


    fun ascentMetrics() : Int

    fun descentMetrics(): Int

    fun lineGapMetrics(): Int

    fun unitsPerEm() : Int

    fun ascent(scale: Double): Double = ascentMetrics() * scale

    fun descent(scale: Double): Double = descentMetrics() * scale

    fun lineGap(scale: Double): Double = lineGapMetrics() * scale

    fun lineSpace(scale: Double): Double = ascent(scale) - descent(scale) + lineGap(scale)

    fun kernAdvance(scale: Double, left: Char, right: Char): Double

    /**
     * Return the glyph for a given character
     */
    fun glyphForCharacter(character: Char): Glyph
    fun bounds(scale: Double): Rectangle
}

/**
 * Load a face
 * @param fileOrUrl a file or url to load the face from
 * @since 0.4.3
 */
fun loadFace(fileOrUrl: String): Face {
    return FontDriver.instance.loadFace(fileOrUrl)
}

/**
 * Scale font based on ascender + descender height
 */
fun fontHeightScaler(font: Face) : Double = 1.0 / (font.ascentMetrics() - font.descentMetrics())

/**
 * Scale font based on units per em
 */
fun fontEmScaler(font: Face) = 1.0 / font.unitsPerEm().also {
    println("units per em: $it")
}