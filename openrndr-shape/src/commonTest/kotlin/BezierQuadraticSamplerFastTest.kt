import org.openrndr.math.Vector3
import org.openrndr.shape.Segment3D
import org.openrndr.shape.internal.quadBez3dTo2d
import kotlin.test.Test
import kotlin.test.assertEquals

class BezierQuadraticSamplerFastTest {
    @Test
    fun testBezier3Dto2D() {
        val s = Segment3D(Vector3.ZERO, Vector3.ONE * 100.0, Vector3(0.0, 1.0, 1.0) * 100.0)
        val s2d = quadBez3dTo2d(s)
        assertEquals(s.length, s2d.length, 1E-8)
    }
}