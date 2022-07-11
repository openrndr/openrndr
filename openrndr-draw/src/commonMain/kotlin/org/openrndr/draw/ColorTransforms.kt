package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix55

fun constant(color: ColorRGBa, ignoreAlpha: Boolean = true): Matrix55 {
    return Matrix55.IDENTITY.copy(c4r0 = color.r, c4r1 = color.g, c4r2 = color.b,
        c4r3 = if (ignoreAlpha) 0.0 else color.alpha
    )
}

fun tint(color: ColorRGBa): Matrix55 {
    return Matrix55(c0r0 = color.r, c1r1 = color.g, c2r2 = color.b, c3r3 = color.alpha, c4r4 = 1.0)
}

val invert: Matrix55 by lazy {
    Matrix55(c0r0 = -1.0, c1r1 = -1.0, c2r2 = -1.0, c3r3 = 1.0, c4r0 = 1.0, c4r1 = 1.0, c4r2 = 1.0, c4r3 = 0.0, c4r4 = 1.0)
}

fun grayscale(r: Double = 0.33, g: Double = 0.33, b: Double = 0.33): Matrix55 {
    return Matrix55(
            c0r0 = r, c1r0 = g, c2r0 = b,
            c0r1 = r, c1r1 = g, c2r1 = b,
            c0r2 = r, c1r2 = g, c2r2 = b,
            c3r3 = 1.0,
            c4r4 = 1.0)
}