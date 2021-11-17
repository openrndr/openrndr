package org.openrndr.svg

import org.openrndr.color.*
import org.openrndr.math.*
import org.openrndr.shape.*
import kotlin.test.*

class TestSVGWriter {

    @Test
    fun compositionCanBeSerializedToSvg() {
        val cd = CompositionDrawer()
        cd.group(id = "Layer_2") {
            val circle = cd.circle(Vector2(100.0, 100.0), 40.0)
            circle?.attributes?.set("openrndr:test", "5")
        }
        val comp = cd.composition
        assertIs<String>(writeSVG(comp))
    }

    @Test
    fun compositionWithTransforms() {
        val comp = drawComposition {
            transformMode = TransformMode.KEEP
            group {
                fill = ColorRGBa.RED
                translate(Vector2(50.0, 50.0))
                circle(Vector2(100.0, 100.0), 50.0)?.id = "circle"
            }
        }

        val svgString = comp.toSVG()

        assertTrue("can serialize colors") {
            // The shorthand hex color is just as valid
            svgString.contains("fill=\"#ff0000\"") || svgString.contains("fill=\"#f00\"")
        }

        val loaded = loadSVG(svgString)
        val shape = loaded.findShape("circle")

        assertTrue("calculates correct effectiveFill") {
            shape?.effectiveFill == ColorRGBa(1.0, 0.0, 0.0, 1.0, Linearity.SRGB)
        }

        assertTrue("calculates correct effectiveTransform") {
            shape?.effectiveTransform?.get(3) == Vector4(50.0, 50.0, 0.0, 1.0)
        }
    }
}