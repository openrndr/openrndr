package org.openrndr.math

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.openrndr.math.test.it
import kotlin.test.Test

class TestVector2 {

    private val maxError = 0.0000001

    @Test
    fun shouldHaveArea() {
        Vector2(0.0, 1.0).areaBetween(Vector2(0.0, 1.0)) shouldBe 0.0
        Vector2(0.0, -1.0).areaBetween(Vector2(0.0, 1.0)) shouldBe 0.0
        Vector2(0.0, 1.0).areaBetween(Vector2(0.0, -1.0)) shouldBe 0.0
        Vector2(0.0, -1.0).areaBetween(Vector2(0.0, -1.0)) shouldBe 0.0
        Vector2(0.0, 1.0).areaBetween(Vector2(1.0, 0.0)) shouldBe 1.0
        Vector2(0.0, 2.0).areaBetween(Vector2(2.0, 0.0)) shouldBe 4.0
        Vector2(0.0, 1.0).areaBetween(Vector2(2.0, 0.0)) shouldBe 2.0
        Vector2(0.0, 2.0).areaBetween(Vector2(1.0, 0.0)) shouldBe 2.0
    }

    @Test
    fun shouldHaveAHashcode() {
        val v0 = Vector2(0.0, 4.0)
        println(v0.hashCode())

    }

    @Test
    fun shouldDoVector2Operations() {

        it("should normalize 0 length") {
            Vector2.ZERO.normalized.closeTo(Vector2.ZERO, maxError)
        }

        it("should support 0 length") {
            Vector2.ZERO.length.closeTo(0.0, maxError)
        }
    }

    @Test
    fun shouldDoVector2Mix() {

        it("should .mix towards first component") {
            Vector2.ONE.mix(Vector2.ZERO, 0.0).closeTo(Vector2.ONE, maxError)
        }

        it("should .mix towards second component") {
            Vector2.ONE.mix(Vector2.ZERO, 1.0).closeTo(Vector2.ZERO, maxError)
        }

        it("should mix() towards first component") {
            mix(Vector2.ONE, Vector2.ZERO, 0.0).closeTo(Vector2.ONE, maxError)
        }

        it("should mix() towards second component") {
            mix(Vector2.ONE, Vector2.ZERO, 1.0).closeTo(Vector2.ZERO, maxError)
        }
    }

    @Test
    fun shouldSerialize() {

        it("should serialize ZERO to JSON") {
            Json.encodeToString(Vector2.ZERO) shouldMatch """\{"x":0(\.0)?,"y":0(\.0)?\}"""
        }

        it("should serialize ONE to JSON") {
            Json.encodeToString(Vector2.ONE) shouldMatch """\{"x":1(\.0)?,"y":1(\.0)?\}"""
        }

        it("should not serialize INFINITY to JSON") {
            shouldThrow<SerializationException> {
                Json.encodeToString(Vector2.INFINITY)
            }
        }

    }

    @Test
    fun shouldCalculateDotProduct() {
        Vector2.ZERO dot Vector2.ZERO shouldBe 0.0
        Vector2.ZERO dot Vector2.ONE shouldBe 0.0
        Vector2.ONE dot Vector2.ZERO shouldBe 0.0
        Vector2.ONE dot Vector2.ONE shouldBe 2.0
    }

}
