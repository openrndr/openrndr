import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import org.openrndr.shape.internal.BezierCubicSamplerT

import kotlin.test.Test

class BezierCubicSampler2DTTest {

    @Test
    fun bla() {
        val bcs2dt = BezierCubicSamplerT<Vector2>()
        val pts = bcs2dt.sample(Vector2(0.0, 0.0), Vector2(25.0, 25.0), Vector2(75.0, -25.0), Vector2(100.0, 0.0))
        val s = Segment2D(Vector2(0.0, 0.0), Vector2(25.0, 25.0), Vector2(75.0, -25.0), Vector2(100.0, 0.0))
        for (p in pts) {
            println("${p.second}: ${p.first} ${s.position(p.second)} ${s.nearest(p.first).segmentT} ${s.nearest(p.first).position}")
        }
    }
}