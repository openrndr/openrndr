import kotlinx.coroutines.runBlocking
import org.openrndr.Application
import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.ProgramImplementation
import org.openrndr.internal.gl3.ApplicationBaseEGLGL3
import org.openrndr.internal.gl3.ApplicationBaseGLFWGL3
import org.openrndr.internal.gl3.ApplicationEGLGL3
import org.openrndr.internal.gl3.ApplicationGLFWGL3
import kotlin.test.*

/**
 * Runs each test in a new application. The backend is GLFW, or headless EGL with
 * `-Dorg.openrndr.test.backend=egl` (`./gradlew -Popenrndr.test.backend=egl`).
 */
abstract class AbstractApplicationTestFixture {
    lateinit var application: Application
    lateinit var program: Program

    private val egl = System.getProperty("org.openrndr.test.backend") == "egl"

    @BeforeTest
    open fun setup() {
        //System.setProperty("org.openrndr.gl3.debug",  "true")
        program = ProgramImplementation()
        if (egl) {
            application = ApplicationBaseEGLGL3().build(program, Configuration())
            runBlocking { application.setup() }
        } else {
            val application = ApplicationBaseGLFWGL3().build(program, Configuration()) as ApplicationGLFWGL3
            this.application = application
            runBlocking { application.setup() }
            application.preloop()
        }
    }

    @AfterTest
    open fun teardown() {
        when (val application = application) {
            is ApplicationEGLGL3 -> application.postLoop()
            is ApplicationGLFWGL3 -> application.postLoop()
        }
    }
}
