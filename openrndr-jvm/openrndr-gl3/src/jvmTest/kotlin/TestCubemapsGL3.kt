import org.openrndr.Program
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestCubemapsGL3 : Spek({
    describe("a program") {
        val program = Program().initializeGLFWGL3Application()

        it("can load a rgb8 dds cubemap") {
            //CubemapGL3.fromUrl(resourceUrl("/cubemaps/garage.dds"))
        }

        it("can load a rgb32f dds cubemap") {
            //CubemapGL3.fromUrl(resourceUrl("/cubemaps/garage-rgba32f.dds"))
        }
    }
})