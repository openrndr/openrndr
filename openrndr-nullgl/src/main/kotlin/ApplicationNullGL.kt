package org.openrndr.internal.nullgl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.openrndr.*


import org.openrndr.draw.Drawer
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2

val logger = KotlinLogging.logger {}

class ApplicationNullGL(override var program: Program, override var configuration: Configuration) : Application() {

    val startMS = System.currentTimeMillis()
    private var exitRequested = false

    init {
        Driver.driver = DriverNullGL()
        program.application = this
    }

    override fun requestDraw() {

    }

    override fun requestFocus() {

    }

    override fun exit() {
        exitRequested = true
    }

    override suspend fun setup() {
        logger.debug { "entering setup" }
        setupPreload(program, configuration)
    }

    override fun loop() {
        logger.debug { "entering loop" }
        program.driver = Driver.instance
        program.drawer = Drawer(Driver.instance)


        logger.debug { "calling program.setup" }
        var setupException: Throwable? = null
        try {
            runBlocking { program.setup() }
        } catch (t: Throwable) {
            setupException = t
        }

        setupException?.let {
            logger.error { "An error occurred inside the program setup" }
            throw(it)
        }
        while (!exitRequested) {
            program.drawImpl()
        }

    }

    override var clipboardContents: String?
        get() = TODO("Not yet implemented")
        set(_) {}
    override var windowTitle = "nulllgl window"
    override var windowPosition = Vector2.ZERO
    override var windowSize = Vector2(640.0, 480.0)
    override var windowMultisample: WindowMultisample
        get() = WindowMultisample.Disabled
        set(_) {
            logger.warn { "Setting window multisampling is not supported" }
        }
    override var windowResizable: Boolean
        get() = false
        set(value) {
            if (value) {
                logger.warn { "Setting window multisampling is not supported" }
            }
        }

    override var cursorPosition: Vector2 = Vector2.ZERO
    override var cursorVisible: Boolean = true
    override var cursorHideMode: MouseCursorHideMode = MouseCursorHideMode.HIDE
    override var cursorType: CursorType = CursorType.ARROW_CURSOR
    override var pointers: List<Pointer> = emptyList()

    override val seconds: Double
        get() = (startMS - System.currentTimeMillis()) / 1000.0
    override var presentationMode: PresentationMode = PresentationMode.AUTOMATIC
    override var windowContentScale: Double
        get() = 1.0
        set(_) {}

    override fun createChildWindow(configuration: WindowConfiguration, program: Program): ApplicationWindow {
        TODO("Not yet implemented")
    }
}