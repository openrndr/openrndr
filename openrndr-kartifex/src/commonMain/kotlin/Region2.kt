package org.openrndr.kartifex

import org.openrndr.kartifex.utils.regions.difference
import org.openrndr.kartifex.utils.regions.intersection
import org.openrndr.kartifex.utils.regions.union

/**
 * Represents a 2D region composed of multiple rings, where each ring defines a contour of the region.
 * Multiple rings can be used to create complex shapes, including those with holes.
 *
 * @constructor Creates a `Region2` object with the specified rings.
 * @property rings An array of `Ring2` objects that represent the contours of the region.
 * @property bounds The bounding box, represented by `Box2`, that encompasses all the rings in the region.
 */
class Region2(val rings: Array<Ring2>) {
    val bounds: Box2

    constructor(rings: Iterable<Ring2>) : this(
        rings.toList().toTypedArray()
    )

    fun test(p: Vec2): Ring2.Result {
        for (r in rings) {
            val result: Ring2.Result = r.test(p)
            if (result.inside) {
                return if (result.curve == null && r.isClockwise) Ring2.Result.OUTSIDE else result
            }
        }
        return Ring2.Result.OUTSIDE
    }

    operator fun contains(p: Vec2): Boolean {
        return test(p).inside
    }

    /// transforms and set operations
    fun transform(m: Matrix3): Region2 {
        return Region2(
            rings
                .map { r: Ring2 ->
                    r.transform(
                        m
                    )
                }.toTypedArray()
        )
    }

    fun intersection(region: Region2): Region2 {
        return intersection(this, region)
    }

    fun union(region: Region2): Region2 {
        return union(this, region)
    }

    fun difference(region: Region2): Region2 {
        return difference(this, region)
    }

    companion object {
        fun of(vararg rings: Ring2): Region2 {
            return Region2(rings.toList())
        }
    }

    init {
        rings.sortBy { it.area }

        bounds = rings.map { r: Ring2 -> r.bounds }
            .fold(Box2.EMPTY) { obj, b -> obj.union(b) }
    }
}
