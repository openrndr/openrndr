import org.openrndr.ktessellation.triangulator.triangulate
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals

class TestKTessellation {
    @Test
    fun testCircle() {
        val c = Circle(300.0, 300.0, 100.0).shape
        val tris = triangulate(c)
    }

    @Test
    fun testRectangle() {
        val c = Rectangle(300.0, 300.0, 100.0, 100.0).shape
        val tris = triangulate(c)
        assertEquals(6, tris.size)
    }

}