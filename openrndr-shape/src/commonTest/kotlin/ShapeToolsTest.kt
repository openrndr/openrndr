import org.openrndr.math.Vector3
import org.openrndr.shape.sampleEquidistant
import kotlin.test.Test
import kotlin.test.assertEquals

class ShapeToolsTest {

    @Test
    fun shouldReturnExpectedNumberOfSamples() {
        val points = listOf(
            Vector3(0.0, 0.0, 0.0),
            Vector3(100.0, 100.0, 50.0),
            Vector3(0.0, 200.0, 0.0)
        )
        val points2 = sampleEquidistant(points, 13)
        assertEquals(points2.size, 13)

        val points3 = sampleEquidistant(points, 14)
        assertEquals(points3.size, 14)
    }
}
