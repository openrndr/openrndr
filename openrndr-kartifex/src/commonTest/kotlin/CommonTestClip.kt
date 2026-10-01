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

    private fun bowtie(): Region2 = Region2.of(
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

    private fun crossedEdges(): Region2 = Region2.of(
        ringFromPoints(
            listOf(
                Vec2(0.0, 0.0),
                Vec2(10.0, 10.0),
                Vec2(10.0, 0.0),
                Vec2(0.0, 10.0)
            )
        )
    )

    @Test
    fun testRemoveSelfIntersectionsTouchingVertexEvenOdd() {
        // Same bowtie as testSelfTouchingRingIntersection, but resolved on its own via
        // removeSelfIntersections() rather than by intersecting it with another region. The two
        // triangles don't overlap in area, so even-odd and nonzero agree here.
        val result = bowtie().removeSelfIntersections(FillRule.EVEN_ODD)
        result.rings.size.shouldBeExactly(2)
        val totalArea = result.rings.sumOf { abs(it.area) }
        totalArea shouldBe (80.0 plusOrMinus 1E-9)
    }

    @Test
    fun testRemoveSelfIntersectionsTouchingVertexNonZero() {
        val result = bowtie().removeSelfIntersections(FillRule.NON_ZERO)
        result.rings.size.shouldBeExactly(2)
        val totalArea = result.rings.sumOf { abs(it.area) }
        totalArea shouldBe (80.0 plusOrMinus 1E-9)
    }

    @Test
    fun testRemoveSelfIntersectionsCrossingEdgesEvenOdd() {
        // A single ring shaped like a bowtie, but where the two triangles are joined by edges
        // that actually cross at an interior point rather than sharing an exact vertex.
        val result = crossedEdges().removeSelfIntersections(FillRule.EVEN_ODD)
        result.rings.size.shouldBeExactly(2)
        val totalArea = result.rings.sumOf { abs(it.area) }
        totalArea shouldBe (50.0 plusOrMinus 1E-9)
    }

    @Test
    fun testRemoveSelfIntersectionsCrossingEdgesNonZero() {
        val result = crossedEdges().removeSelfIntersections(FillRule.NON_ZERO)
        result.rings.size.shouldBeExactly(2)
        val totalArea = result.rings.sumOf { abs(it.area) }
        totalArea shouldBe (50.0 plusOrMinus 1E-9)
    }

    @Test
    fun testRemoveSelfIntersectionsNoOp() {
        val c0 = Ring2.circle().transform(Matrix3.scale(100.0))
        val r0 = Region2.of(c0)

        for (fill in FillRule.entries) {
            val result = r0.removeSelfIntersections(fill)
            result.rings.size.shouldBeExactly(1)
            abs(result.rings[0].area) shouldBe (abs(c0.area) plusOrMinus 1E-9)
        }
    }

    @Test
    fun testRemoveSelfIntersectionsNonZeroNestedSameWinding() {
        // Two concentric, same-direction (both CCW) circles bundled as a single region. Under
        // the even-odd rule this is ambiguous (Region2.test's "smallest ring wins" convention
        // means neither ring's own boundary ever tests as a genuine interior point of the
        // other), but under the nonzero rule the inner circle never flips the sign of the
        // winding number -- both of its sides stay nonzero -- so it contributes nothing to the
        // boundary and the result collapses to just the outer disk.
        val outer = Ring2.circle().transform(Matrix3.scale(100.0))
        val inner = Ring2.circle().transform(Matrix3.scale(50.0))
        val r = Region2(listOf(outer, inner))

        val result = r.removeSelfIntersections(FillRule.NON_ZERO)
        result.rings.size.shouldBeExactly(1)
        abs(result.rings[0].area) shouldBe (abs(outer.area) plusOrMinus 1E-6)
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
