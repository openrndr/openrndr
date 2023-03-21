package org.openrndr.draw.font

import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape

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

    fun bounds(size: Double): Rectangle

}

/**
 * A face (font) representation
 */
interface Face {

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