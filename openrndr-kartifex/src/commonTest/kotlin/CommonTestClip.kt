import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeExactly
import org.openrndr.kartifex.Matrix3
import org.openrndr.kartifex.Region2
import org.openrndr.kartifex.Ring2
import org.openrndr.kartifex.Vec2
import kotlin.test.Test

class CommonTestClip {

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

    @Test
    fun testProblemBezierIntersection() {
        val b0 = Bezier2.CubicBezier2(
            Vec2(2461.964649197695, -952.1441769362448),
            Vec2(2461.1818602722083, -954.044949655754),
            Vec2(2460.787918089939, -956.0729587762241),
            Vec2(2460.787918089939, -958.1039726407873)
        )
        val b1 = Bezier2.CubicBezier2(
            Vec2(2461.9546471841327, -952.1441720561323),
            Vec2(2461.9546471841327, -952.1481051678613),
            Vec2(2461.95698225021, -952.1518351983657),
            Vec2(2461.960844919649, -952.1534250425353)
        )
        // Fixed with FAT_LINE_MIN_LENGTH in Intersection.kt
        b0.intersections(b1).size.shouldBeExactly(1)
    }
}
