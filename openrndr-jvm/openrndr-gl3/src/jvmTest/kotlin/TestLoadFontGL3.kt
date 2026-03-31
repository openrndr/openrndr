import org.openrndr.draw.loadFont
import org.openrndr.resourceUrl
import kotlin.test.Test


class TestLoadFontGL3 : AbstractApplicationTestFixture() {
    @Test
    fun `load font from resources`() {
        val font = program.loadFont(resourceUrl("/fonts/Roboto-Medium.ttf"), 16.0)
        program.drawer.fontMap = font
        program.drawer.text("this is a test", 0.0, 0.0)
    }

    @Test
    fun `load font with spaces from resources`() {
        val font = program.loadFont(resourceUrl("/fonts/Roboto Medium.ttf"), 16.0)
        program.drawer.fontMap = font
        program.drawer.text("this is a test", 0.0, 0.0)
    }

    @Test
    fun `load font from String path`() {
        val font = program.loadFont("src/jvmTest/resources/fonts/Roboto-Medium.ttf", 16.0)
        program.drawer.fontMap = font
        program.drawer.text("this is a test", 0.0, 0.0)
    }

    @Test
    fun `load font with spaces from String path`() {
        val font = program.loadFont("src/jvmTest/resources/fonts/Roboto Medium.ttf", 16.0)
        program.drawer.fontMap = font
        program.drawer.text("this is a test", 0.0, 0.0)
    }

    @Test
    fun `load font from https`() {
        val font = program.loadFont("https://github.com/openrndr/openrndr/raw/refs/heads/master/openrndr-jvm/openrndr-gl3/src/jvmTest/resources/fonts/Roboto-Medium.ttf", 16.0)
        program.drawer.fontMap = font
        program.drawer.text("this is a test", 0.0, 0.0)
    }

    @Test
    fun `load font with spaces from https`() {
        val font = program.loadFont("https://github.com/openrndr/openrndr/raw/refs/heads/master/openrndr-jvm/openrndr-gl3/src/jvmTest/resources/fonts/Roboto Medium.ttf", 16.0)
        program.drawer.fontMap = font
        program.drawer.text("this is a test", 0.0, 0.0)
    }
}