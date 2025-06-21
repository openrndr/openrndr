import kotlinx.coroutines.runBlocking
import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.ProgramImplementation
import org.openrndr.internal.glfw.ApplicationBaseGLFW
import org.openrndr.internal.glfw.ApplicationGLFW
import kotlin.test.*

abstract class AbstractApplicationTestFixture {
    lateinit var applicationBase: ApplicationBaseGLFW
    lateinit var application: ApplicationGLFW
    lateinit var program: Program

    @BeforeTest
    open fun setup() {
        //System.setProperty("org.openrndr.gl3.debug",  "true")
        program = ProgramImplementation()
        applicationBase = ApplicationBaseGLFW()
        application = applicationBase.build(program, Configuration()) as ApplicationGLFW
        runBlocking { application.setup() }
        application.preloop()

    }

    @AfterTest
    open fun teardown() {
        application.postLoop()

//        val ci = Driver.instance.contextID
//        Driver.instance.destroyContext(ci)
//        Session.root.end()

    }
}