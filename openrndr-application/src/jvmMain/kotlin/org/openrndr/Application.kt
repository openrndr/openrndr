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
 * [Application.run] looks for a preload class on the class path, if found this class instantiated
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
 * This class is responsible for selecting and initializing the appropriate graphics backend.
 *
 * By default, the GLFW backend is used. This can be customized by setting the VM property
 * `org.openrndr.application` to "ApplicationGLFW" or "ApplicationEGL".
 * However, if `org.openrndr.internal.nullgl.ApplicationNullGL` is found on the classpath,
 * NullGL will be used as the backend instead, regardless of other settings.
 */
@ApplicationDslMarker
actual abstract class Application {
    companion object {
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
            if (preload != null) {
                preload.onConfiguration(configuration)
                preload.onProgramSetup(program)
            }
        }
    }

    actual abstract var program: Program
    actual abstract var configuration: Configuration

    internal actual fun run() {
        runBlocking {
            this@Application.setup()
        }
        this.loop()
    }

    internal actual suspend fun runAsync() {
        throw NotImplementedError("Asynchronous application is unsupported, use Application.run()")
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
 * Runs [program] as a synchronous application with the given [configuration].
 * @see application
 */
actual fun application(program: Program, configuration: Configuration) {
    val applicationBase: ApplicationBase = ApplicationBase.initialize()
    val application = applicationBase.build(program, configuration)
    application.run()
}

/**
 * Runs [program] as an asynchronous application with the given [configuration].
 * @see applicationAsync
 */
actual suspend fun applicationAsync(program: Program, configuration: Configuration) {
    throw NotImplementedError("Asynchronous application is unsupported, use application()")
}