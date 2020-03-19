import org.amshove.kluent.`should be equal to`
import org.openrndr.math.Vector2
import org.openrndr.shape.SegmentJoin
import org.openrndr.shape.contour
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestShapeContour : Spek({

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

    describe("a simple contour") {
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

        it("can be offset with miter joins") {
            for (i in -100 until 100) {
                val offset = curve.offset(i * 1.0, SegmentJoin.MITER)
            }
        }

        it("can be offset with round joins") {
            for (i in -100 until 100) {
                val offset = curve.offset(i * 1.0, SegmentJoin.ROUND)
            }
        }

        it("can be offset with bevel joins") {
            for (i in -100 until 100) {
                val offset = curve.offset(i * 1.0, SegmentJoin.BEVEL)
            }
        }

        it("can be sampled for equidistant linear segments") {
            for (i in 1 until 100) {
                val resampled = curve.sampleEquidistant(i)
                resampled.position(0.0) `should be near` curve.position(0.0)
                resampled.position(1.0) `should be near` curve.position(1.0)
            }
        }
    }

    describe("another simple contour") {
        // https://github.com/openrndr/openrndr/issues/79#issuecomment-601119834
        val curve  = contour {
            moveTo(Vector2(60.0, 200.0))
            continueTo(Vector2(300.0, 300.0))
            continueTo(Vector2(280.0, 200.0))
            continueTo(Vector2(60.0, 200.0))
            close()
        }
        val offset0 = curve.offset(20.0, SegmentJoin.MITER)
        val offset1 = offset0.offset(20.0, SegmentJoin.MITER)
    }
})
