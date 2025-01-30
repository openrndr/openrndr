package org.openrndr.kartifex


/**
 * Represents a three-dimensional axis-aligned box defined by two points in 3D space.
 *
 * @constructor Initializes the box with the specified lower (`a`) and upper (`b`) points.
 * Internally determines the correct lower and upper bounds along each axis.
 * This ensures that, regardless of input order, the box is represented correctly.
 *
 * @property lx The lower bound of the box along the x-axis.
 * @property ly The lower bound of the box along the y-axis.
 * @property lz The lower bound of the box along the z-axis.
 * @property ux The upper bound of the box along the x-axis.
 * @property uy The upper bound of the box along the y-axis.
 * @property uz The upper bound of the box along the z-axis.
 *
 * @constructor A secondary constructor that initializes the box using two `Vec3` objects
 * representing the lower and upper bounds.
 */
class Box3 internal constructor(ax: Double, ay: Double, az: Double, bx: Double, by: Double, bz: Double) :
    Box<Vec3, Box3>() {
    val lx: Double
    val ly: Double
    val lz: Double
    val ux: Double
    val uy: Double
    val uz: Double

    constructor(a: Vec3, b: Vec3) : this(a.x, a.y, a.z, b.x, b.y, b.z)

    fun box2(): Box2 {
        return Box2(lx, ly, ux, uz)
    }

    override fun construct(a: Vec3, b: Vec3): Box3 {
        return Box3(a, b)
    }

    override fun empty(): Box3 {
        return EMPTY
    }

    override fun lower(): Vec3 {
        return Vec3(lx, ly, lz)
    }

    override fun upper(): Vec3 {
        return Vec3(ux, uy, uz)
    }

    override val isEmpty: Boolean
        get() = this === EMPTY

    companion object {
        val EMPTY = Box3(
            Vec3(Double.NaN, Double.NaN, Double.NaN),
            Vec3(Double.NaN, Double.NaN, Double.NaN)
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
    }
}
