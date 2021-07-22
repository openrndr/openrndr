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
}