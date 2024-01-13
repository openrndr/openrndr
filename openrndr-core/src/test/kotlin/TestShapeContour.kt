import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should match all with`
import org.amshove.kluent.`should not be equal to`
import org.openrndr.math.Vector2
import org.openrndr.shape.*
import io.kotest.core.spec.style.DescribeSpec

class TestShapeContour : DescribeSpec({

    describe("two equivalent org.openrndr.shape.contours") {
        val a = Circle(40.0, 40.0, 100.0).contour
        val b = Circle(40.0, 40.0, 100.0).contour
        it("should be equal") {
            a `should be equal to` b
        }
    }

    describe("two non-equivalent org.openrndr.shape.contours") {
        val a = Circle(40.0, 40.0, 100.0).contour
        val b = Circle(40.0, 80.0, 100.0).contour
        it("should not be equal") {
            a `should not be equal to` b
        }
    }

    describe("a single line contour") {
        val c = contour {
            moveTo(0.0, 0.0)
            lineTo(100.0, 100.0)
        }

        it("can sample 0 points") {
            val points = c.equidistantPositions(0)
            points.size `should be equal to` 0
        }

        it("can sample 1 points") {
            val points = c.equidistantPositions(1)
            points[0] `should be near` Vector2.ZERO
            points.size `should be equal to` 1
        }

        it("can sample 2 points") {
            val points = c.equidistantPositions(2)
            points[0] `should be near` Vector2.ZERO
            points[1] `should be near` Vector2(100.0, 100.0)
            points.size `should be equal to` 2
        }

        it("can sample 3 points") {
            val points = c.equidistantPositions(3)
            points[0] `should be near` Vector2.ZERO
            points[1] `should be near` Vector2(50.0, 50.0)
            points[2] `should be near` Vector2(100.0, 100.0)
            points.size `should be equal to` 3
        }

        it("can sample 4 points") {
            val points = c.equidistantPositions(4)
            points[0] `should be near` Vector2.ZERO
            points[1] `should be near` Vector2(100.0 / 3, 100.0 / 3)
            points[2] `should be near` Vector2(200.0 / 3, 200.0 / 3)
            points[3] `should be near` Vector2(100.0, 100.0)
            points.size `should be equal to` 4
        }
    }

    describe("a two line contour") {
        val curve = contour {
            moveTo(0.0, 0.0)
            lineTo(100.0, 100.0)
            lineTo(200.0, 100.0)
        }
        it("can be sampled adaptively") {
            val resampled = curve.sampleLinear()
            resampled.closed `should be equal to` false
            resampled.position(0.0) `should be near` curve.position(0.0)
            resampled.position(1.0) `should be near` curve.position(1.0)
        }
        it("can be sampled for equidistant points") {
            for (i in 4 until 100) {
                val points = curve.equidistantPositions(i)
                points.size `should be equal to` i

            }
        }

        it("can be sampled for equidistant linear segments") {
            for (i in 4 until 100) {
                val resampled = curve.sampleEquidistant(i)
                resampled.position(0.0) `should be near` curve.position(0.0)
                resampled.position(1.0) `should be near` curve.position(1.0)
                resampled.closed `should be equal to` false
            }
        }
    }

    describe("a simple open contour") {
        val width = 640
        val height = 480

        val curve = contour {
            moveTo(Vector2(0.1 * width, 0.3 * height))
            continueTo(Vector2(0.5 * width, 0.5 * height))
            continueTo(Vector2(0.9 * width, 0.3 * height))
        }

    }

    describe("a simple closed contour") {
        val width = 640
        val height = 480
        val curve = contour {
            moveTo(Vector2(0.1 * width, 0.3 * height))
            continueTo(Vector2(0.5 * width, 0.5 * height))
            continueTo(Vector2(0.9 * width, 0.3 * height))
            continueTo(Vector2(0.1 * width, 0.3 * height))
            close()
        }

        it("has proper normals") {
            for (i in 0..1000) {
                val normal = curve.normal(i / 1000.0)
                normal.x `should be equal to` normal.x
                normal.y `should be equal to` normal.y
            }
        }

        it("it has the right number of segments") {
            curve.segments.size `should be equal to` 3
        }

        it("it is properly closed") {
            curve.position(0.0) `should be near` curve.position(1.0)
        }


        it("can be sampled for equidistant linear segments") {
            for (i in 4 until 100) {
                val resampled = curve.sampleEquidistant(i)
                resampled.position(0.0) `should be near` curve.position(0.0)
                resampled.position(1.0) `should be near` curve.position(1.0)
            }
        }
    }

    describe("another simple closed contour") {
        // https://github.com/openrndr/openrndr/issues/79#issuecomment-601119834
        val curve = contour {
            moveTo(Vector2(60.0, 200.0))
            continueTo(Vector2(300.0, 300.0))
            continueTo(Vector2(280.0, 200.0))
            continueTo(Vector2(60.0, 200.0))
            close()
        }

        val positions = curve.adaptivePositions()
        positions.zipWithNext().`should match all with` { (it.second - it.first).squaredLength > 0.0 }


        it("has CCW winding") {
            curve.winding `should be equal to` Winding.COUNTER_CLOCKWISE
        }


        it("can be sampled adaptively") {
            val resampled = curve.sampleLinear()
            resampled.closed `should be equal to` true
            resampled.position(0.0) `should be near` curve.position(0.0)
            resampled.position(1.0) `should be near` curve.position(1.0)
        }

    }

    describe("a circle contour") {
        val curve = Circle(100.0, 100.0, 200.0).contour
        it("can be sampled adaptively") {
            val resampled = curve.sampleLinear()
            resampled.closed `should be equal to` true
            resampled.position(0.0) `should be near` curve.position(0.0)
            resampled.position(1.0) `should be near` curve.position(1.0)
        }
        it("can be sampled for equidistant linear segments") {
            for (i in 4 until 100) {
                val resampled = curve.sampleEquidistant(i)
                resampled.position(0.0) `should be near` curve.position(0.0)
                resampled.closed `should be equal to` true
            }
        }

        it("it can be subbed from 0.0 to 1.0") {
            val s = curve.sub(0.0, 1.0)
            s.position(0.0) `should be near` curve.position(0.0)
            s.position(1.0) `should be near` curve.position(1.0)
        }

        it("it can be subbed from -0.1 to 0.0") {
            curve.sub(-0.1, 0.0)
        }

        it("it can be subbed from -2.0 to -1.0") {
            val s = curve.sub(-2.0, -1.0)
            s.position(0.0) `should be near` curve.position(0.0)
            s.position(1.0) `should be near` curve.position(1.0)
        }
        it("it can be subbed from 0 to 0") {
            val s = curve.sub(0.0, 0.0)
            println(s.length)

        }
        it("it can be subbed from -1 to -1") {
            curve.sub(-1.0, -1.0)

        }
        it("it can be subbed from 0.0 to 0.001 ") {
//            for (i in -2000 .. 10000) {
//                val o = i / 10000
//                val s = curve.sub(0.0 + o, 0.01 + o)
//                s.position(0.0) `should be somewhat near` curve.position(0.0 + o)
//                s.position(1.0) `should be somewhat near` curve.position(0.01+ o)
//            }
        }

        it("it can be subbed from 0.0 to 0.001 ") {
            val s = curve.sub(0.0, 1 / 100000.0)
            s.position(1.0) `should be somewhat near` curve.position(1 / 100000.0)
        }

        it("adaptive positions and corners should have the same length") {
            val pc = curve.adaptivePositionsAndCorners()
            pc.first.size `should be equal to` pc.second.size
        }

//        it("it can be subbed from 0.0 to 0.001 ") {
//            for (i in 1 until 100000) {
//
//                val s = curve.sub(0.0, i/100000.0)
//                println(i)
//                s.position(1.0) `should be somewhat near` curve.position(i/100000.0)
//
//
//            }
//        }

    }
    describe("a rectangle contour") {
        val curve = Rectangle(100.0, 100.0, 200.0, 200.0).contour

        it("can be sampled adaptively") {
            val resampled = curve.sampleLinear()
            resampled.closed `should be equal to` true
            resampled.position(0.0) `should be near` curve.position(0.0)
            resampled.position(1.0) `should be near` curve.position(1.0)
        }

        it("can be sampled for equidistant linear segments") {
            for (i in 4 until 100) {
                val resampled = curve.sampleEquidistant(i)
                resampled.position(0.0) `should be near` curve.position(0.0)
                resampled.position(1.0) `should be near` curve.position(1.0)
                resampled.closed `should be equal to` true
            }
        }
    }

    describe("a very specific contour") {
        val c = contour {
            moveTo(0.0, 0.0)
            curveTo(204.0, 378.0, 800.0, 240.0)
            curveTo(204.0, 378.0, 0.0, 480.0)
        }
        val points = c.equidistantPositions(100)
        it("should give 100 points") {
            points.size `should be equal to` 100
        }
    }

})
