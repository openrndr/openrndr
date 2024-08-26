import org.junit.jupiter.api.Assertions.assertTrue
import org.openrndr.internal.gl3.DriverVersionGL
import kotlin.test.Test
import kotlin.test.assertFalse

class TestDriverVersion {
    /**
     * Test DriverVersionGL comparison
     */
    @Test
    fun testIsAtLeast() {
        assertTrue(DriverVersionGL.GL_VERSION_4_1.isAtLeast(DriverVersionGL.GL_VERSION_4_1))
        assertTrue(DriverVersionGL.GL_VERSION_4_2.isAtLeast(DriverVersionGL.GL_VERSION_4_1))
        assertTrue(DriverVersionGL.GL_VERSION_4_2.isAtLeast(DriverVersionGL.GL_VERSION_4_1, DriverVersionGL.GLES_VERSION_3_1))
        assertFalse(DriverVersionGL.GL_VERSION_4_2.isAtLeast(DriverVersionGL.GLES_VERSION_3_1))
    }
}