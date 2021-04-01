package org.openrndr.math

import org.amshove.kluent.`should be equal to`
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestSpherical : Spek({

    val maxError = 0.0000001

    describe("Spherical operations") {
        it("converts between spherical and vector") {
            val sp = Spherical(100.0, 140.0, 140.0)
            val v = sp.cartesian
            val sp2 = v.spherical

            sp2.radius `should be equal to` sp.radius
            sp2.phi `should be equal to` sp.phi
            sp2.theta `should be equal to` sp.theta

            val v2 = sp2.cartesian
            v2.x `should be equal to` v.x
            v2.y `should be equal to` v.y
            v2.z `should be equal to` v.z
        }
    }
})