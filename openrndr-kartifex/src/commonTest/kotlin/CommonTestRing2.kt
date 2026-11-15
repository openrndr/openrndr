import io.kotest.matchers.shouldBe
import org.openrndr.kartifex.*
import org.openrndr.kartifex.utils.solveQuadratic
import kotlin.test.Test

class CommonTestRing2 {

    @Test
    fun solveQuadraticWithNegativeCoefficientsAndATinyQuadraticTerm() {
        // the factor that normalizes the coefficients has to follow their magnitudes, not their signs
        val roots = solveQuadratic(3.8390986270008325E-13, 238.18752639488488, -2.928167060267697)
        roots.size shouldBe 1
        (kotlin.math.abs(roots[0] - 2.928167060267697 / 238.18752639488488) < 1e-12) shouldBe true
    }

    @Test
    fun testRingWithAQuadraticSegment() {
        // the rounded tip of a star, the horizontal rays from points inside it cross its quadratic once
        val start = Vec2(444.6352549156242, 231.1832212156129)
        val control = Vec2(470.0, 239.99999999999997)
        val end = Vec2(444.6352549156242, 248.81677878438708)
        val curve = Bezier2.curve(start, control, end)
        val ring = Ring2(listOf(curve, Line2.line(end, start)))

        ring.test(Vec2(444.81, 231.4)).inside shouldBe true
        ring.test(Vec2(444.9, 231.33)).inside shouldBe true
        ring.test(Vec2(450.0, 240.0)).inside shouldBe true
        ring.test(Vec2(460.0, 232.0)).inside shouldBe false

        // compare with an even-odd test on a fine polygon, leaving out points close to the outline. Coarse enough to run
        // within the time limit of the browser tests on CI
        val polygon = (0..500).map { curve.position(it / 500.0) } + start
        var wrong = 0
        for (i in 0..30) {
            for (j in 0..30) {
                val p = Vec2(444.0 + 27.0 * i / 30, 230.0 + 20.0 * j / 30)
                var inside = false
                var near = false
                for (k in 0 until polygon.size - 1) {
                    val a = polygon[k]
                    val ab = polygon[k + 1].sub(a)
                    val t = (Vec.dot(p.sub(a), ab) / Vec.dot(ab, ab)).coerceIn(0.0, 1.0)
                    if (a.add(ab.mul(t)).sub(p).length() < 0.05) near = true
                    val b = polygon[k + 1]
                    if ((a.y > p.y) != (b.y > p.y) && p.x < a.x + (p.y - a.y) / (b.y - a.y) * (b.x - a.x)) {
                        inside = !inside
                    }
                }
                if (!near && ring.test(p).inside != inside) wrong++
            }
        }
        wrong shouldBe 0
    }
}
