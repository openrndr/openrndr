package org.openrndr.math

import kotlin.math.absoluteValue
import io.kotest.matchers.doubles.between
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.openrndr.math.test.it
import org.openrndr.math.test.it as describe
import kotlin.test.Test

class TestMapping {

    @Test
    fun shouldDoMappingOperations() {
        describe("Mapping Double from zero-width before domain to non-zero-width after domain") {
            it("should not produce NaN results") {
                map(0.0, 0.0, 0.0, 1.0, 0.0) shouldBe (0.0 plusOrMinus 10E-6)
                map(0.0, 0.0, 0.0, 1.0, 1.0) shouldBe (1.0 plusOrMinus 10E-6)
            }
        }

        describe("Mapping Double from zero-width before domain to zero-width after domain") {
            it("should not produce NaN results") {
                map(0.0, 0.0, 0.0, 0.0, 0.0) shouldBe (0.0 plusOrMinus 10E-6)
                map(0.0, 0.0, 0.0, 0.0, 1.0) shouldBe (0.0 plusOrMinus 10E-6)
            }
        }

        describe("Mapping Double") {
            val beforeLeft = 0.0
            val beforeRight = 1.0
            val beforeVal = beforeRight * 2.0 // out of range

            val afterLeft = 0.0
            val afterRight = beforeRight * 2.0

            it("should not clamp") {
                map(
                    beforeLeft, beforeRight, afterLeft, afterRight, beforeVal
                ) shouldNotBe between(afterLeft, afterRight, 0.0)
            }

            it("should clamp") {
                map(
                    beforeLeft, beforeRight, afterLeft, afterRight, beforeVal, true
                ) shouldBe between(afterLeft, afterRight, 0.0)
            }

            it("should not clamp") {
                beforeVal.map(
                    beforeLeft, beforeRight, afterLeft, afterRight
                ) shouldNotBe between(afterLeft, afterRight, 0.0)
            }

            it("should clamp") {
                beforeVal.map(
                    beforeLeft, beforeRight, afterLeft, afterRight, true
                ) shouldBe between(afterLeft, afterRight, 0.0)
            }
        }

        describe("Mapping Vector2") {
            val beforeLeft = Vector2.ZERO
            val beforeRight = Vector2.ONE
            val beforeVal = beforeRight * 2.0 // out of range

            val afterLeft = Vector2.ZERO
            val afterRight = beforeRight * 2.0

            it("should not clamp") {
                val mapped = beforeVal.map(beforeLeft, beforeRight,
                    afterLeft, afterRight)
                for (i in 0 until 2)
                    mapped[i] shouldNotBe between(afterLeft[i], afterRight[i], 0.0)
            }

            it("should clamp") {
                val mapped = beforeVal.map(beforeLeft, beforeRight,
                    afterLeft, afterRight, true)
                for (i in 0 until 2)
                    mapped[i] shouldBe between(afterLeft[i], afterRight[i], 0.0)
            }
        }

        describe("Mapping Vector3") {
            val beforeLeft = Vector3.ZERO
            val beforeRight = Vector3.ONE
            val beforeVal = beforeRight * 2.0 // out of range

            val afterLeft = Vector3.ZERO
            val afterRight = beforeRight * 2.0

            it("should not clamp") {
                val mapped = beforeVal.map(beforeLeft, beforeRight,
                    afterLeft, afterRight)
                for (i in 0 until 3)
                    mapped[i] shouldNotBe between(afterLeft[i], afterRight[i], 0.0)
            }

            it("should clamp") {
                val mapped = beforeVal.map(beforeLeft, beforeRight,
                    afterLeft, afterRight, true)
                for (i in 0 until 3)
                    mapped[i] shouldBe between(afterLeft[i], afterRight[i], 0.0)
            }
        }

        describe("Mapping Vector4") {
            val beforeLeft = Vector4.ZERO
            val beforeRight = Vector4.ONE
            val beforeVal = beforeRight * 2.0 // out of range

            val afterLeft = Vector4.ZERO
            val afterRight = beforeRight * 2.0

            it("should not clamp") {
                val mapped = beforeVal.map(beforeLeft, beforeRight,
                    afterLeft, afterRight)
                for (i in 0 until 4)
                    mapped[i] shouldNotBe between(afterLeft[i], afterRight[i], 0.0)
            }

            it("should clamp") {
                val mapped = beforeVal.map(beforeLeft, beforeRight,
                    afterLeft, afterRight, true)
                for (i in 0 until 4)
                    mapped[i] shouldBe between(afterLeft[i], afterRight[i], 0.0)
            }
        }
    }

    @Test
    fun shouldDoMixingOperations() {
        describe("Mixing double") {
            it("should produce expected result") {
                mix(1.0, 3.0, 0.5) shouldBe (2.0 plusOrMinus 10E-6)
                mix(3.0, 1.0, 0.5) shouldBe (2.0 plusOrMinus 10E-6)
            }
        }
        describe("Mixing angles") {
            it("should interpolate via shortest side") {
                mixAngle(5.0, 355.0, 0.5) shouldBe (0.0 plusOrMinus  10E-6)
                mixAngle(355.0, 5.0, 0.5) shouldBe (0.0 plusOrMinus  10E-6)
                mixAngle(-100.0, 100.0, 0.5).absoluteValue shouldBe (180.0 plusOrMinus  10E-6)
                mixAngle(100.0, -100.0, 0.5).absoluteValue shouldBe (180.0 plusOrMinus  10E-6)
            }
        }
    }

}
