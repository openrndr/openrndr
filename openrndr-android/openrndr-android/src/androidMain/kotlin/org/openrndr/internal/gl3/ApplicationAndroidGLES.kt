package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.openrndr.Application
import org.openrndr.ApplicationWindow
import org.openrndr.Configuration
import org.openrndr.CursorType
import org.openrndr.GLSurfaceViewListener
import org.openrndr.MouseCursorHideMode
import org.openrndr.PresentationMode
import org.openrndr.Program
import org.openrndr.WindowConfiguration
import org.openrndr.WindowMultisample
import org.openrndr.draw.Drawer
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2

private val logger = KotlinLogging.logger {}

@Suppress("UNUSED_PARAMETER")
class ApplicationAndroidGLES(
    override var program: Program,
    override var configuration: Configuration
) : Application(), GLSurfaceViewListener {

    override var cursorVisible: Boolean = false
    override var cursorHideMode: MouseCursorHideMode = MouseCursorHideMode.HIDE

    override var cursorType: CursorType = CursorType.ARROW_CURSOR
//    override var pointers: List<Pointer> = emptyList()

    override var cursorPosition: Vector2
        get() = Vector2(0.0, 0.0)
        set(value) {}
    private var driver = DriverAndroidGLES(DriverVersionGL.GLES_VERSION_3_1)

    private var initialized = false

    //    private var exitRequested = false
    private var startTime = 0L
    private val vaos = IntArray(1)

    init {
        logger.info { "ApplicationAndroidGLES::init" }
        Driver.driver = driver
        program.application = this
    }

    /** Called from Renderer.onSurfaceCreated (GL thread). */
    override fun onSurfaceCreated() {
        logger.info { "onSurfaceCreated" }

        // GL state that must happen on the GL thread:
        glGenVertexArrays(vaos)
        glBindVertexArray(vaos[0])

        // Install drawer & run Program.setup() on the GL thread
        program.drawer = Drawer(driver)
        program.driver = driver
        startTime = System.currentTimeMillis()

        // TODO: need to handle app foreground after going background
        if (!initialized) {
            runBlocking { program.setup() }
        }

        initialized = true
    }

    /** Called from Renderer.onSurfaceChanged (GL thread). */
    override fun onSurfaceChanged(width: Int, height: Int) {
        logger.info { "onSurfaceChanged" }
        driver.onSurfaceChanged(width, height)
        program.width = width
        program.height = height
    }

    /** Called every frame from Renderer.onDrawFrame (GL thread). */
    override fun onDrawFrame() {
        glBindVertexArray(vaos[0])
        @Suppress("DEPRECATION")
        program.drawer.reset()
        program.drawer.ortho()
        program.drawImpl()
    }

    override fun requestDraw() { /* GLSurfaceView drives the loop */
    }

    override fun requestFocus() {}
    override fun exit() { /* activity handles finish() */
    }

    override suspend fun setup() { /* done in onSurfaceCreated */
    }

    // Desktop's blocking loop() is not used on Android.
    override fun loop() { /* not used; GLSurfaceView drives frames */
    }

    override var clipboardContents: String? get() = null; set(_) {}
    override var windowTitle: String get() = ""; set(_) {}
    override var windowPosition: Vector2 get() = Vector2.ZERO; set(_) {}
    override var windowSize: Vector2
        get() = Vector2(configuration.width.toDouble(), configuration.height.toDouble())
        set(_) {}
    override var windowMultisample: WindowMultisample
        get() = WindowMultisample.Disabled
        set(_) {}
    override var windowResizable: Boolean
        get() = false
        set(_) {}
    override val seconds: Double
        get() = (System.currentTimeMillis() - startTime) / 1000.0
    override var presentationMode: PresentationMode
        get() = PresentationMode.AUTOMATIC
        set(_) {}
    override var windowContentScale: Double
        get() = 1.0
        set(_) {}

    override fun createChildWindow(
        configuration: WindowConfiguration,
        program: Program
    ): ApplicationWindow = error("Child windows are not supported on Android")

}