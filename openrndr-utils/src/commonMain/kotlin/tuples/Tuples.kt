package org.openrndr.utils.tuples

data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
) {
    override fun toString(): String {
        return "($first, $second, $third, $fourth)"
    }
}

data class Quintuple<out A, out B, out C, out D, out E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
) {
    override fun toString(): String {
        return "($first, $second, $third, $fourth, $fifth)"
    }
}

data class Sextuple<out A, out B, out C, out D, out E, out F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F,
) {
    override fun toString(): String {
        return "($first, $second, $third, $fourth, $fifth, $sixth)"
    }
}
