import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.longs.shouldBeExactly
import io.lacuna.artifex.*
import io.lacuna.artifex.utils.regions.Split

import org.openrndr.kartifex.utils.regions.*
import org.openrndr.kartifex.Matrix3 as KMatrix3
import org.openrndr.kartifex.Vec2 as KVec2
import org.openrndr.kartifex.Ring2 as KRing2
import org.openrndr.kartifex.Region2 as KRegion2
import org.openrndr.kartifex.Bezier2 as KBezier2

import kotlin.test.Test

class TestClip {
    @Test
    fun testCircleIntersection() {
        val c0 = Ring2.circle().transform(Matrix3.scale(100.0))
        val r0 = Region2.of(c0)
        val r1 = Region2.of(Ring2.circle().transform((Matrix3.translate(40.0, 40.0)).mul(Matrix3.scale(100.0))))

        r0.contains(Vec2(0.0, 0.0)).shouldBeTrue()
        r1.contains(Vec2(0.0, 0.0)).shouldBeTrue()
        r0.intersection(r1).rings.size.shouldBeExactly(1)

        val kc0 = KRing2.circle().transform(KMatrix3.scale(100.0))
        val kr0 = KRegion2.of(kc0)
        val kr1 = KRegion2.of(KRing2.circle().transform((KMatrix3.translate(40.0, 40.0)).mul(KMatrix3.scale(100.0))))

        kr0.contains(KVec2(0.0, 0.0)).shouldBeTrue()
        kr1.contains(KVec2(0.0, 0.0)).shouldBeTrue()
        kr0.intersection(kr1).rings.size.shouldBeExactly(1)
    }

    @Test
    fun testCircleDifference0() {
        val c0 = Ring2.circle().transform(Matrix3.scale(100.0))
        val r0 = Region2.of(c0)
        val r1 = Region2.of(Ring2.circle().transform((Matrix3.translate(40.0, 40.0)).mul(Matrix3.scale(100.0))))

        r0.contains(Vec2(0.0, 0.0)).shouldBeTrue()
        r1.contains(Vec2(0.0, 0.0)).shouldBeTrue()
        r0.difference(r1).rings.size.shouldBeExactly(1)

        val kc0 = KRing2.circle().transform(KMatrix3.scale(100.0))
        val kr0 = KRegion2.of(kc0)
        val kr1 = KRegion2.of(KRing2.circle().transform((KMatrix3.translate(40.0, 40.0)).mul(KMatrix3.scale(100.0))))

        kr0.contains(KVec2(0.0, 0.0)).shouldBeTrue()
        kr1.contains(KVec2(0.0, 0.0)).shouldBeTrue()
        kr0.difference(kr1).rings.size.shouldBeExactly(1)
    }

    @Test
    fun testCircleDifference1() {
        val c0 = Ring2.circle().transform(Matrix3.scale(100.0))
        val r0 = Region2.of(c0)
        val r1 = Region2.of(Ring2.circle().transform(Matrix3.scale(50.0)))

        r0.contains(Vec2(0.0, 0.0)).shouldBeTrue()
        r1.contains(Vec2(0.0, 0.0)).shouldBeTrue()
        r0.difference(r1).rings.size.shouldBeExactly(2)

        val kc0 = KRing2.circle().transform(KMatrix3.scale(100.0))
        val kr0 = KRegion2.of(kc0)
        val kr1 = KRegion2.of(KRing2.circle().transform(KMatrix3.scale(50.0)))

        kr0.contains(KVec2(0.0, 0.0)).shouldBeTrue()
        kr1.contains(KVec2(0.0, 0.0)).shouldBeTrue()
        kr0.difference(kr1).rings.size.shouldBeExactly(2)
    }

    @Test
    fun testCircleUnion() {
        val c0 = Ring2.circle().transform(Matrix3.scale(100.0))
        val r0 = Region2.of(c0)
        val r1 = Region2.of(Ring2.circle().transform((Matrix3.translate(40.0, 40.0)).mul(Matrix3.scale(100.0))))

        r0.contains(Vec2(0.0, 0.0)).shouldBeTrue()
        r1.contains(Vec2(0.0, 0.0)).shouldBeTrue()
        r0.union(r1).rings.size.shouldBeExactly(1)

        val kc0 = KRing2.circle().transform(KMatrix3.scale(100.0))
        val kr0 = KRegion2.of(kc0)
        val kr1 = KRegion2.of(KRing2.circle().transform((KMatrix3.translate(40.0, 40.0)).mul(KMatrix3.scale(100.0))))

        kr0.contains(KVec2(0.0, 0.0)).shouldBeTrue()
        kr1.contains(KVec2(0.0, 0.0)).shouldBeTrue()
        kr0.union(kr1).rings.size.shouldBeExactly(1)
    }

    @Test
    fun testSuspectCase0() {

        val c0 = Region2.of(Ring2.of(
            Bezier2.curve(Vec2(0.0, 0.0), Vec2(100.0, 0.0)),
            Bezier2.curve(Vec2(100.0, 0.0), Vec2(100.0, 100.0)),
            Bezier2.curve(Vec2(100.0, 100.0), Vec2(0.0, 100.0)),
            Bezier2.curve(Vec2(0.0, 100.0), Vec2(0.0, 0.0)))
        )

        val c1 = Region2.of(Ring2.of(
            Bezier2.curve(Vec2(0.0, 0.0), Vec2(100.0, 0.0)),
            Bezier2.curve(Vec2(100.0, 0.0), Vec2(100.0, 100.0)),
            Bezier2.curve(Vec2(100.0, 100.0), Vec2(0.0, 100.0)),
            Bezier2.curve(Vec2(0.0, 100.0), Vec2(-50.0, 50.0), Vec2(0.0, 0.0))
        ).transform(Matrix3.translate(110.0, 0.0)))

        val split = Split.split(c0, c1)
        split.splits.size().shouldBeExactly(2)

        val kc0 = KRegion2.of(KRing2.of(
            KBezier2.curve(KVec2(0.0, 0.0), KVec2(100.0, 0.0)),
            KBezier2.curve(KVec2(100.0, 0.0), KVec2(100.0, 100.0)),
            KBezier2.curve(KVec2(100.0, 100.0), KVec2(0.0, 100.0)),
            KBezier2.curve(KVec2(0.0, 100.0), KVec2(0.0, 0.0)))
        )

        val kc1 = KRegion2.of(KRing2.of(
            KBezier2.curve(KVec2(0.0, 0.0), KVec2(100.0, 0.0)),
            KBezier2.curve(KVec2(100.0, 0.0), KVec2(100.0, 100.0)),
            KBezier2.curve(KVec2(100.0, 100.0), KVec2(0.0, 100.0)),
            KBezier2.curve(KVec2(0.0, 100.0), KVec2(-50.0, 50.0), KVec2(0.0, 0.0))
        ).transform(KMatrix3.translate(110.0, 0.0)))

        val ksplit = split(kc0, kc1)
        ksplit.splits.size.shouldBeExactly(2)
    }
}