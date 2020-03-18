import org.amshove.kluent.`should be equal to`
import org.openrndr.math.Vector2
import org.openrndr.shape.contour
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestShapeContour : Spek({
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
            for (i in 0 .. 1000) {
                val normal = curve.normal(i/1000.0)
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

        it("can be offset") {
            val offsetSegments = curve.segments.map { it.offset(20.0) }
            val offset = curve.offset(20.0)

            println(offsetSegments.size)
            println(offsetSegments.sumBy { it.size })
            println(offset.segments.size)
        }

    }
})
