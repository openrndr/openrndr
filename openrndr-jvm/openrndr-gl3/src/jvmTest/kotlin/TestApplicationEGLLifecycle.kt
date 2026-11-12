import kotlinx.coroutines.runBlocking
import org.openrndr.Configuration
import org.openrndr.ProgramImplementation
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.internal.gl3.ApplicationBaseEGLGL3
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Runs headless EGL applications through their loop, with `-Popenrndr.test.backend=egl`
 */
class TestApplicationEGLLifecycle {
    /** runs an application that downloads a float color buffer on its first frame and exits */
    private fun runApplication(): Boolean {
        var downloaded = false
        val program = object : ProgramImplementation() {
            override suspend fun setup() {
                extend {
                    val buffer = colorBuffer(16, 16, type = ColorType.FLOAT32)
                    buffer.shadow.download()
                    buffer.destroy()
                    downloaded = true
                    application.exit()
                }
            }
        }
        val application = ApplicationBaseEGLGL3().build(program, Configuration().apply { width = 64; height = 64 })
        runBlocking { application.setup() }
        application.loop()
        return downloaded
    }

    @Test
    fun `applications exit from their loop and the next application in the JVM works`() {
        if (System.getProperty("org.openrndr.test.backend") != "egl") return
        assertTrue(runApplication())
        assertTrue(runApplication())
    }
}
