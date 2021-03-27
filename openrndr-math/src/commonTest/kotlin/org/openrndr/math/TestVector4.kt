package org.openrndr.math

import io.kotest.assertions.throwables.shouldThrow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import kotlinx.serialization.SerializationException
import org.openrndr.math.test.it
import kotlin.test.Test

class TestVector4 {

    private val maxError = 0.0000001

    @Test
    fun shouldDoVector4Operations() {

        it("should normalize 0 length") {
            Vector4.ZERO.normalized.closeTo(Vector4.ZERO, maxError)
        }

        it("should support 0 length") {
            Vector4.ZERO.length.closeTo(0.0, maxError)
        }
    }

    @Test
    fun shouldDoVector4Mix() {

        it("should .mix towards first component") {
            Vector4.ONE.mix(Vector4.ZERO, 0.0).closeTo(Vector4.ONE, maxError)
        }

        it("should .mix towards second component") {
            Vector4.ONE.mix(Vector4.ZERO, 1.0).closeTo(Vector4.ZERO, maxError)
        }

        it("should mix() towards first component") {
            mix(Vector4.ONE, Vector4.ZERO, 0.0).closeTo(Vector4.ONE, maxError)
        }

        it("should mix() towards second component") {
            mix(Vector4.ONE, Vector4.ZERO, 1.0).closeTo(Vector4.ZERO, maxError)
        }
    }

    @Test
    fun shouldSerialize() {

        it("should serialize ZERO to JSON") {
            Json.encodeToString(Vector4.ZERO) shouldMatch """\{"x":0(\.0)?,"y":0(\.0)?,"z":0(\.0)?,"w":0(\.0)?\}"""
        }

        it("should serialize ONE to JSON") {
            Json.encodeToString(Vector4.ONE) shouldMatch """\{"x":1(\.0)?,"y":1(\.0)?,"z":1(\.0)?,"w":1(\.0)?\}"""
        }

        it("should not serialize INFINITY to JSON") {
            shouldThrow<SerializationException> {
                Json.encodeToString(Vector4.INFINITY)
            }
        }

    }

    @Test
    fun shouldMatch() {

        Json.encodeToString(Vector2.ZERO) shouldMatch """\{"x":0(\.0)?,"y":0(\.0)?\}"""

    }

}