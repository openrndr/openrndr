package org.openrndr.kartifex


class Box3 internal constructor(ax: Double, ay: Double, az: Double, bx: Double, by: Double, bz: Double) :
    Box<Vec3, Box3>() {
    var lx = 0.0
    var ly = 0.0
    var lz = 0.0
    var ux = 0.0
    var uy = 0.0
    var uz = 0.0

    constructor(a: Vec3, b: Vec3) : this(a.x, a.y, a.z, b.x, b.y, b.z) {}

    fun box2(): Box2 {
        return Box2(lx, ly, ux, uz)
    }

    protected override fun construct(a: Vec3, b: Vec3): Box3 {
        return Box3(a, b)
    }

    protected override fun empty(): Box3 {
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
