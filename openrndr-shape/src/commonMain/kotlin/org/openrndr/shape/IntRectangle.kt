package org.openrndr.shape

import kotlinx.serialization.Serializable
import org.openrndr.math.IntVector2
import kotlin.jvm.JvmRecord

/**
 * Creates a new [IntRectangle].
 *
 * Uses [IntVector2]s which require whole numbers (integers) for position and dimensions.
 *
 * Also see [Rectangle].
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
@Serializable
@JvmRecord
data class IntRectangle(val corner: IntVector2, val width: Int, val height: Int) {
    val x get() = corner.x

    val y get() = corner.y

    val center get() = corner + IntVector2(width / 2, height / 2)

    /** The unitless area covered by this [IntRectangle]. */
    val area get() = width * height

    /** The dimensions of the [IntRectangle]. */
    val dimensions get() = IntVector2(width, height)

    /** Casts [IntRectangle] to [Rectangle]. */
    val rectangle get() = Rectangle(corner.vector2, width.toDouble(), height.toDouble())
}

fun IntRectangle(x: Int, y: Int, width: Int, height: Int) = IntRectangle(IntVector2(x, y), width, height)
