package org.openrndr

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.openrndr.math.Vector2
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

/**
 * Application interface
 */
@ApplicationDslMarker
actual abstract class Application {
    actual companion object {
        actual fun run(program: Program, configuration: Configuration) {
            if (enableProfiling) {
                Runtime.getRuntime().addShutdownHook(object : Thread() {
                    override fun run() {
                        report()
                    }
                })
            }

            val c = applicationClass(configuration)
            val application = c.declaredConstructors[0].newInstance(program, configuration) as Application
            runBlocking {
                application.setup()
            }
            application.loop()
        }

        actual suspend fun runAsync(program: Program, configuration: Configuration) {
            error("use run")
        }

        private fun applicationClass(configuration: Configuration): Class<*> {
            try {
                val c = Application::class.java.classLoader.loadClass("org.openrndr.internal.nullgl.ApplicationNullGL")
                logger.debug { "NullGL found" }
                return c
            } catch (e: ClassNotFoundException) {
                logger.debug { "NullGL not found" }
            }

            return if (!configuration.headless)
                Application::class.java.classLoader.loadClass("org.openrndr.internal.gl3.ApplicationGLFWGL3")
            else
                Application::class.java.classLoader.loadClass("org.openrndr.internal.gl3.ApplicationEGLGL3")
        }
    }

    actual abstract fun requestDraw()
    actual abstract fun requestFocus()

    actual abstract fun exit()
    actual abstract suspend fun setup()

    actual abstract fun loop()
    actual abstract var clipboardContents: String?
    actual abstract var windowTitle: String

    actual abstract var windowPosition: Vector2
    actual abstract var windowSize: Vector2
    actual abstract var cursorPosition: Vector2
    actual abstract var cursorVisible: Boolean
    actual abstract var cursorHideMode: MouseCursorHideMode
    actual abstract var cursorType: CursorType

    actual abstract val seconds: Double

    actual abstract var presentationMode: PresentationMode
    actual abstract var windowContentScale: Double
}

/**
 * Runs [program] as an application using [configuration].
 */
actual fun application(program: Program, configuration: Configuration) {
    Application.run(program, configuration)
}

actual suspend fun applicationAsync(program: Program, configuration: Configuration) {
    error("use application()")
}