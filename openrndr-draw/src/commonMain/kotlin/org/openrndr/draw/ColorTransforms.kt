package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix55

/**
 * Creates a 5x5 matrix based on the given color values.
 *
 * @param color The color represented as an instance of ColorRGBa, where the r, g, b, and alpha
 * components will be used to modify the matrix.
 * @param ignoreAlpha A boolean flag indicating whether the alpha component of the color should be ignored.
 * If true, the alpha value in the matrix will be set to 0.0. Defaults to true.
 * @return A 5x5 matrix (Matrix55) with the r, g, b components set in the corresponding matrix columns,
 * and the alpha value determined by the ignoreAlpha parameter.
 */
fun constant(color: ColorRGBa, ignoreAlpha: Boolean = true): Matrix55 {
    return Matrix55.IDENTITY.copy(c4r0 = color.r, c4r1 = color.g, c4r2 = color.b,
        c4r3 = if (ignoreAlpha) 0.0 else color.alpha
    )
}

/**
 * Applies a color tint transformation and returns a 5x5 matrix representing the transformation.
 *
 * @param color The `ColorRGBa` instance containing the red, green, blue, and alpha values of the color tint to apply.
 * @return A 5x5 transformation matrix with the color tint applied based on the provided color.
 */
fun tint(color: ColorRGBa): Matrix55 {
    return Matrix55(c0r0 = color.r, c1r1 = color.g, c2r2 = color.b, c3r3 = color.alpha, c4r4 = 1.0)
}

/**
 * A lazily initialized 5x5 matrix (Matrix55) representing a transformation matrix.
 * The matrix is configured with specific coefficient values to perform an inversion transformation.
 */
val invert: Matrix55 by lazy {
    Matrix55(c0r0 = -1.0, c1r1 = -1.0, c2r2 = -1.0, c3r3 = 1.0, c4r0 = 1.0, c4r1 = 1.0, c4r2 = 1.0, c4r3 = 0.0, c4r4 = 1.0)
}

/**
 * Creates a grayscale transformation matrix with the specified red, green, and blue coefficients.
 *
 * @param r The coefficient for the red channel. Default is 0.33.
 * @param g The coefficient for the green channel. Default is 0.33.
 * @param b The coefficient for the blue channel. Default is 0.33.
 * @return A 5x5 matrix representing the grayscale transformation.
 */
fun grayscale(r: Double = 0.33, g: Double = 0.33, b: Double = 0.33): Matrix55 {
    return Matrix55(
            c0r0 = r, c1r0 = g, c2r0 = b,
            c0r1 = r, c1r1 = g, c2r1 = b,
            c0r2 = r, c1r2 = g, c2r2 = b,
            c3r3 = 1.0,
            c4r4 = 1.0)
}