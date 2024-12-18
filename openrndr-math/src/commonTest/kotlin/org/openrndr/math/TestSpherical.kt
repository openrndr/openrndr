package org.openrndr.math

import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertTrue

class TestSpherical {

    @Test
    fun shouldConform() {
        assertTrue(Spherical.UNIT_X.cartesian.distanceTo(Vector3.UNIT_X) < 1E-9)
        assertTrue(Spherical.UNIT_Y.cartesian.distanceTo(Vector3.UNIT_Y) < 1E-9)
        assertTrue(Spherical.UNIT_Z.cartesian.distanceTo(Vector3.UNIT_Z) < 1E-9)
    }

    @Test
    fun shouldBeConvertibleToCartesianAndBack() {
        // given
        val sp = Spherical(100.0, 140.0, 140.0)
        sp.phi
        val v = sp.cartesian
        val sp2 = v.spherical

        // then
        sp2.radius shouldBe sp.radius
        sp2.phi shouldBe sp.phi
        sp2.theta shouldBe sp.theta

        val v2 = sp2.cartesian
        v2.x shouldBe v.x
        v2.y shouldBe v.y
        v2.z shouldBe v.z
    }

    @Test
    fun shouldSerializeToJson() {
        // given
        val spherical = Spherical(1.42, 2.42, 3.42)
        Json.encodeToString(spherical) shouldBe """{"theta":1.42,"phi":2.42,"radius":3.42}"""
    }

}
