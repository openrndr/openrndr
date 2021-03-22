package org.openrndr.math

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldMatch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.openrndr.math.test.it
import kotlin.test.Test

class TestVector2 {

    private val maxError = 0.0000001

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

}
