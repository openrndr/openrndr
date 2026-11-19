package org.openrndr.awt

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.biditextdriver.BidiTextDriverUax9
import org.openrndr.draw.DrawThread
import org.openrndr.draw.Session
import org.openrndr.draw.font.internal.BidiTextDriver
import org.openrndr.draw.font.internal.CachingFontDriver
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.fontdriver.freetype.FontDriverFreetype
import org.openrndr.internal.Driver
import org.openrndr.internal.ImageDriver
import org.openrndr.internal.KeyboardDriver
import org.openrndr.internal.ResourceThread
import org.openrndr.internal.gl3.*
import org.openrndr.internal.gl3.angle.extractAngleLibraries
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType
import org.openrndr.textshapingdriver.harfbuzz.TextShapingDriverHarfBuzz
import java.util.concurrent.CopyOnWriteArrayList

private val logger = KotlinLogging.logger { }

/**
 * Drivers and libraries of the AWT back-end
 */
internal object AWTBackend {
    private var initialized = false

    /** OpenGL ES is drawn through ANGLE on macOS */
    val angle: Boolean
        get() = Platform.type == PlatformType.MAC && DriverGL3Configuration.driverType == DriverTypeGL.GLES &&
                DriverGL3Configuration.glesBackend == GlesBackend.ANGLE

    /**
     * Installs the drivers that are not installed yet and extracts ANGLE, before the first canvas is created
     */
    @Synchronized
    fun initialize() {
        if (initialized) return
        initialized = true
        if (angle) {
            extractAngleLibraries()
        }
        // key events of the AWT back-end carry AWT key codes
        KeyboardDriver.driver = KeyboardDriverAWT()
        if (ImageDriver.driver == null) ImageDriver.driver = ImageDriverStbImage()
        if (FontDriver.driver == null) FontDriver.driver = CachingFontDriver(FontDriverFreetype())
        if (TextShapingDriver.driver == null) TextShapingDriver.driver = TextShapingDriverHarfBuzz()
        if (BidiTextDriver.driver == null) BidiTextDriver.driver = BidiTextDriverUax9()
    }

    /**
     * Creates the OpenGL driver, once, with a context current
     */
    fun ensureDriver() {
        if (Driver.driver != null) return
        val type = DriverGL3Configuration.driverType
        val major = glGetInteger(org.lwjgl.opengl.GL30.GL_MAJOR_VERSION)
        val minor = glGetInteger(org.lwjgl.opengl.GL30.GL_MINOR_VERSION)
        val version = DriverVersionGL.find(type, major, minor)
            ?: error("unsupported driver version $type $major.$minor")
        logger.info { "OpenGL vendor: ${glGetString(org.lwjgl.opengl.GL11.GL_VENDOR)}" }
        logger.info { "OpenGL renderer: ${glGetString(org.lwjgl.opengl.GL11.GL_RENDERER)}" }
        logger.info { "OpenGL version: ${glGetString(org.lwjgl.opengl.GL11.GL_VERSION)}" }

        Driver.driver = object : DriverGL3(version) {
            override val contextID: Long
                get() = AWTRenderLoop.contextId

            override fun createResourceThread(session: Session?, f: () -> Unit): ResourceThread {
                error("resource threads are not supported by the AWT application back-end")
            }

            override fun createDrawThread(session: Session?): DrawThread {
                error("draw threads are not supported by the AWT application back-end")
            }
        }
    }
}

/**
 * The thread that draws all programs of the AWT back-end, in turn, like the windows of the other back-ends. Programs
 * are set up, drawn and receive their events on this thread.
 */
internal object AWTRenderLoop {
    /** the context that is current on the render thread */
    @Volatile
    var contextId = 0L

    private val applications = CopyOnWriteArrayList<ApplicationAWT>()
    private val lock = Object()
    private var thread: Thread? = null

    fun add(application: ApplicationAWT) {
        synchronized(lock) {
            applications.addIfAbsent(application)
            if (thread == null) {
                thread = Thread(::run, "openrndr-awt-render").apply {
                    isDaemon = true
                    start()
                }
            }
            lock.notifyAll()
        }
    }

    fun remove(application: ApplicationAWT) {
        applications.remove(application)
    }

    private fun run() {
        while (true) {
            synchronized(lock) {
                while (applications.isEmpty()) {
                    lock.wait()
                }
            }
            var drawn = false
            for (application in applications) {
                drawn = try {
                    application.frame() || drawn
                } catch (e: Throwable) {
                    logger.error(e) { "program failed, it is ended" }
                    application.fail(e)
                    drawn
                }
            }
            if (!drawn) {
                // no canvas is showing, wait for one
                Thread.sleep(8)
            }
        }
    }
}
