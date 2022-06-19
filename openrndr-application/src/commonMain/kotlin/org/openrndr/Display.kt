package org.openrndr

import org.openrndr.math.IntVector2

/**
 * Any coordinates or positions are provided as _screen coordinates_,
 * which may or may not be equivalent to pixel coordinates, depending on your setup.
 */
abstract class Display {
    /** The name of this display as reported by the current graphics backend. */
    abstract val name: String?

    /** The horizontal position of the top-left corner of this Display. */
    abstract val x: Int?

    /** The vertical position of the top-left corner of this Display. */
    abstract val y: Int?

    abstract val width: Int?
    abstract val height: Int?

    /**
     *  The ratio between the current DPI and the platform's default DPI.
     *  More specifically, this is the horizontal scale factor,
     *  but on most displays that will match the vertical scale factor.
     */
    abstract val contentScale: Double?

    /** The position of the top-left corner of this Display. */
    val position: IntVector2? by lazy {
        val x = x ?: return@lazy null
        val y = y ?: return@lazy null
        IntVector2(x, y)
    }

    val dimensions: IntVector2? by lazy {
        val w = width ?: return@lazy null
        val h = height ?: return@lazy null
        IntVector2(w, h)
    }

    abstract override fun toString(): String
}