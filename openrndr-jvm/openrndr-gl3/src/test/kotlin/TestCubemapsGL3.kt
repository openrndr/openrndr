import org.openrndr.draw.loadCubemap
import org.openrndr.resourceUrl
import kotlin.test.Ignore
import kotlin.test.Test

@Ignore
class TestCubemapsGL3 : AbstractApplicationTestFixture() {
    @Test
    fun `can load a rgb8 dds cubemap`() {
        loadCubemap(resourceUrl("/cubemaps/garage.dds"))
    }

    @Test
    fun `can load a rgb32f dds cubemap`() {
        loadCubemap(resourceUrl("/cubemaps/garage-rgba32f.dds"))
    }
}