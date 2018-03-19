import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.lwjgl.BufferUtils
import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.draw.*
import org.openrndr.internal.gl3.ApplicationGL3
import org.openrndr.internal.gl3.CubemapGL3
import org.openrndr.internal.gl3.VertexBufferGL3
import org.openrndr.math.Vector2
import org.openrndr.resourceUrl
import java.nio.ByteBuffer

object TestCubemapsGL3 : Spek({
    describe("a program") {
        val p = Program()
        val app = ApplicationGL3(p, Configuration())
        app.setup()
        app.preloop()

        it("can load a rgb8 dds cubemap") {
            CubemapGL3.fromUrl(resourceUrl("/cubemaps/garage.dds"))
        }

        it("can load a rgb32f dds cubemap") {
            CubemapGL3.fromUrl(resourceUrl("/cubemaps/garage-rgba32f.dds"))
        }
    }

})