package org.openrndr.kartifex

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

    fun box3(): Box3 {
        return Box3(lx, ly, lz, ux, uy, uz)
    }

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
