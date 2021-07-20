package org.openrndr.utils

/**
 * @author ztellman
 */
object Hashes {
    fun hash(x: Double): Int {
        val hash: Long = 31L * x.toBits()
        return (hash xor (hash ushr 32)).toInt()
    }

    fun hash(x: Double, y: Double): Int {
        var hash = 1L
        hash = hash * 31 + x.toBits()
        hash = hash * 31 + y.toBits()
        return (hash xor (hash ushr 32)).toInt()
    }

    fun hash(x: Double, y: Double, z: Double): Int {
        var hash = 1L
        hash = hash * 31 + x.toBits()
        hash = hash * 31 + y.toBits()
        hash = hash * 31 + z.toBits()
        return (hash xor (hash ushr 32)).toInt()
    }

    fun hash(x: Double, y: Double, z: Double, w: Double): Int {
        var hash = 1L
        hash = hash * 31 + x.toBits()
        hash = hash * 31 + y.toBits()
        hash = hash * 31 + z.toBits()
        hash = hash * 31 + w.toBits()
        return (hash xor (hash ushr 32)).toInt()
    }
}