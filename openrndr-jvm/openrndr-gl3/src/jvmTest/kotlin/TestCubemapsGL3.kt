import org.openrndr.internal.gl3.CubemapGL3
import org.openrndr.resourceUrl
import kotlin.test.*

@Ignore
class TestCubemapsGL3 : AbstractApplicationTestFixture() {
    @Test
    fun `can load a rgb8 dds cubemap`() {
        CubemapGL3.fromUrl(resourceUrl("/cubemaps/garage.dds"), null, null)
    }

    @Test
    fun `can load a rgb32f dds cubemap`() {
        CubemapGL3.fromUrl(resourceUrl("/cubemaps/garage-rgba32f.dds"), null, null)
    }
}