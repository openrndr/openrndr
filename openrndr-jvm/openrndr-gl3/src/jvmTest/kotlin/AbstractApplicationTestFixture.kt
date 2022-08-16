import kotlinx.coroutines.runBlocking
import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.internal.gl3.ApplicationGLFWGL3
import kotlin.test.*

abstract class AbstractApplicationTestFixture {
    lateinit var application: ApplicationGLFWGL3
    lateinit var program: Program

    @BeforeTest
    open fun setup() {
        program = Program()
        application = ApplicationGLFWGL3(program, Configuration())
        runBlocking { application.setup() }
        application.preloop()
    }

    @AfterTest
    open fun teardown() {
        application.exit()
    }
}