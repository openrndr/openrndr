package org.openrndr.kartifex

/**
 * Represents a four-dimensional bounding box defined by two Vec4 points.
 * This class extends the abstract class `Box`, which provides common functionality for multi-dimensional bounding boxes.
 * The box is defined by its lower and upper bounds in 4D space.
 *
 * This class provides additional capabilities for extracting lower-dimensional bounding boxes (e.g., 3D and 2D boxes),
 * creating empty boxes, and determining spatial relationships such as containment and intersection.
 *
 * @constructor Creates a `Box4` defined by the given lower and upper bounds in all four dimensions.
 * @param ax The x-coordinate of the first point.
 * @param ay The y-coordinate of the first point.
 * @param az The z-coordinate of the first point.
 * @param aw The w-coordinate of the first point.
 * @param bx The x-coordinate of the second point.
 * @param by The y-coordinate of the second point.
 * @param bz The z-coordinate of the second point.
 * @param bw The w-coordinate of the second point.
 */
class Box4 private constructor(
    ax: Double,
    ay: Double,
    az: Double,
    aw: Double,
    bx: Double,
    by: Double,
    bz: Double,
    bw: Double
) :
    Box<Vec4, Box4>() {
    private val lx: Double
    private val ly: Double
    private val lz: Double
    private val lw: Double
    private val ux: Double
    private val uy: Double
    private val uz: Double
    private val uw: Double

    constructor(a: Vec4, b: Vec4) : this(a.x, a.y, a.z, a.w, b.x, b.y, b.z, b.w)

    fun box2(): Box2 {
        return Box2(lx, ly, ux, uy)
    }

    override fun construct(a: Vec4, b: Vec4): Box4 {
        return Box4(a, b)
    }

    override fun empty(): Box4 {
        return EMPTY
    }

    override fun lower(): Vec4 {
        return Vec4(lx, ly, lz, lw)
    }

    override fun upper(): Vec4 {
        return Vec4(ux, uy, uz, uw)
    }

    override val isEmpty: Boolean
        get() = this === EMPTY

    companion object {
        val EMPTY = Box4(
            Vec4(Double.NaN, Double.NaN, Double.NaN, Double.NaN), Vec4(
                Double.NaN, Double.NaN, Double.NaN, Double.NaN
            )
        )
    }

    init {
        if (ax < bx) {
            lx = ax
            ux = bx
        } else {
            ux = ax
            lx = bx
        }
        if (ay < by) {
            ly = ay
            uy = by
        } else {
            uy = ay
            ly = by
        }
        if (az < bz) {
            lz = az
            uz = bz
        } else {
            uz = az
            lz = bz
        }
        if (aw < bw) {
            lw = aw
            uw = bw
        } else {
            uw = aw
            lw = bw
        }
    }
}
