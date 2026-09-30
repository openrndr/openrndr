import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import org.openrndr.kartifex.*
import kotlin.math.abs
import kotlin.test.Test

private fun ringFromPoints(points: List<Vec2>): Ring2 =
    Ring2(points.indices.map { i -> Line2.line(points[i], points[(i + 1) % points.size]) })

class CommonTestClip {

    @Test
    fun testSelfTouchingRingIntersection() {
        // A single ring visiting the same vertex twice: two triangles joined at one exact
        // vertex, analogous to a marching-squares contour threading through a saddle point.
        // Without splitting at that shared vertex, the two triangles get merged into a single
        // self-touching ring instead of being resolved as two separate simple triangles.
        val bowtie = Region2.of(
            ringFromPoints(
                listOf(
                    Vec2(0.0, 0.0),
                    Vec2(10.0, 0.0),
                    Vec2(5.0, 8.0),
                    Vec2(0.0, 0.0),
                    Vec2(-10.0, 0.0),
                    Vec2(-5.0, -8.0)
                )
            )
        )
        val square = Region2.of(
            ringFromPoints(
                listOf(
                    Vec2(-20.0, -20.0),
                    Vec2(20.0, -20.0),
                    Vec2(20.0, 20.0),
                    Vec2(-20.0, 20.0)
                )
            )
        )

        val result = square.intersection(bowtie)
        result.rings.size.shouldBeExactly(2)
        val totalArea = result.rings.sumOf { abs(it.area) }
        totalArea shouldBe (80.0 plusOrMinus 1E-9)
    }

    @Test
    fun testCircleIntersection() {
        val c0 = Ring2.circle().transform(Matrix3.scale(100.0))
        val r0 = Region2.of(c0)
        val r1 = Region2.of(Ring2.circle().transform((Matrix3.translate(40.0, 40.0)).mul(Matrix3.scale(100.0))))

        r0.contains(Vec2(0.0, 0.0)).shouldBeTrue()
        r1.contains(Vec2(0.0, 0.0)).shouldBeTrue()
        r0.intersection(r1).rings.size.shouldBeExactly(1)
    }

    @Test
    fun testCircleDifference0() {
        val c0 = Ring2.circle().transform(Matrix3.scale(100.0))
        val r0 = Region2.of(c0)
        val r1 = Region2.of(Ring2.circle().transform((Matrix3.translate(40.0, 40.0)).mul(Matrix3.scale(100.0))))

        r0.contains(Vec2(0.0, 0.0)).shouldBeTrue()
        r1.contains(Vec2(0.0, 0.0)).shouldBeTrue()
        r0.difference(r1).rings.size.shouldBeExactly(1)
    }

    @Test
    fun testCircleDifference1() {
        val c0 = Ring2.circle().transform(Matrix3.scale(100.0))
        val r0 = Region2.of(c0)
        val r1 = Region2.of(Ring2.circle().transform(Matrix3.scale(50.0)))

        r0.contains(Vec2(0.0, 0.0)).shouldBeTrue()
        r1.contains(Vec2(0.0, 0.0)).shouldBeTrue()
        r0.difference(r1).rings.size.shouldBeExactly(2)
    }

    @Test
    fun testCircleUnion() {
        val c0 = Ring2.circle().transform(Matrix3.scale(100.0))
        val r0 = Region2.of(c0)
        val r1 = Region2.of(Ring2.circle().transform((Matrix3.translate(40.0, 40.0)).mul(Matrix3.scale(100.0))))

        r0.contains(Vec2(0.0, 0.0)).shouldBeTrue()
        r1.contains(Vec2(0.0, 0.0)).shouldBeTrue()
        r0.union(r1).rings.size.shouldBeExactly(1)
    }
}
