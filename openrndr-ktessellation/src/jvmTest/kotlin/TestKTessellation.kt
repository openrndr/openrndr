import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.triangulate
import kotlin.test.Test
import kotlin.test.assertEquals

class TestKTessellation {
    @Test
    fun testCircle() {
        val c = Circle(300.0, 300.0, 100.0).shape
        triangulate(c)
    }

    @Test
    fun testRectangle() {
        val c = Rectangle(300.0, 300.0, 100.0, 100.0).shape
        val tris = triangulate(c)
        assertEquals(
            6, tris.size,
            "triangulate() on a rectangle should produce 6 vertices"
        )
    }

    @Test
    fun testWinding() {
        val c = Circle(300.0, 300.0, 100.0).shape
        val tris = triangulate(c)

        val windings = mutableSetOf<Boolean>()
        for (i in tris.indices step 3) {
            val a = tris[i]
            val b = tris[i + 1]
            val c = tris[i + 2]
            windings.add((b.x - a.x) * (c.y - a.y) < (b.y - a.y) * (c.x - a.x))
        }
        assertEquals(
            1, windings.size,
            "triangulate() should produce triangles with only 1 type of winding"
        )
    }

}