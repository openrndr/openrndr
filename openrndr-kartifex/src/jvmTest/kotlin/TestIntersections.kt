import io.kotest.matchers.ints.shouldBeExactly
import io.lacuna.artifex.Bezier2
import io.lacuna.artifex.Line2
import io.lacuna.artifex.Vec2
import kotlin.test.Test
import org.openrndr.kartifex.Bezier2 as KBezier2
import org.openrndr.kartifex.Line2 as KLine2
import org.openrndr.kartifex.Vec2 as KVec2

class TestIntersections {

    @Test
    fun testCurveCurveIntersection() {
        val c0 = Bezier2.curve(Vec2(0.0, 0.0), Vec2(100.0, 0.0),  Vec2(0.0, 100.0), Vec2(100.0, 100.0))
        val c1 = Bezier2.curve(Vec2(100.0, 0.0), Vec2(0.0, 0.0),  Vec2(100.0, 100.0), Vec2(0.0, 0.0))

        val ints = c0.intersections(c1)

        ints.size.shouldBeExactly(2)
        println(ints[0])
        println(ints[1])

        val kc0 = KBezier2.curve(KVec2(0.0, 0.0), KVec2(100.0, 0.0),  KVec2(0.0, 100.0), KVec2(100.0, 100.0))
        val kc1 = KBezier2.curve(KVec2(100.0, 0.0), KVec2(0.0, 0.0),  KVec2(100.0, 100.0), KVec2(0.0, 0.0))

        val kints = kc0.intersections(kc1)
        kints.size.shouldBeExactly(2)
    }

    @Test
    fun testKnownProblematicCase0() {
        // This tests https://github.com/lacuna/artifex/issues/4
        val s0 = Vec2(435.3194971681168, 905.231313655894)
        val a0 = Vec2(436.60194226635423, 904.5228510213844)
        val b0 = Vec2(438.3935801257989, 904.1857845636355)
        val e0 = Vec2(440.3919343740061, 904.1270373708342)

        val s1 = Vec2(435.198246034818, 896.3191997888123)
        val a1 = Vec2(436.84762896823827, 898.9951604687135)
        val b1 = Vec2(438.2241643938649, 902.5923131816958)
        val e1 = Vec2(439.1164972433261, 906.1884150468093)

        val curve0 = Bezier2.curve(s0, a0, b0, e0)
        val curve1 = Bezier2.curve(s1, a1, b1, e1)

        // Artifex does not detect the intersection
        val ints00 = curve0.intersections(curve1)
        ints00.size.shouldBeExactly(0)

        val ints10 = curve0.reverse().intersections(curve1)
        ints10.size.shouldBeExactly(0)

        val ints11 = curve0.reverse().intersections(curve1.reverse())
        ints11.size.shouldBeExactly(0)

        val ints01 = curve0.intersections(curve1.reverse())
        ints01.size.shouldBeExactly(0)

        //

        val ks0 = KVec2(435.3194971681168, 905.231313655894)
        val ka0 = KVec2(436.60194226635423, 904.5228510213844)
        val kb0 = KVec2(438.3935801257989, 904.1857845636355)
        val ke0 = KVec2(440.3919343740061, 904.1270373708342)

        val ks1 = KVec2(435.198246034818, 896.3191997888123)
        val ka1 = KVec2(436.84762896823827, 898.9951604687135)
        val kb1 = KVec2(438.2241643938649, 902.5923131816958)
        val ke1 = KVec2(439.1164972433261, 906.1884150468093)

        val kcurve0 = KBezier2.curve(ks0, ka0, kb0, ke0)
        val kcurve1 = KBezier2.curve(ks1, ka1, kb1, ke1)

        // KArtifex also does not detect the intersection
        val kints00 = kcurve0.intersections(kcurve1)
        kints00.size.shouldBeExactly(1)
    }

    @Test
    fun testProblematicCase1() {
        val l = Line2.line(Vec2(512.0, 96.0), Vec2(128.0, 384.0))
        val c = Bezier2.curve(Vec2(128.0, 384.0), Vec2(320.0, 168.0), Vec2(512.0, 384.0))
        val i = l.intersections(c)
        // Artifex misses this intersection
        i.size.shouldBeExactly(0)

        val kl = KLine2.line(KVec2(512.0, 96.0), KVec2(128.0, 384.0))
        val kc = KBezier2.curve(KVec2(128.0, 384.0), KVec2(320.0, 168.0), KVec2(512.0, 384.0))
        val ki = kl.intersections(kc)
        // Kartifex finds the intersections
        ki.size.shouldBeExactly(2)
    }

}