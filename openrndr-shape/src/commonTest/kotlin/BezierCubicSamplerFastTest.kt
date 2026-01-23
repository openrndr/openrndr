import org.openrndr.math.Vector3
import org.openrndr.shape.Segment3D
import org.openrndr.shape.internal.cubicError
import org.openrndr.shape.internal.flattenCubic
import org.openrndr.shape.internal.numQuadratics
import org.openrndr.shape.internal.quadBez3dTo2d
import org.openrndr.shape.internal.toQuadratics
import kotlin.test.Test
import kotlin.test.assertEquals

class BezierCubicSamplerFastTest {
    @Test
    fun testCubicToQuadraticCount() {
        val s = Segment3D(Vector3.ZERO, Vector3.ONE * 100.0,  Vector3.ONE * 105.0, Vector3(0.0, 1.0, 1.0) * 100.0)
        val quads = s.numQuadratics(0.1)
        println(quads)
    }

    @Test
    fun testCubicToQuadratics() {
        val s = Segment3D(Vector3.ZERO, Vector3.ONE * 100.0,  Vector3.ONE * 105.0, Vector3(0.0, 1.0, 1.0) * 100.0)
        println(s.cubicError())
        s.toQuadratics(0.01).map {
            println(it.cubicError())
        }
    }

    @Test
    fun testCubicFlatten() {
        val s = Segment3D(Vector3.ZERO, Vector3.ONE * 100.0,  Vector3.ONE * 105.0, Vector3(0.0, 1.0, 1.0) * 100.0)
        val flattened = flattenCubic(s, 0.1, 0.1)
        println(flattened)
    }

}