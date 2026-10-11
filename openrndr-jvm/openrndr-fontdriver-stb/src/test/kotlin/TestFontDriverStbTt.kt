import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.loadFontImageMap
import org.openrndr.fontdriver.stb.FontDriverStbTt
import kotlin.test.Test
import kotlin.test.assertTrue

class TestFontDriverStbTt: AbstractApplicationTestFixture() {

    @Test
    fun testLoadFace() {
        val face = loadFace("../../data/fonts/Platypi-Regular.ttf").withSize(32.0)
        assertTrue(face.height > 0.0)
    }

    @Test
    fun testLoadFontMap() {
        FontDriver.driver = FontDriverStbTt()

        val face = loadFace("../../data/fonts/Platypi-Regular.ttf").withSize(32.0)
        val font = loadFontImageMap("../../data/fonts/Platypi-Regular.ttf", 32.0, contentScale = 2.0)
        assertTrue(font.height > 0.0)

        println(face.height)
        println(font.height)
    }
}