package org.openrndr.math

import kotlin.jvm.JvmInline

@JvmInline
value class Float16(val v: UShort) {

    companion object {
        val MIN_VALUE = Float16(0x0001u)
        val MAX_VALUE = Float16(0x7bffu)

        val MIN_VALUE_FLOAT = MIN_VALUE.toFloat()
        val MAX_VALUE_FLOAT = MAX_VALUE.toFloat()
        
        val POSITIVE_INFINITY = Float16(0x7c00u)
        val NEGATIVE_INFINITY = Float16(0xfc00u)
        val NaN = Float16(0x7e00u)

        fun fromFloat(f: Float): Float16 {
            val bits = f.toRawBits()
            val s = (bits shr 16) and 0x8000
            var e = ((bits shr 23) and 0xff) - (127 - 15)
            var m = bits and 0x7fffff

            if (e <= 0) {
                if (e < -10) {
                    return Float16((s and 0x8000).toUShort())
                }
                m = m or 0x800000
                val shift = 14 - e
                val add = 1 shl (shift - 1)
                m = (m + add) shr shift
                return Float16(((s and 0x8000) or m).toUShort())
            } else if (e >= 31) {
                if (e == 31 + (127 - 15) && m != 0) {
                    // NaN
                    return Float16(((s and 0x8000) or 0x7e00 or (m shr 13)).toUShort())
                }
                // Infinity
                return Float16(((s and 0x8000) or 0x7c00).toUShort())
            }

            m = m + 0x00001000 + ((m shr 13) and 1)
            if (m and 0x00800000 != 0) {
                m = 0
                e++
            }

            if (e >= 31) {
                return Float16(((s and 0x8000) or 0x7c00).toUShort())
            }

            return Float16(((s and 0x8000) or (e shl 10) or (m shr 13)).toUShort())
        }
    }

    fun toFloat(): Float {
        val bits = v.toInt()
        val s = (bits and 0x8000) shl 16
        var e = (bits and 0x7c00) shr 10
        var m = bits and 0x03ff

        if (e == 0) {
            if (m == 0) {
                return Float.fromBits(s)
            } else {
                while (m and 0x0400 == 0) {
                    m = m shl 1
                    e--
                }
                e++
                m = m and 0x03ff
            }
        } else if (e == 31) {
            return Float.fromBits(s or 0x7f800000 or (m shl 13))
        }

        e = e + (127 - 15)
        m = m shl 13
        return Float.fromBits(s or (e shl 23) or m)
    }

    fun toDouble(): Double = toFloat().toDouble()
}

fun Float.toFloat16(): Float16 = Float16.fromFloat(this)
fun Double.toFloat16(): Float16 = this.toFloat().toFloat16()