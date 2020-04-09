package org.openrndr.math

import org.amshove.kluent.`should be equal to`
import org.openrndr.resourceUrl
import org.openrndr.shape.Winding
import org.openrndr.svg.loadSVG
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestSVGLoader : Spek({

    describe("a simple SVG file") {
        val composition = loadSVG(resourceUrl("/svg/closed-shapes.svg"))

        it("has only closed shapes") {
            composition.findShapes().all { it.shape.contours.all { it.closed } } `should be equal to` true
        }

        it("has only clockwise shapes") {
            composition.findShapes().all { it.shape.contours.all { it.winding == Winding.CLOCKWISE } } `should be equal to` true
        }
    }

})