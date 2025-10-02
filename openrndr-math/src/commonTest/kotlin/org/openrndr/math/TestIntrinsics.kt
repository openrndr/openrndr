package org.openrndr.math

import kotlin.math.sqrt
import kotlin.test.Test

class TestIntrinsics {

    @Test
    fun dot() {
        val dotFma = fmaDot(sqrt(2.0), sqrt(2.0), sqrt(2.0), sqrt(2.0), sqrt(2.0), sqrt(2.0))
        val dot = sqrt(2.0) * sqrt(2.0) + sqrt(2.0) * sqrt(2.0) + sqrt(2.0) * sqrt(2.0)
        println("fma: $dotFma, manual: $dot")
    }

}