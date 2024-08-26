import org.amshove.kluent.*
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import io.kotest.core.spec.style.DescribeSpec
import kotlin.math.sqrt

infix fun Vector2.`should be near`(other: Vector2) {
    x shouldBeInRange (other.x - 0.00001..other.x + 0.00001)
    y shouldBeInRange (other.y - 0.00001..other.y + 0.00001)
}

infix fun Vector2.`should be somewhat near`(other: Vector2) {
    x shouldBeInRange (other.x - 0.01..other.x + 0.01)
    y shouldBeInRange (other.y - 0.01..other.y + 0.01)
}


class TestSegment : DescribeSpec({
    describe("a horizontal segment") {
        val segment = Segment2D(Vector2(0.0, 100.0), Vector2(100.0, 100.0))
        segment.normal(0.0) `should be near` Vector2(0.0, -1.0)
    }

    describe("a linear segment") {
        val segment = Segment2D(Vector2(0.0, 0.0), Vector2(100.0, 100.0))
        it("has evaluable and correct bounds") {
            val bounds = segment.bounds
            bounds.x `should be equal to` segment.start.x
            bounds.y `should be equal to` segment.start.y
            bounds.width `should be equal to` 100.0
            bounds.height `should be equal to` 100.0
        }

        it("it has an evaluable normal at t = 0.0") {
            val normal = segment.normal(0.0)
            normal.length.shouldBeNear(1.0, 10E-6)
            println(normal)
        }

        it("can be split in half") {
            val sides = segment.split(0.5)
            sides.size `should be equal to` 2
        }

//        it("can be split at 0.0, but result in 1 part") {
//            val sides = segment.split(0.0)
//            sides.size `should be equal to` 1
//        }
//
//        it("can be split at 1.0, but result in 1 part") {
//            val sides = segment.split(1.0)
//            sides.size `should be equal to` 1
//        }
        it("can be subbed from 0.0 to 1.0") {
            val sub = segment.sub(0.0, 1.0)
            sub `should be` segment
        }
        it("can be subbed from 0.0 to 0.5") {
            val sub = segment.sub(0.0, 0.5)
            (sub.start - segment.start).squaredLength `should be equal to` 0.0
        }
        it("can be subbed from 0.5 to 1.0") {
            val sub = segment.sub(0.5, 1.0)
            (sub.end - segment.end).squaredLength `should be equal to` 0.0
        }

        it("has a length") {
            segment.length `should be equal to` sqrt(100.0 * 100.0 * 2.0)
        }

        it("has a normal") {
            segment.normal(0.0)
        }



        it("can be promoted to a quadratic segment") {
            val quadratic = segment.quadratic
            quadratic.position(0.0) `should be equal to` segment.position(0.0)
            quadratic.position(0.25) `should be equal to` segment.position(0.25)
            quadratic.position(0.5) `should be equal to` segment.position(0.5)
            quadratic.position(0.75) `should be equal to` segment.position(0.75)
            quadratic.position(1.0) `should be equal to` segment.position(1.0)

            quadratic.normal(0.0) `should be near` segment.normal(0.0)
            quadratic.normal(0.25) `should be near` segment.normal(0.25)
            quadratic.normal(0.5) `should be near` segment.normal(0.5)
            quadratic.normal(0.75) `should be near` segment.normal(0.75)
            quadratic.normal(1.0) `should be near` segment.normal(1.0)

        }

        it("can be promoted to a cubic segment") {
            val cubic = segment.cubic
            cubic.position(0.0) `should be near` segment.position(0.0)
            cubic.position(0.25) `should be near` segment.position(0.25)
            cubic.position(0.5) `should be near` segment.position(0.5)
            cubic.position(0.75) `should be near` segment.position(0.75)
            cubic.position(1.0) `should be near` segment.position(1.0)
        }

        it("it can sample equidistant points") {
            for (i in 2 until 100) {
                val points = segment.equidistantPositions(i)
                points.size `should be equal to` i
                points.first() `should be near` segment.position(0.0)
                points.last() `should be near`  segment.position(1.0)
            }
        }
    }

    describe("a quadratic segment with co-inciding p0/c0") {
        val segment = Segment2D(Vector2(10.0, 10.0), Vector2(10.0, 10.0), Vector2(100.0, 100.0))

        it("has a non-zero derivative at t=0") {
            segment.derivative(0.0).squaredLength `should be greater than` 0.0
        }

        it("has a non-zero derivative at t=0.5") {
            segment.derivative(0.5).squaredLength `should be greater than` 0.0
        }
        it("has a non-zero derivative at t=1") {
            segment.derivative(1.0).squaredLength `should be greater than` 0.0
        }
        it("it can sample equidistant points") {
            for (i in 2 until 100) {
                val points = segment.equidistantPositions(i)
                points.size `should be equal to` i
                points.first() `should be near` segment.position(0.0)
                points.last() `should be near`  segment.position(1.0)

            }
        }
    }

    describe("a quadratic segment with co-inciding p1/c0") {
        val segment = Segment2D(Vector2.ZERO, Vector2(100.0, 100.0), Vector2(100.0, 100.0))

        it("has a non-zero derivative at t=0") {
            segment.derivative(0.0).squaredLength `should be greater than` 0.0
        }

        it("has a non-zero derivative at t=0.5") {
            segment.derivative(0.5).squaredLength `should be greater than` 0.0
        }
        it("has a non-zero derivative at t=1") {
            segment.derivative(1.0).squaredLength `should be greater than` 0.0
        }
        it("it can sample equidistant points") {
            for (i in 2 until 100) {
                val points = segment.equidistantPositions(i)
                points.size `should be equal to` i
                points.first() `should be near` segment.position(0.0)
                points.last() `should be near`  segment.position(1.0)
            }
        }
    }

    describe("a cubic segment with co-inciding p0/c0") {
        val segment = Segment2D(Vector2(10.0, 10.0), Vector2(10.0, 10.0), Vector2(50.0, 50.0), Vector2(100.0, 100.0))

        it("has a non-zero derivative at t=0") {
            segment.derivative(0.0).squaredLength `should be greater than` 0.0
        }

        it("has a non-zero derivative at t=epsilon") {
            segment.derivative(0.00001).squaredLength `should be greater than` 0.0
        }

        it("has a non-zero derivative at t=1/3") {
            segment.derivative(1.0 / 3.0).squaredLength `should be greater than` 0.0
        }
        it("has a non-zero derivative at t=2/3") {
            segment.derivative(2.0 / 3.0).squaredLength `should be greater than` 0.0
        }
        it("has a non-zero derivative at t=1") {
            segment.derivative(1.0).squaredLength `should be greater than` 0.0
        }
        it("it can sample equidistant points") {
            for (i in 2 until 100) {
                val points = segment.equidistantPositions(i)
                points.size `should be equal to` i
                points.first() `should be near` segment.position(0.0)
                points.last() `should be near`  segment.position(1.0)
            }
        }
    }

    describe("a cubic segment with co-inciding p1/c1") {
        val segment = Segment2D(Vector2(10.0, 10.0), Vector2(50.0, 50.0), Vector2(100.0, 100.0), Vector2(100.0, 100.0))

        it("has a non-zero derivative at t=0") {
            segment.derivative(0.0).squaredLength `should be greater than` 0.0
        }

        it("has a non-zero derivative at t=epsilon") {
            segment.derivative(0.00001).squaredLength `should be greater than` 0.0
        }

        it("has a non-zero derivative at t=1/3") {
            segment.derivative(1.0 / 3.0).squaredLength `should be greater than` 0.0
        }
        it("has a non-zero derivative at t=2/3") {
            segment.derivative(2.0 / 3.0).squaredLength `should be greater than` 0.0
        }
        it("has a non-zero derivative at t=1") {
            segment.derivative(1.0).squaredLength `should be greater than` 0.0
        }
        it("it can sample equidistant points") {
            for (i in 2 until 100) {
                val points = segment.equidistantPositions(i)
                points.size `should be equal to` i
                points.first() `should be near` segment.position(0.0)
                points.last() `should be near`  segment.position(1.0)
            }
        }
    }


    describe("a cubic segment with co-inciding p0/c0 and p1/c1") {
        val segment = Segment2D(Vector2(10.0, 10.0), Vector2(10.0, 10.0), Vector2(100.0, 100.0), Vector2(100.0, 100.0))

        it("has a non-zero derivative at t=0") {
            segment.derivative(0.0).squaredLength `should be greater than` 0.0
        }

        it("has a non-zero derivative at t=epsilon") {
            segment.derivative(0.00001).squaredLength `should be greater than` 0.0
        }

        it("has a non-zero derivative at t=1/3") {
            segment.derivative(1.0 / 3.0).squaredLength `should be greater than` 0.0
        }

        it("has a non-zero derivative at t=1/2") {
            segment.derivative(1.0 / 2.0).squaredLength `should be greater than` 0.0
        }

        it("has a non-zero derivative at t=2/3") {
            segment.derivative(2.0 / 3.0).squaredLength `should be greater than` 0.0
        }
        it("has a non-zero derivative at t=1") {
            segment.derivative(1.0).squaredLength `should be greater than` 0.0
        }
        it("it can sample equidistant points") {
            for (i in 2 until 100) {
                val points = segment.equidistantPositions(i)
                points.size `should be equal to` i
                points.first() `should be near` segment.position(0.0)
                points.last() `should be near`  segment.position(1.0)
            }
        }
    }


    describe("a cubic segment with co-inciding c0/c1") {
        val segment = Segment2D(Vector2(10.0, 10.0), Vector2(50.0, 50.0), Vector2(50.0, 50.0), Vector2(100.0, 100.0))

        it("has a non-zero derivative at t=0") {
            segment.derivative(0.0).squaredLength `should be greater than` 0.0
        }

        it("has a non-zero derivative at t=epsilon") {
            segment.derivative(0.00001).squaredLength `should be greater than` 0.0
        }

        it("has a non-zero derivative at t=1/3") {
            segment.derivative(1.0 / 3.0).squaredLength `should be greater than` 0.0
        }

        it("has a non-zero derivative at t=1/2") {
            segment.derivative(1.0 / 2.0).squaredLength `should be greater than` 0.0
        }

        it("has a non-zero derivative at t=2/3") {
            segment.derivative(2.0 / 3.0).squaredLength `should be greater than` 0.0
        }
        it("has a non-zero derivative at t=1") {
            segment.derivative(1.0).squaredLength `should be greater than` 0.0
        }
        it("it can sample equidistant points") {
            for (i in 2 until 100) {
                val points = segment.equidistantPositions(i)
                points.size `should be equal to` i
                points.first() `should be near` segment.position(0.0)
                points.last() `should be near`  segment.position(1.0)
            }
        }
    }


    describe("a cubic segment") {
        val segment = Segment2D(Vector2(0.0, 0.0), Vector2(100.0, 100.0), Vector2(50.0, 100.0), Vector2(0.0, 100.0))

        it("has evaluable bounds") {
            segment.bounds
        }

        it("has evaluable extrema") {
            segment.extrema()
        }


        it("can be split in half") {
            val sides = segment.split(0.5)
            sides.size `should be equal to` 2
        }

//        it("can be split at 0.0, but result in 1 part") {
//            val sides = segment.split(0.0)
//            sides.size `should be equal to` 1
//        }
//
//        it("can be split at 1.0, but result in 1 part") {
//            val sides = segment.split(1.0)
//            sides.size `should be equal to` 1
//        }

        it("can be subbed from 0.0 to 1.0") {
            val sub = segment.sub(0.0, 1.0)
            sub `should be` segment
        }
        it("can be subbed from 0.0 to 0.5") {
            val sub = segment.sub(0.0, 0.5)
            (sub.start - segment.start).squaredLength `should be equal to` 0.0
        }
        it("can be subbed from 0.5 to 1.0") {
            val sub = segment.sub(0.5, 1.0)
            (sub.end - segment.end).squaredLength `should be equal to` 0.0
        }
        it("it can sample equidistant points") {
            for (i in 2 until 100) {
                val points = segment.equidistantPositions(i)
                points.size `should be equal to` i
                points.first() `should be near` segment.position(0.0)
                points.last() `should be near`  segment.position(1.0)
            }
        }
    }
})