package org.openrndr.color

/**
 * The matrices between the linear RGB spaces and XYZ, derived in full precision from the chromaticities of the
 * primaries and the D65 white point, so that each pair is inverse to double precision. Matrices are row-major.
 */
internal object RgbMatrices {
    /** the linear sRGB to XYZ matrix of IEC 61966-2-1 */
    val SRGB_TO_XYZ = rgbToXyz(0.64, 0.33, 0.30, 0.60, 0.15, 0.06, 0.3127, 0.3290)
    val XYZ_TO_SRGB = inverse(SRGB_TO_XYZ)

    /** the linear Display P3 to XYZ matrix, the primaries of DCI-P3 with the white point of sRGB */
    private val P3_TO_XYZ = rgbToXyz(0.680, 0.320, 0.265, 0.690, 0.150, 0.060, 0.3127, 0.3290)
    val P3_TO_SRGB = multiply(XYZ_TO_SRGB, P3_TO_XYZ)
    val SRGB_TO_P3 = inverse(P3_TO_SRGB)

    /**
     * The matrix whose columns are the XYZ of the primaries, scaled so that the primaries add up to the white point
     */
    private fun rgbToXyz(
        xr: Double, yr: Double, xg: Double, yg: Double, xb: Double, yb: Double, xw: Double, yw: Double
    ): DoubleArray {
        val primaries = doubleArrayOf(
            xr / yr, xg / yg, xb / yb,
            1.0, 1.0, 1.0,
            (1 - xr - yr) / yr, (1 - xg - yg) / yg, (1 - xb - yb) / yb
        )
        val white = doubleArrayOf(xw / yw, 1.0, (1 - xw - yw) / yw)
        val inverse = inverse(primaries)
        val s = DoubleArray(3) { i -> inverse[i * 3] * white[0] + inverse[i * 3 + 1] * white[1] + inverse[i * 3 + 2] * white[2] }
        return DoubleArray(9) { primaries[it] * s[it % 3] }
    }

    private fun inverse(m: DoubleArray): DoubleArray {
        val c00 = m[4] * m[8] - m[5] * m[7]
        val c01 = m[5] * m[6] - m[3] * m[8]
        val c02 = m[3] * m[7] - m[4] * m[6]
        val det = m[0] * c00 + m[1] * c01 + m[2] * c02
        val inv = 1.0 / det
        return doubleArrayOf(
            c00 * inv, (m[2] * m[7] - m[1] * m[8]) * inv, (m[1] * m[5] - m[2] * m[4]) * inv,
            c01 * inv, (m[0] * m[8] - m[2] * m[6]) * inv, (m[2] * m[3] - m[0] * m[5]) * inv,
            c02 * inv, (m[1] * m[6] - m[0] * m[7]) * inv, (m[0] * m[4] - m[1] * m[3]) * inv
        )
    }

    private fun multiply(a: DoubleArray, b: DoubleArray) = DoubleArray(9) { i ->
        val row = i / 3
        val column = i % 3
        a[row * 3] * b[column] + a[row * 3 + 1] * b[3 + column] + a[row * 3 + 2] * b[6 + column]
    }
}

/** Chromas in LCH below this are those of grays, rounding errors of their conversions */
internal const val GRAY_CHROMA = 1E-10
