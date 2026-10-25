import org.openrndr.draw.font.Face
import org.openrndr.draw.font.internal.CachingFontDriver
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.fontdriver.stb.FontDriverStbTt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestCachingFontDriver {

    @Test
    fun testLoadsOnceAndFacesAreIndependentlyClosable() {
        val stb = FontDriverStbTt()
        var loads = 0
        val counting = object : FontDriver {
            override fun loadFace(fileOrUrl: String): Face {
                loads++
                return stb.loadFace(fileOrUrl)
            }
        }
        val driver = CachingFontDriver(counting)
        val path = "../../data/fonts/Platypi-Regular.ttf"

        val a = driver.loadFace(path)
        val b = driver.loadFace(path)
        assertEquals(1, loads)
        assertEquals(FontDriver.DEFAULT_SIZE_IN_POINTS, b.sizeInPoints)

        a.close()
        assertTrue(b.withSize(32.0).height > 0.0)

        val c = driver.loadFace(path)
        assertEquals(1, loads)
        assertTrue(c.height > 0.0)

        b.close()
        c.close()
        driver.clear()
    }
}
