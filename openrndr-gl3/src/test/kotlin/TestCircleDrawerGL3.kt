
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.lwjgl.BufferUtils
import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.VertexElementType
import org.openrndr.draw.vertexFormat
import org.openrndr.internal.gl3.ApplicationGLFWGL3
import org.openrndr.internal.gl3.VertexBufferGL3
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import java.nio.ByteBuffer

object TestCircleDrawerGL3 : Spek({

    describe("a program") {
        val program = Program()
        val app = ApplicationGLFWGL3(program, Configuration())
        app.setup()
        app.preloop()

        describe("a circle drawer") {

            program.drawer.circle(Vector2(0.0, 0.0), 40.0)

            program.drawer.circles((0..20000).map {
                Circle(Vector2(Math.random(), Math.random()), Math.random()*20.0)
            })


            program.drawer.circle(Vector2(0.0, 0.0), 40.0)
        }

    }


})