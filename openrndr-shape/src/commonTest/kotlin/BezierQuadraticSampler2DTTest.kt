import org.openrndr.math.Vector2
import org.openrndr.shape.Segment
import org.openrndr.shape.internal.BezierQuadraticSamplerT
import kotlin.test.Test

class BezierQuadraticSampler2DTTest {

    @Test
    fun bla() {
        val bqs2dt = BezierQuadraticSamplerT<Vector2>()
        val pts = bqs2dt.sample(Vector2(0.0, 0.0), Vector2(50.0, 50.0), Vector2(100.0, 0.0))
        val s = Segment(Vector2(0.0, 0.0), Vector2(50.0, 50.0), Vector2(100.0, 0.0))
        for (p in pts) {
            println("${p.second}: ${p.first} ${s.position(p.second)}")
        }
    }
}