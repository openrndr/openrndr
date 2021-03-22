package org.openrndr.math

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.openrndr.math.test.it
import kotlin.test.Test

class TestVector3 {

    private val maxError = 0.0000001

    @Test
    fun shouldDoVector3Operations() {

        it("should normalize 0 length") {
            Vector3.ZERO.normalized.closeTo(Vector3.ZERO, maxError)
        }

        it("should support 0 length") {
            Vector3.ZERO.length.closeTo(0.0, maxError)
        }
    }

    @Test
    fun shouldDoVector3Mix() {

        it("should .mix towards first component") {
            Vector3.ONE.mix(Vector3.ZERO, 0.0).closeTo(Vector3.ONE, maxError)
        }

        it("should .mix towards second component") {
            Vector3.ONE.mix(Vector3.ZERO, 1.0).closeTo(Vector3.ZERO, maxError)
        }

        it("should mix() towards first component") {
            mix(Vector3.ONE, Vector3.ZERO, 0.0).closeTo(Vector3.ONE, maxError)
        }

        it("should mix() towards second component") {
            mix(Vector3.ONE, Vector3.ZERO, 1.0).closeTo(Vector3.ZERO, maxError)
        }
    }

    @Test
    fun shouldSerialize() {

        it("should serialize ZERO to JSON") {
            Json.encodeToString(Vector3.ZERO) shouldMatch """\{"x":0(\.0)?,"y":0(\.0)?,"z":0(\.0)?\}"""
        }

        it("should serialize ONE to JSON") {
            Json.encodeToString(Vector3.ONE) shouldMatch """\{"x":1(\.0)?,"y":1(\.0)?,"z":1(\.0)?\}"""
        }

        it("should not serialize INFINITY to JSON") {
            shouldThrow<SerializationException> {
                Json.encodeToString(Vector3.INFINITY)
            }
        }

    }

}
