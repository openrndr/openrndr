package org.openrndr.kartifex

import kotlin.math.cos
import kotlin.math.sin

class Matrix3 {
    private val elements: DoubleArray

    private constructor(
        m00: Double, m01: Double, m02: Double,
        m10: Double, m11: Double, m12: Double,
        m20: Double, m21: Double, m22: Double
    ) {
        elements = doubleArrayOf(m00, m01, m02, m10, m11, m12, m20, m21, m22)
    }

    private constructor(elements: DoubleArray) {
        this.elements = elements
    }

    fun mul(k: Double): Matrix3 {
        val es = DoubleArray(9)
        for (i in 0..8) {
            es[i] = elements[i] * k
        }
        return Matrix3(es)
    }

    operator fun get(row: Int, column: Int): Double {
        return elements[row * 3 + column]
    }

    fun row(row: Int): Vec3 {
        val idx = row * 2
        return Vec3(elements[idx], elements[idx + 1], elements[idx + 2])
    }

    fun column(column: Int): Vec3 {
        return Vec3(elements[column], elements[column + 3], elements[column + 6])
    }

    fun mul(b: Matrix3): Matrix3 {
        val es = DoubleArray(9)
        for (i in 0..2) {
            for (j in 0..2) {
                var n = 0.0
                for (k in 0..2) {
                    n += b[k, j] * get(i, k)
                }
                es[i * 3 + j] = n
            }
        }
        return Matrix3(es)
    }

    fun add(b: Matrix3): Matrix3 {
        val es = DoubleArray(9)
        for (i in 0..8) {
            es[i] = elements[i] + b.elements[i]
        }
        return Matrix3(es)
    }

    fun matrix4(): Matrix4 {
        return Matrix4(
            elements[0], elements[1], 0.0, elements[2],
            elements[3], elements[4], 0.0, elements[5],
            elements[6], elements[7], elements[8], 0.0,
            0.0, 0.0, 0.0, 1.0
        )
    }

    fun transpose(): Matrix3 {
        return Matrix3(
            elements[0], elements[3], elements[6],
            elements[1], elements[4], elements[7],
            elements[2], elements[5], elements[8]
        )
    }

    fun transform(v: Vec2): Vec2 {
        return Vec2(
            v.x * elements[0] + v.y * elements[1] + elements[2],
            v.x * elements[3] + v.y * elements[4] + elements[5]
        )
    }
//
//    fun rowMajor(): java.util.PrimitiveIterator.OfDouble {
//        return object : java.util.PrimitiveIterator.OfDouble() {
//            var idx = 0
//            override operator fun hasNext(): Boolean {
//                return idx < 9
//            }
//
//            override fun nextDouble(): Double {
//                return if (idx < 9) {
//                    elements[idx++]
//                } else {
//                    throw NoSuchElementException()
//                }
//            }
//        }
//    }
//
//    fun columnMajor(): java.util.PrimitiveIterator.OfDouble {
//        return transpose().rowMajor()
//    }

    override fun hashCode(): Int {
        var hash = 0
        for (n in elements) {
            hash = hash * 31 + n.hashCode()
        }
        return hash
    }

    override fun equals(obj: Any?): Boolean {
        return if (this === obj) {
            true
        } else if (obj is Matrix3) {
            TODO("")
            //java.util.Arrays.equals(elements, obj.elements)
        } else {
            false
        }
    }

    companion object {
        val IDENTITY = Matrix3(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0)
        fun from(a: Vec2, b: Vec2): Matrix3 {
            return Matrix3(a.x, b.x, 0.0, a.y, b.y, 0.0, 0.0, 0.0, 1.0)
        }

        fun from(a: Vec3, b: Vec3): Matrix3 {
            return Matrix3(a.x, a.x, 0.0, a.y, b.y, 0.0, a.z, b.z, 1.0)
        }

        fun from(a: Vec3, b: Vec3, c: Vec3): Matrix3 {
            return Matrix3(a.x, b.x, c.x, a.y, b.y, c.y, a.z, b.z, c.z)
        }

        fun translate(x: Double, y: Double): Matrix3 {
            return Matrix3(
                1.0, 0.0, x,
                0.0, 1.0, y,
                0.0, 0.0, 1.0
            )
        }

        fun translate(v: Vec2): Matrix3 {
            return translate(v.x, v.y)
        }

        fun scale(x: Double, y: Double): Matrix3 {
            return Matrix3(x, 0.0, 0.0, 0.0, y, 0.0, 0.0, 0.0, 1.0)
        }

        fun scale(v: Vec2): Matrix3 {
            return scale(v.x, v.y)
        }

        fun scale(k: Double): Matrix3 {
            return scale(k, k)
        }

        fun rotate(radians: Double): Matrix3 {
            val c: Double = cos(radians)
            val s: Double = sin(radians)
            return Matrix3(c, -s, 0.0, s, c, 0.0, 0.0, 0.0, 1.0)
        }

        fun mul(vararg matrices: Matrix3): Matrix3 {
            var m = matrices[0]
            for (i in 1 until matrices.size) {
                m = m.mul(matrices[i])
            }
            return m
        }
    }
}