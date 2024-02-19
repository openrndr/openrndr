package org.openrndr

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.openrndr.ApplicationConfiguration.preloadClassName
import org.openrndr.math.Vector2
import org.openrndr.platform.Platform

private val logger = KotlinLogging.logger {}

/**
 * [ApplicationPreload] can be used to configure [Application] and [Program] without changing
 * user code.
 *
 * On application initialization a preload class is looked for on the classpath,
 * if found this class is instantiated and used for configuration.
 *
 * The `org.openrndr.preloadclass` property can be used to set the name for the preload class,
 * the default value is `org.openrndr.Preload`.
 *
 */
open class ApplicationPreload {
    /**
     * Called before passing the configuration to [Application].
     * This can be used to override resolution and other configuration settings.
     */
    open fun onConfiguration(configuration: Configuration) {}

    /**
     * Called before setting up the [Program]
     * This can be used to install extensions.
     */
    open fun onProgramSetup(program: Program) {}
}

object ApplicationConfiguration {
    val preloadClassName by lazy { Platform.property("org.openrndr.preloadclass") ?: "org.openrndr.Preload" }
}

abstract class ApplicationWindow(val program: Program) {
    abstract var windowTitle: String
    abstract var windowPosition: Vector2
    abstract var windowSize: Vector2
    abstract val windowResizable: Boolean
    abstract val windowMultisample: WindowMultisample
    abstract val windowFocused: Boolean

    abstract var cursorPosition: Vector2
    abstract var cursorVisible: Boolean
    abstract var cursorHideMode: MouseCursorHideMode
    abstract var cursorType: CursorType
    abstract val cursorInWindow: Boolean

    abstract var presentationMode: PresentationMode
    abstract fun requestDraw()
    abstract var windowContentScale: Double
}

/**
 * This class is responsible for selecting and initializing the appropriate graphics backend.
 *
 * By default, the GLFW backend is used. This can be customized by setting the VM property
 * `org.openrndr.application` to "GLFW" or "EGL".
 * However, if `org.openrndr.internal.nullgl.ApplicationNullGL` is found on the classpath,
 * NullGL will be used as the backend instead, regardless of other settings.
 */
actual abstract class Application {
    companion object {
        fun setupPreload(program: Program, configuration: Configuration) {
            val preload = try {
                @Suppress("UNCHECKED_CAST") val c =
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

    actual abstract val pointers: List<Pointer>

    actual abstract val seconds: Double

    actual abstract var presentationMode: PresentationMode
    actual abstract var windowContentScale: Double
    abstract fun createChildWindow(configuration: WindowConfiguration, program: Program): ApplicationWindow
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