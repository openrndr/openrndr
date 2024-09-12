package org.openrndr.shape

import org.openrndr.math.IntVector3
import kotlin.math.max
import kotlin.math.min

class IntBox(val corner: IntVector3, val width: Int, val height: Int, val depth: Int) {

    val xRange
        get() = min(corner.x, corner.x + width)..< max(corner.x, corner.x + width)

    val yRange
        get() = min(corner.y, corner.y + height)..< max(corner.y, corner.y + height)

    val zRange
        get() = min(corner.z, corner.z + depth)..< max(corner.z, corner.z + depth)
}