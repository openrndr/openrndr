package org.openrndr.math

import org.openrndr.shape.CompositionDrawer
import org.openrndr.shape.compound
import org.openrndr.svg.writeSVG
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestSVGWriter : Spek({

    describe("a composition") {
        val cd = CompositionDrawer()
        cd.group("Layer_2") {
            val circle = cd.circle(Vector2(100.0, 100.0), 40.0)
            circle.attributes["openrndr:test"] = "5"
        }
        val comp = cd.composition
        it("can be written to SVG string") {
            val t = writeSVG(comp)
            println(t)
        }
    }
})