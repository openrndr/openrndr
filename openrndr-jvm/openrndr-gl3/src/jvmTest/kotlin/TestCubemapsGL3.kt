import kotlinx.coroutines.runBlocking
import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.internal.gl3.ApplicationGLFWGL3
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestCubemapsGL3 : Spek({
    describe("a program") {
        val p = Program()
        val app = ApplicationGLFWGL3(p, Configuration())
        runBlocking { app.setup() }
        app.preloop()

        it("can load a rgb8 dds cubemap") {
            //CubemapGL3.fromUrl(resourceUrl("/cubemaps/garage.dds"))
        }

        it("can load a rgb32f dds cubemap") {
            //CubemapGL3.fromUrl(resourceUrl("/cubemaps/garage-rgba32f.dds"))
        }
    }

})