import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.ProgramImplementation
import org.openrndr.draw.Session
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.internal.Driver
import org.openrndr.webgl.ApplicationBaseWebGL
import org.openrndr.webgl.ApplicationWebGL
import web.dom.ElementId
import web.dom.document
import web.html.HTMLCanvasElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

private val logger = KotlinLogging.logger {  }
abstract class AbstractApplicationTestFixture {
    lateinit var applicationBase: ApplicationBaseWebGL
    lateinit var application: ApplicationWebGL
    lateinit var program: Program

    @BeforeTest
    open fun setup() {
        logger.info { "Setting up test"}
        program = ProgramImplementation()
        val configuration = Configuration()
        var canvas = document.getElementById(ElementId(configuration.canvasId)) as? HTMLCanvasElement
        if (canvas == null) {
            canvas = document.createElement("canvas") as HTMLCanvasElement
            canvas.id = ElementId(configuration.canvasId)
            document.body.appendChild(canvas)
        }
        // the tests do not use fonts, without a font driver the setup would suspend to load harfbuzzjs
        if (FontDriver.driver == null) {
            FontDriver.driver = object : FontDriver {
                override fun loadFace(fileOrUrl: String) = error("fonts are not loaded in these tests")
            }
        }
        applicationBase = ApplicationBaseWebGL()
        application = applicationBase.build(program, configuration) as ApplicationWebGL
        var error: Throwable? = null
        val setupBlock: suspend () -> Unit = {
            application.setup()
            Driver.instance.enableErrorChecking()
        }
        setupBlock.startCoroutine(object : Continuation<Unit> {
            override val context: CoroutineContext get() = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) {
                result.onFailure { error = it }
            }
        })
        error?.let { throw it }
    }

    @AfterTest
    open fun teardown() {
        logger.info { "Tearing down test" }
        Session.root.end()
        Driver.driver = null
    }
}