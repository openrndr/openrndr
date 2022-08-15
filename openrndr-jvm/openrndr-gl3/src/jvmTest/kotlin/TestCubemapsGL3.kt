import org.openrndr.Program
import kotlin.test.*

class TestCubemapsGL3 {
    val program = Program().initializeGLFWGL3Application()

    @Test
    fun `can load a rgb8 dds cubemap`() {
        //CubemapGL3.fromUrl(resourceUrl("/cubemaps/garage.dds"))
    }

    @Test
    fun `can load a rgb32f dds cubemap`() {
        //CubemapGL3.fromUrl(resourceUrl("/cubemaps/garage-rgba32f.dds"))
    }
}