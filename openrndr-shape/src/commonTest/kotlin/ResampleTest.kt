import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.proportionalize
import org.openrndr.shape.resample
import kotlin.test.Test
import kotlin.test.assertEquals

class ResampleTest {
    @Test
    fun testResample2() {
        val v1 = listOf(Vector2(0.0, 0.0) to 0.0, Vector2(100.0, 0.0) to 1.0)
        val v2 = listOf(Vector3(0.0, 0.0, 0.0) to 0.0, Vector3(100.0, 0.0, 0.0) to 0.2, Vector3(0.0, 100.0, 0.0) to 1.0)

        val resampled = resample(v1, v2)
        assertEquals(resampled.size, 3)

        assertEquals(0.0, resampled[0].third)
        assertEquals(0.2, resampled[1].third)
        assertEquals(20.0, resampled[1].first.x)
        assertEquals(1.0, resampled[2].third)
    }

    @Test
    fun testResample2WithNoisyInput() {
        val v1 = listOf(Vector2(0.0, 0.0) to 0.0, Vector2(100.0, 0.0) to 1.0)
        val v2 = listOf(Vector3(0.0, 0.0, 0.0) to 0.01, Vector3(100.0, 0.0, 0.0) to 0.2, Vector3(0.0, 100.0, 0.0) to 0.99)

        val resampled = resample(v1, v2)
        assertEquals(resampled.size, 5)
        assertEquals(0.0, resampled[0].third)
        assertEquals(1.0, resampled[4].third)
        assertEquals(Vector3(0.0, 0.0, 0.0), resampled[0].second)
        assertEquals(Vector3(0.0, 100.0, 0.0), resampled[4].second)
    }

    @Test
    fun testProportionalize() {
        val v1 = listOf(Vector2(0.0, 0.0) to 0.0, Vector2(20.0, 0.0) to 0.5, Vector2(100.0, 0.0) to 1.0)
        val p1 = proportionalize(v1)
        assertEquals(3, p1.size)
        assertEquals(0.0, p1[0].second)
        assertEquals(0.2, p1[1].second)
        assertEquals(1.0, p1[2].second)
    }
}