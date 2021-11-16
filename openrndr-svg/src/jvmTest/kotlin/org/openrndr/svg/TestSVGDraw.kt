package org.openrndr.svg

import org.openrndr.*
import kotlin.test.*

class TestSVGDraw {

    @Test
    fun complexSvgDoesNotBreakDrawer() {
        application {
            program {
                val composition = loadSVG(resourceUrl("/svg/text-template-2.svg"))
                for (shape in composition.findShapes().map { it.shape  }) {
                    for (c in shape.contours) {
                        for (s in c.segments) {
                            assertTrue(s.length > 10E-6)
                            val ap = s.adaptivePositions()
                            for (pair in ap.zipWithNext()) {
                                val d = pair.second - pair.first
                                assertTrue(d.length > 10E-6)
                            }
                        }
                        val p = c.adaptivePositions()
                        for (pair in p.zipWithNext()) {
                            val d = pair.second - pair.first
                            assertTrue(d.length > 10E-6)
                        }
                    }
                }
                extend {
                    drawer.composition(composition)
                    application.exit()
                }
            }
        }
    }
}