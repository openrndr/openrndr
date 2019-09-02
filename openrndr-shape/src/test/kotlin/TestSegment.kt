
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestCircleDrawerGL3 : Spek({

    describe("a program") {
        describe("a segment") {
            val segment = Segment(Vector2(0.0, 0.0), Vector2(100.0, 100.0), Vector2(50.0, 100.0), Vector2(0.0, 100.0))
            val extrema = segment.extrema()
            println(segment.bounds)

            extrema.forEach {
                println(it)
            }

            println(segment.reduced.size)
            segment.reduced.forEach {
                println(it)
                it.scale(0.5)
            }

            segment.offset(5.0)

        }
    }
})