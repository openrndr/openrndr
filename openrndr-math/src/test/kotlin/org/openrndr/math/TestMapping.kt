package org.openrndr.math

import org.amshove.kluent.shouldBeInRange
import org.amshove.kluent.shouldNotBeInRange
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestMapping : Spek({

    val maxError = 0.0000001

    describe("Mapping Operations") {

        describe("Mapping Double") {
            val beforeLeft = 0.0
            val beforeRight = 1.0
            val beforeVal = beforeRight * 2.0 // out of range

            val afterLeft = 0.0
            val afterRight = beforeRight * 2.0

            it("should not clamp") {
                map(beforeLeft, beforeRight, afterLeft, afterRight, beforeVal)
                        .shouldNotBeInRange(afterLeft, afterRight)
            }

            it("should clamp") {
                map(beforeLeft, beforeRight, afterLeft, afterRight, beforeVal, true)
                        .shouldBeInRange(afterLeft, afterRight)
            }

            it("should not clamp") {
                beforeVal.map(beforeLeft, beforeRight, afterLeft, afterRight)
                        .shouldNotBeInRange(afterLeft, afterRight)
            }

            it("should clamp") {
                beforeVal.map(beforeLeft, beforeRight, afterLeft, afterRight, true)
                        .shouldBeInRange(afterLeft, afterRight)
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
                    mapped[i].shouldNotBeInRange(afterLeft[i], afterRight[i])
            }

            it("should clamp") {
                val mapped = beforeVal.map(beforeLeft, beforeRight,
                        afterLeft, afterRight, true)
                for (i in 0 until 2)
                    mapped[i].shouldBeInRange(afterLeft[i], afterRight[i])
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
                    mapped[i].shouldNotBeInRange(afterLeft[i], afterRight[i])
            }

            it("should clamp") {
                val mapped = beforeVal.map(beforeLeft, beforeRight,
                        afterLeft, afterRight, true)
                for (i in 0 until 3)
                    mapped[i].shouldBeInRange(afterLeft[i], afterRight[i])
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
                    mapped[i].shouldNotBeInRange(afterLeft[i], afterRight[i])
            }

            it("should clamp") {
                val mapped = beforeVal.map(beforeLeft, beforeRight,
                        afterLeft, afterRight, true)
                for (i in 0 until 4)
                    mapped[i].shouldBeInRange(afterLeft[i], afterRight[i])
            }
        }
    }
})