import kotlinx.coroutines.runBlocking
import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.ProgramImplementation
import org.openrndr.draw.Session
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.ApplicationBaseGLFWGL3
import org.openrndr.internal.gl3.ApplicationGLFWGL3
import kotlin.test.*

abstract class AbstractApplicationTestFixture {
    lateinit var applicationBase: ApplicationBaseGLFWGL3
    lateinit var application: ApplicationGLFWGL3
    lateinit var program: Program

    @BeforeTest
    open fun setup() {
        program = ProgramImplementation()
        applicationBase = ApplicationBaseGLFWGL3()
        application = applicationBase.build(program, Configuration()) as ApplicationGLFWGL3
        runBlocking { application.setup() }
        application.preloop()
    }

    @AfterTest
    open fun teardown() {
        application.exit()
        val ci = Driver.instance.contextID
        Driver.instance.destroyContext(ci)
        Session.root.end()
    }
}