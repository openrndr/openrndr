package org.openrndr.svg

import org.openrndr.*
import org.openrndr.color.*
import org.openrndr.math.*
import org.openrndr.shape.*
import kotlin.test.*

class TestSVGLoader {

    @Test
    fun loadSimpleSvgFile() {
        val composition = loadSVG(resourceUrl("/svg/closed-shapes.svg"))

        assertContains(composition.namespaces, "xmlns", "it has namespaces")

        assertTrue("has only closed shapes") {
            composition.findShapes().all { it.shape.contours.all(ShapeContour::closed) }
        }

        assertTrue("has only clockwise shapes") {
            composition.findShapes()
                .all { node -> node.shape.contours.all { it.winding == Winding.CLOCKWISE } }
        }
    }

    @Test
    fun starSvgFile() {
        val composition = loadSVG(resourceUrl("/svg/star.svg"))

        assertTrue("has only open shapes") {
            composition.findShapes()
                .all { it.shape.contours.none(ShapeContour::closed) }
        }

        assertSame(
            composition.findShapes().size,
            37
        )
    }

    @Test
    fun text001SvgFile() {
        val composition = loadSVG(resourceUrl("/svg/text-001.svg"))

        assertTrue("all shapes are closed") {
            composition.findShapes()
                .all { it.shape.contours.all(ShapeContour::closed) }
        }
    }

    @Test
    fun openCompoundSvgFile() {
        val composition = loadSVG(resourceUrl("/svg/open-compound.svg"))

        assertSame(
            composition.findShapes()[0].shape.topology,
            ShapeTopology.OPEN
        )

        assertFalse {
            composition.findShapes()
                .all { it.shape.contours.all(ShapeContour::closed) }
        }
    }

    @Test
    fun patterns2SvgFile() {
        val composition = loadSVG(resourceUrl("/svg/patterns-2.svg"))

        assertIs<List<Vector2>>(
            triangulate(composition.findShapes()[0].shape)
        )

        assertTrue("all shapes are closed") {
            composition.findShapes()
                .all { it.shape.contours.all(ShapeContour::closed) }
        }
    }

    @Test
    fun deltaSvgFile() {
        val composition = loadSVG(resourceUrl("/svg/delta.svg"))

        assertContentEquals(
            composition.findShapes()[1].effectiveTransform.toDoubleArray(),
            Matrix44(
                0.2, 0.0, 0.0, 8.0,
                0.0, 0.2, 0.0, 21.888,
                0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 1.0,
            ).toDoubleArray(),
            "has correctly calculated a list of transforms"
        )
    }

    @Test
    fun viewboxAndGraphicsSvgFile() {
        val composition = loadSVG(resourceUrl("/svg/viewBox-and-graphics.svg"))
        val shape0 = composition.findShapes()[0]
        val shape1 = composition.findShapes()[1]

        assertTrue("has the specified viewBox") {
            (composition.documentStyle.viewBox as ViewBox.Value).value == Rectangle(
                8.0,
                0.0,
                16.0,
                16.0
            )
        }

        assertTrue("has the specified dimensions") {
            composition.bounds == CompositionDimensions(
                6.0.pixels,
                7.0.pixels,
                32.0.pixels,
                32.0.pixels
            )
        }

        assertContentEquals(
            composition.calculateViewportTransform().toDoubleArray(),
            Matrix44(
                2.0, 0.0, 0.0, -10.0,
                0.0, 2.0, 0.0, 7.0,
                0.0, 0.0, 1.0, 0.0,
                0.0, 0.0, 0.0, 1.0
            ).toDoubleArray(),
            "can calculate the viewBox transformation"
        )

        assertContentEquals(
            shape0.effectiveTransform.toDoubleArray(),
            Matrix44(
                1.0, 0.0, 0.0, 16.0,
                0.0, 1.0, 0.0, -8.0,
                0.0, 0.0, 1.0, 0.0,
                0.0, 0.0, 0.0, 1.0
            ).toDoubleArray(),
            "can calculate the element transformation"
        )

        assertTrue("has correctly parsed the functional RGB notation in fill") {
            shape0.fill == ColorRGBa(0.75, 0.14, 0.33, linearity = Linearity.SRGB)
        }

        assertSame(
            shape1.lineCap,
            org.openrndr.draw.LineCap.ROUND
        )

        assertSame(
            shape1.lineJoin,
            org.openrndr.draw.LineJoin.ROUND
        )

        assertEquals(
            shape1.miterLimit,
            50.0,
            10e-6
        )

        assertEquals(
            shape1.opacity,
            0.8,
            10e-6
        )

        assertEquals(
            shape1.strokeOpacity,
            0.5,
            10e-6
        )

        assertEquals(
            shape1.fillOpacity,
            0.25,
            10e-6
        )
    }

    @Test
    fun polylineSvgFile() {
        val composition = loadSVG(resourceUrl("/svg/polyline.svg"))
        val largePolyline = composition.findShape("large")!!
        val contours = largePolyline.shape.contours
        assertEquals(1, contours.size)
        assertEquals(79, contours[0].segments.size)
        val flawedPolyline = composition.findShape("flawed")!!
        assertEquals(0, flawedPolyline.shape.contours.size)
    }
}