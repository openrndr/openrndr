package org.openrndr.color

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

class ColorRGBaTest {

    @Test
    @Suppress("ReplaceCallWithBinaryOperator")
    fun shouldDoEqualsProperly() {
        ColorRGBa.BLACK.equals(ColorRGBa.BLACK) shouldBe true
        ColorRGBa.BLACK.equals(ColorRGBa.WHITE) shouldBe false
        (ColorRGBa.BLACK == ColorRGBa.BLACK) shouldBe true
        (ColorRGBa.BLACK == ColorRGBa.WHITE) shouldBe false
        ColorRGBa.BLACK shouldBe ColorRGBa.BLACK
        ColorRGBa.BLACK shouldNotBe ColorRGBa.WHITE
        ColorRGBa.BLACK shouldBe ColorRGBa(0.0, 0.0, 0.0, 1.0, Linearity.SRGB)
        ColorRGBa.BLACK shouldNotBe ColorRGBa(0.0, 0.0, 0.0, 1.0, Linearity.LINEAR)
        ColorRGBa(0.0, 0.0, 0.0, 1.0, Linearity.SRGB) shouldBe ColorRGBa(0.0, 0.0, 0.0, 1.0, Linearity.SRGB)
    }

    @Test
    fun mixTwoColors() {
        val mixed = mix(
            ColorRGBa(1.0, 0.0, 0.0, 1.0, Linearity.ASSUMED_LINEAR),
            ColorRGBa(1.0, 1.0, 1.0, 0.0, Linearity.LINEAR),
            0.5
        )
        mixed.linearity shouldBe Linearity.ASSUMED_LINEAR
    }
}
