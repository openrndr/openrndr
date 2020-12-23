package org.openrndr

import mu.KotlinLogging
import org.openrndr.math.Vector2
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

/**
 * PresentationMode describes modes of frame presentation
 */
enum class PresentationMode {
    /**
     * automatic presentation mode, frames are presented at highest rate possible
     */
    AUTOMATIC,

    /**
     * manual presentation mode, presentation only takes place after requesting redraw
     */
    MANUAL,
}

@DslMarker
annotation class ApplicationDslMarker

/**
 * Application interface
 */
@ApplicationDslMarker
abstract class Application {
    companion object {
        fun run(program: Program, configuration: Configuration) {

            if (enableProfiling) {
                Runtime.getRuntime().addShutdownHook(object : Thread() {
                    override fun run() {
                        report()
                    }
                })
            }

            val c = applicationClass(configuration)
            val application = c.declaredConstructors[0].newInstance(program, configuration) as Application
            application.setup()
            application.loop()
        }

        fun runAsync(program: Program, configuration: Configuration) {
            val c = applicationClass(configuration)
            val application = c.declaredConstructors[0].newInstance(program, configuration) as Application
            thread {
                application.setup()
                application.loop()
            }
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

    abstract fun requestDraw()
    abstract fun requestFocus()

    abstract fun exit()
    abstract fun setup()

    abstract fun loop()
    abstract var clipboardContents: String?
    abstract var windowTitle: String

    abstract var windowPosition: Vector2
    abstract var windowSize: Vector2
    abstract var cursorPosition: Vector2
    abstract var cursorVisible: Boolean
    abstract var cursorType: CursorType

    abstract val seconds: Double

    abstract var presentationMode: PresentationMode
}

/**
 * Runs [program] as an application using [configuration].
 */
fun application(program: Program, configuration: Configuration = Configuration()) {
    Application.run(program, configuration)
}

/**
 * Resolves resource named [name] relative to [class] as a [String] based URL.
 */
fun resourceUrl(name: String, `class`: Class<*> = Application::class.java): String {
    val resource = `class`.getResource(name)
    if (resource == null) {
        throw RuntimeException("resource $name not found")
    } else {
        return `class`.getResource(name).toExternalForm()
    }
}