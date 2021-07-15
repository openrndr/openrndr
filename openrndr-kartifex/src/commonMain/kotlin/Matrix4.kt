package org.openrndr.kartifex

import kotlin.math.cos
import kotlin.math.sin


/**
 *
 */
class Matrix4 {
    private val elements: DoubleArray

    internal constructor(
        m00: Double, m01: Double, m02: Double, m03: Double,
        m10: Double, m11: Double, m12: Double, m13: Double,
        m20: Double, m21: Double, m22: Double, m23: Double,
        m30: Double, m31: Double, m32: Double, m33: Double
    ) {
        elements = doubleArrayOf(
            m00, m01, m02, m03,
            m10, m11, m12, m13,
            m20, m21, m22, m23,
            m30, m31, m32, m33
        )
    }

    private constructor(elements: DoubleArray) {
        this.elements = elements
    }

    fun mul(k: Double): Matrix4 {
        val es = DoubleArray(16)
        for (i in 0..15) {
            es[i] = elements[i] * k
        }
        return Matrix4(es)
    }

    operator fun get(row: Int, column: Int): Double {
        return elements[(row shl 2) + column]
    }

    fun row(row: Int): Vec4 {
        val idx = row shl 2
        return Vec4(elements[idx], elements[idx + 1], elements[idx + 2], elements[idx + 3])
    }

    fun column(column: Int): Vec4 {
        return Vec4(
            elements[column], elements[column + 4],
            elements[column + 8], elements[column + 12]
        )
    }

    fun mul(b: Matrix4): Matrix4 {
        val es = DoubleArray(16)
        for (i in 0..3) {
            for (j in 0..3) {
                var n = 0.0
                for (k in 0..3) {
                    n += b[k, j] * get(i, k)
                }
                es[(i shl 2) + j] = n
            }
        }
        return Matrix4(es)
    }

    fun add(b: Matrix4): Matrix4 {
        val es = DoubleArray(16)
        for (i in 0..15) {
            es[i] = elements[i] + b.elements[i]
        }
        return Matrix4(es)
    }

    fun transpose(): Matrix4 {
        return Matrix4(
            elements[0], elements[4], elements[8], elements[12],
            elements[1], elements[5], elements[9], elements[13],
            elements[2], elements[6], elements[10], elements[14],
            elements[3], elements[7], elements[11], elements[15]
        )
    }

    fun transform(v: Vec3): Vec3 {
        return Vec3(
            v.x * elements[0] + v.y * elements[1] + v.z * elements[2] + elements[3],
            v.x * elements[4] + v.y * elements[5] + v.z * elements[6] + elements[7],
            v.x * elements[8] + v.y * elements[9] + v.z * elements[10] + elements[11]
        )
    }

    /*
    fun rowMajor(): java.util.PrimitiveIterator.OfDouble {
        return object : java.util.PrimitiveIterator.OfDouble() {
            var idx = 0
            override operator fun hasNext(): Boolean {
                return idx < 16
            }

            override fun nextDouble(): Double {
                return if (idx < 16) {
                    elements[idx++]
                } else {
                    throw NoSuchElementException()
                }
            }
        }
    }

    fun columnMajor(): java.util.PrimitiveIterator.OfDouble {
        return transpose().rowMajor()
    }
*/
    /*
    override fun hashCode(): Int {
        var hash = 0
        for (n in elements) {
            hash = hash * 31 + Hashes.hash(n)
        }
        return hash
    }

    override fun equals(obj: Any?): Boolean {
        return if (this === obj) {
            true
        } else if (obj is Matrix4) {
            java.util.Arrays.equals(elements, obj.elements)
        } else {
            false
        }
    }
    */


    /*
    override fun toString(): String {
        val s = StringBuffer()
        rowMajor().forEachRemaining(DoubleConsumer { n: Double -> s.append(n).append(", ") })
        return s.toString()
    }
*/
    companion object {
        val IDENTITY = Matrix4(
            1.0, 0.0, 0.0, 0.0,
            0.0, 1.0, 0.0, 0.0,
            0.0, 0.0, 1.0, 0.0,
            0.0, 0.0, 0.0, 1.0
        )

        fun from(a: Vec3, b: Vec3, c: Vec3): Matrix4 {
            return Matrix4(a.x, b.x, c.x, 0.0, a.y, b.y, c.y, 0.0, a.z, b.z, c.z, 0.0, 0.0, 0.0, 0.0, 1.0)
        }

        fun from(
            a: Vec4,
            b: Vec4,
            c: Vec4,
            d: Vec4
        ): Matrix4 {
            return Matrix4(a.x, b.x, c.x, d.x, a.y, b.y, c.y, d.y, a.z, b.z, c.z, d.z, a.w, b.w, c.w, d.w)
        }

        fun translate(x: Double, y: Double, z: Double): Matrix4 {
            return Matrix4(
                1.0, 0.0, 0.0, x,
                0.0, 1.0, 0.0, y,
                0.0, 0.0, 1.0, z,
                0.0, 0.0, 0.0, 1.0
            )
        }

        fun translate(v: Vec3): Matrix4 {
            return translate(v.x, v.y, v.z)
        }

        fun scale(x: Double, y: Double, z: Double): Matrix4 {
            return Matrix4(
                x, 0.0, 0.0, 0.0,
                0.0, y, 0.0, 0.0,
                0.0, 0.0, z, 0.0,
                0.0, 0.0, 0.0, 1.0
            )
        }

        fun scale(v: Vec3): Matrix4 {
            return scale(v.x, v.y, v.z)
        }

        fun scale(k: Double): Matrix4 {
            return scale(k, k, k)
        }

        fun rotateX(radians: Double): Matrix4 {
            val c: Double = cos(radians)
            val s: Double = sin(radians)
            return Matrix4(
                1.0, 0.0, 0.0, 0.0,
                0.0, c, -s, 0.0,
                0.0, s, c, 0.0,
                0.0, 0.0, 0.0, 1.0
            )
        }

        fun rotateY(radians: Double): Matrix4 {
            val c: Double = cos(radians)
            val s: Double = sin(radians)
            return Matrix4(
                c, 0.0, s, 0.0,
                0.0, 1.0, 0.0, 0.0,
                -s, 0.0, c, 0.0,
                0.0, 0.0, 0.0, 1.0,
            )
        }

        fun rotateZ(radians: Double): Matrix4 {
            val c: Double = cos(radians)
            val s: Double = sin(radians)
            return Matrix4(
                c, -s, 0.0, 0.0,
                s, c, 0.0, 0.0,
                0.0, 0.0, 1.0, 0.0,
                0.0, 0.0, 0.0, 1.0
            )
        }

        fun mul(vararg matrices: Matrix4): Matrix4 {
            var m = matrices[0]
            for (i in 1 until matrices.size) {
                m = m.mul(matrices[i])
            }
            return m
        }
    }
}
