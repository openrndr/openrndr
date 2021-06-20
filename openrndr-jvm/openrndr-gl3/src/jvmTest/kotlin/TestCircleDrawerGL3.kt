
import kotlinx.coroutines.runBlocking
import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.internal.gl3.ApplicationGLFWGL3
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestCircleDrawerGL3 : Spek({
    describe("a program") {
        val program = Program()
        val app = ApplicationGLFWGL3(program, Configuration())
        runBlocking { app.setup() }
        app.preloop()

        describe("a circle drawer") {
            program.drawer.circle(Vector2(0.0, 0.0), 40.0)

            program.drawer.circles((0..20000).map {
                Circle(Vector2(Math.random(), Math.random()), Math.random() * 20.0)
            })
            program.drawer.circle(Vector2(0.0, 0.0), 40.0)
        }
    }
})