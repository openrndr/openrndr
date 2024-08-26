package org.openrndr.kartifex


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
