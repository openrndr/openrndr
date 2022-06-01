package org.openrndr

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.openrndr.math.Vector2

private val logger = KotlinLogging.logger {}

/**
 * Application preload class
 * [ApplicationPreload] can be used to configure [Application] and [Program] without changing
 * user code.
 *
 * [Application.Companion.run] looks for a preload class on the class path, if found this class instantiated
 * and used for configuration.
 *
 * The `org.openrndr.preloadclass` property can be used to set the name for the preload class,
 * the default value is `org.openrndr.Preload`.
 *
 */
open class ApplicationPreload {
    /**
     * called before passing the configuration to [Application]
     * This can be used to override resolution and other configuration settings
     */
    open fun onConfiguration(configuration: Configuration) {}

    /**
     * called before setting up the [Program]
     * This can be used to install extensions
     */
    open fun onProgramSetup(program: Program) {}
}

/**
 * Application interface
 */
@ApplicationDslMarker
actual abstract class Application {
    companion object {
        fun initialize(program: Program, configuration: Configuration): Application {
            if (enableProfiling) {
                Runtime.getRuntime().addShutdownHook(object : Thread() {
                    override fun run() {
                        report()
                    }
                })
            }

            val c = applicationClass(configuration)
            return c.declaredConstructors[0].newInstance() as Application
        }

        fun setupPreload(program: Program, configuration: Configuration) {
            val preloadClassName =
                (System.getProperties()["org.openrndr.preloadclass"] as? String)
                    ?: "org.openrndr.Preload"
            val preload = try {
                val c =
                    Application::class.java.classLoader.loadClass(preloadClassName) as Class<ApplicationPreload>
                logger.info { "preload class found '$preloadClassName'" }
                c.constructors.first().newInstance() as ApplicationPreload
            } catch (e: ClassNotFoundException) {
                logger.info { "no preload class found '$preloadClassName'" }
                null
            }
            if(preload != null) {
                preload.onConfiguration(configuration)
                preload.onProgramSetup(program)
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

    actual abstract var program: Program
    actual abstract var configuration: Configuration

    actual fun run(program: Program, configuration: Configuration) {
        runBlocking {
            this@Application.setup(program, configuration)
        }
        this.loop()
    }

    actual suspend fun runAsync(program: Program, configuration: Configuration) {
        error("use run")
    }

    actual abstract fun requestDraw()
    actual abstract fun requestFocus()

    actual abstract fun exit()
    actual abstract suspend fun setup(program: Program, configuration: Configuration)

    actual abstract fun loop()
    abstract val displays: List<Display>
    actual abstract var clipboardContents: String?
    actual abstract var windowTitle: String
    actual abstract var windowPosition: Vector2
    actual abstract var windowSize: Vector2
    actual abstract var windowResizable: Boolean
    actual abstract var windowMultisample: WindowMultisample

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
    val application: Application = Application.initialize(program, configuration)
    application.run(program, configuration)
}

actual suspend fun applicationAsync(program: Program, configuration: Configuration) {
    error("use application()")
}