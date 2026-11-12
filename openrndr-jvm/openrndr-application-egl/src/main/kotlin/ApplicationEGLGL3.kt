package org.openrndr.internal.gl3

import kotlinx.coroutines.runBlocking
import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.egl.EGL
import org.lwjgl.egl.EGL15.*
import org.lwjgl.egl.EXTDeviceEnumeration.eglQueryDevicesEXT
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33C.*
import org.lwjgl.system.JNI
import org.lwjgl.system.MemoryStack.stackPush
import org.openrndr.*
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.Clock
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawThread
import org.openrndr.draw.Drawer
import org.openrndr.draw.Session
import org.openrndr.draw.renderTarget
import org.openrndr.internal.Driver
import org.openrndr.internal.ResourceThread
import org.openrndr.math.Vector2
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

/** EGL_PLATFORM_SURFACELESS_MESA, a display that needs no window system, rendering on a GPU or with llvmpipe */
private const val EGL_PLATFORM_SURFACELESS_MESA = 0x31DD

/** EGL_PLATFORM_DEVICE_EXT, a display on a device, a GPU without a window system */
private const val EGL_PLATFORM_DEVICE_EXT = 0x313F

/** The longest a loop waits without work, in milliseconds, in case work is queued without waking it */
private const val IDLE_TIMEOUT = 1000L

/** The attribute of the profile of an OpenGL context, EGL_CONTEXT_OPENGL_PROFILE_MASK */
private const val EGL_CONTEXT_OPENGL_PROFILE_MASK_ATTRIBUTE = 0x30FD

/**
 * An EGL display with its configuration, the main context and its surface, from which shared contexts for other
 * threads are made
 */
private class EGLContextFactory(val display: Long, val config: Long, val version: DriverVersionGL) {
    var mainContext: Long = EGL_NO_CONTEXT

    /** A context that shares objects with the main context and a 1x1 pbuffer surface for it */
    fun createShared(): Pair<Long, Long> {
        stackPush().use {
            val surface = eglCreatePbufferSurface(display, config, it.ints(EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE))
            val context = eglCreateContext(display, config, mainContext, contextAttributes(it, version))
            require(context != EGL_NO_CONTEXT) { "failed to create a shared EGL context (EGL error ${eglGetError()})" }
            return surface to context
        }
    }

    fun makeCurrent(surface: Long, context: Long) {
        // the client API is bound per thread, eglGetCurrentContext returns the context of the bound API
        eglBindAPI(EGL_OPENGL_API)
        require(eglMakeCurrent(display, surface, surface, context)) {
            "failed to make the EGL context current (EGL error ${eglGetError()})"
        }
    }

    fun destroy(surface: Long, context: Long) {
        eglDestroyContext(display, context)
        if (surface != EGL_NO_SURFACE) {
            eglDestroySurface(display, surface)
        }
    }
}

private fun contextAttributes(stack: org.lwjgl.system.MemoryStack, version: DriverVersionGL) = stack.ints(
    EGL_CONTEXT_MAJOR_VERSION, version.majorVersion,
    EGL_CONTEXT_MINOR_VERSION, version.minorVersion,
    EGL_CONTEXT_OPENGL_PROFILE_MASK_ATTRIBUTE, EGL_CONTEXT_OPENGL_CORE_PROFILE_BIT,
    EGL_NONE
)

@Suppress("UNUSED_PARAMETER")
class ApplicationEGLGL3(override var program: Program, override var configuration: Configuration) : Application() {

    override var cursorVisible: Boolean = false
    override var cursorHideMode: MouseCursorHideMode = MouseCursorHideMode.HIDE

    override var cursorType: CursorType = CursorType.ARROW_CURSOR

    override var cursorPosition: Vector2
        get() = Vector2(0.0, 0.0)
        set(value) {}

    private lateinit var factory: EGLContextFactory
    private lateinit var driver: DriverGL3
    private var surface = EGL_NO_SURFACE
    private var context = EGL_NO_CONTEXT

    @Volatile
    private var exitRequested = false
    private var startTime = System.nanoTime()
    private val vaos = IntArray(1)

    init {
        program.application = this
    }

    override var windowHitTest: ((Vector2) -> Hit)?
        get() = null
        set(value) {
            logger.warn { "window hit test is not supported" }
        }

    /** Guards [drawRequested], the loop waits on it in [PresentationMode.MANUAL] */
    private val drawLock = Object()

    @Volatile
    private var drawRequested = false

    /** Is there work for the loop, queued in the dispatcher or for the main thread? Guarded by [drawLock] */
    private var workQueued = false

    /** Wakes the loop when it waits in [PresentationMode.MANUAL] */
    private fun wake() {
        synchronized(drawLock) {
            workQueued = true
            drawLock.notifyAll()
        }
    }

    override fun requestDraw() {
        synchronized(drawLock) {
            drawRequested = true
            drawLock.notifyAll()
        }
    }

    override fun requestFocus() {

    }

    override fun exit() {
        exitRequested = true
        synchronized(drawLock) {
            drawLock.notifyAll()
        }
    }

    override fun windowClose() {

    }

    override fun windowMaximize() {

    }

    override fun windowMinimize() {

    }

    override fun windowFullscreen(mode: Fullscreen) {

    }

    /**
     * Opens and initializes an EGL display. The platform can be chosen with the property `org.openrndr.egl.platform`
     * (`default`, `surfaceless` or `device`), the default display is used when the `EGL_PLATFORM` environment variable
     * is set. Otherwise the default display is tried first, then the surfaceless display of Mesa, which needs no
     * window system, then the first device, a GPU without a window system.
     */
    private fun openDisplay(): Long {
        val clientExtensions = eglQueryString(EGL_NO_DISPLAY, EGL_EXTENSIONS)?.split(" ")?.toSet() ?: emptySet()
        val capabilities = EGL.getCapabilities()

        // called through the function pointers, the LWJGL wrappers reject the native display EGL_DEFAULT_DISPLAY (0)
        // that the surfaceless platform needs
        fun platformDisplay(platform: Int, nativeDisplay: Long): Long {
            val function = when {
                capabilities.eglGetPlatformDisplay != 0L -> capabilities.eglGetPlatformDisplay
                capabilities.eglGetPlatformDisplayEXT != 0L -> capabilities.eglGetPlatformDisplayEXT
                else -> return EGL_NO_DISPLAY
            }
            return JNI.callPPP(platform, nativeDisplay, 0L, function)
        }

        fun default(): Long = eglGetDisplay(EGL_DEFAULT_DISPLAY)

        fun surfaceless(): Long =
            if ("EGL_MESA_platform_surfaceless" in clientExtensions) platformDisplay(EGL_PLATFORM_SURFACELESS_MESA, EGL_DEFAULT_DISPLAY)
            else EGL_NO_DISPLAY

        fun device(): Long {
            if ("EGL_EXT_platform_device" !in clientExtensions || "EGL_EXT_device_enumeration" !in clientExtensions) {
                return EGL_NO_DISPLAY
            }
            stackPush().use {
                val devices = it.mallocPointer(16)
                val count = it.mallocInt(1)
                if (!eglQueryDevicesEXT(devices, count) || count[0] == 0) {
                    return EGL_NO_DISPLAY
                }
                return platformDisplay(EGL_PLATFORM_DEVICE_EXT, devices[0])
            }
        }

        val platforms = when (val property = System.getProperty("org.openrndr.egl.platform")) {
            null, "" -> if (System.getenv("EGL_PLATFORM") != null) {
                listOf("default" to ::default)
            } else {
                listOf("default" to ::default, "surfaceless" to ::surfaceless, "device" to ::device)
            }
            "default" -> listOf("default" to ::default)
            "surfaceless" -> listOf("surfaceless" to ::surfaceless)
            "device" -> listOf("device" to ::device)
            else -> error("unknown EGL platform '$property', use default, surfaceless or device")
        }

        val failures = mutableListOf<String>()
        for ((name, open) in platforms) {
            val display = open()
            if (display == EGL_NO_DISPLAY) {
                failures.add("$name: not available")
                logger.info { "EGL $name platform is not available" }
                continue
            }
            stackPush().use {
                val major = it.mallocInt(1)
                val minor = it.mallocInt(1)
                if (eglInitialize(display, major, minor) && major[0] > 0) {
                    logger.info { "EGL display ($name platform), EGL ${major[0]}.${minor[0]}, vendor ${eglQueryString(display, EGL_VENDOR)}" }
                    return display
                }
                val reason = "eglInitialize failed with EGL error 0x${eglGetError().toString(16)}"
                failures.add("$name: $reason")
                logger.info { "EGL $name platform: $reason, trying the next platform" }
            }
        }
        error(
            "Could not open an EGL display (${failures.joinToString("; ")}). Without a window system, install Mesa " +
                    "(libegl-mesa0, libgl1-mesa-dri) for the surfaceless platform or a GPU driver with EGL device support."
        )
    }

    override suspend fun setup() {
        val display = openDisplay()

        stackPush().use {
            val configCount = it.mallocInt(1)

            val attributes = it.ints(
                EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
                EGL_BLUE_SIZE, 8,
                EGL_GREEN_SIZE, 8,
                EGL_RED_SIZE, 8,
                EGL_DEPTH_SIZE, 24,
                EGL_STENCIL_SIZE, 8,
                EGL_RENDERABLE_TYPE, EGL_OPENGL_BIT,
                EGL_NONE
            )

            if (!eglChooseConfig(display, attributes, null, configCount) || configCount[0] == 0) {
                error("no EGL config with an OpenGL pbuffer of 8 bit colors, 24 bit depth and 8 bit stencil (EGL error ${eglGetError()})")
            }

            logger.debug { "number of EGL configs found: ${configCount[0]}" }
            val configs = it.mallocPointer(configCount[0])
            eglChooseConfig(display, attributes, configs, configCount)

            // a pbuffer surface takes its size, not the attributes of the config
            val surfaceAttributes = it.ints(EGL_WIDTH, configuration.width, EGL_HEIGHT, configuration.height, EGL_NONE)
            surface = eglCreatePbufferSurface(display, configs[0], surfaceAttributes)

            eglBindAPI(EGL_OPENGL_API)

            // the most recent OpenGL version the driver supports
            var version: DriverVersionGL? = null
            for (candidate in DriverGL3.candidateVersions(DriverTypeGL.GL)) {
                context = eglCreateContext(display, configs[0], EGL_NO_CONTEXT, contextAttributes(it, candidate))
                if (context != EGL_NO_CONTEXT) {
                    version = candidate
                    break
                }
            }
            require(version != null && context != EGL_NO_CONTEXT) {
                "failed to create an OpenGL core context of version 3.3 or later (EGL error ${eglGetError()})"
            }
            logger.debug { "client APIs: ${eglQueryString(display, EGL_CLIENT_APIS)}" }
            factory = EGLContextFactory(display, configs[0], version)
            factory.mainContext = context
            factory.makeCurrent(surface, context)
            GL.createCapabilities()
            logger.info { "OpenGL ${glGetString(GL_VERSION)}, renderer ${glGetString(GL_RENDERER)}, vendor ${glGetString(GL_VENDOR)}" }

            val contextVersion = DriverVersionGL.find(DriverTypeGL.GL, glGetInteger(GL_MAJOR_VERSION), glGetInteger(GL_MINOR_VERSION)) ?: version
            driver = createDriver(contextVersion)
            Driver.driver = driver
            driver.onMainThreadWork = ::wake
            program.dispatcher.onWorkQueued = ::wake

            glGenVertexArrays(vaos)
            glBindVertexArray(vaos[0])

            startTime = System.nanoTime()

            Animatable.clock(object : Clock {
                override val time: Long
                    get() = (program.seconds * 1E3).toLong()
                override val timeNanos: Long
                    get() = (program.seconds * 1E6).toLong()
            })

            val defaultRenderTarget = ProgramRenderTargetGL3(program)
            defaultRenderTarget.bind()

            setupPreload(program, configuration)

            Program.active = program

            program.drawer = Drawer(driver)
            runBlocking {
                program.setup()
            }
        }
    }

    private fun createDriver(version: DriverVersionGL) = object : DriverGL3(version) {
        override val contextID: Long
            get() = eglGetCurrentContext()

        override fun createResourceThread(session: Session?, f: () -> Unit): ResourceThread {
            val (threadSurface, threadContext) = factory.createShared()
            thread(isDaemon = true, name = "ResourceThread") {
                factory.makeCurrent(threadSurface, threadContext)
                GL.createCapabilities()
                NullRenderTargetGL3().bind()
                Driver.instance.clear(ColorRGBa.BLACK)
                try {
                    f()
                } catch (e: Throwable) {
                    logger.error(e) { "Caught exception in resource thread." }
                } finally {
                    executeOnMainThread {
                        Driver.instance.destroyContext(threadContext)
                        factory.destroy(threadSurface, threadContext)
                    }
                }
            }
            return object : ResourceThread {}
        }

        override fun createDrawThread(session: Session?): DrawThread {
            val (threadSurface, threadContext) = factory.createShared()
            return DrawThreadEGL(factory, threadSurface, threadContext)
        }
    }

    override fun loop() {
        val defaultRenderTarget = renderTarget(configuration.width, configuration.height) {
            colorBuffer()
            depthBuffer()
        }
        defaultRenderTarget.bind()

        while (!exitRequested) {
            driver.processMainThreadExecutables()
            program.dispatcher.execute()

            // in manual presentation the loop waits for requestDraw, for work queued in the dispatcher or for the main
            // thread, or until the next delay of a coroutine is due
            if (presentationMode == PresentationMode.MANUAL) {
                synchronized(drawLock) {
                    if (!drawRequested && !exitRequested && !workQueued) {
                        val due = program.dispatcher.millisUntilNextDelay()
                        if (due == null || due > 0L) {
                            drawLock.wait(due?.coerceAtMost(IDLE_TIMEOUT) ?: IDLE_TIMEOUT)
                        }
                    }
                    workQueued = false
                }
                if (!drawRequested) {
                    continue
                }
                drawRequested = false
            }
            if (exitRequested) {
                break
            }

            glBindVertexArray(vaos[0])
            @Suppress("DEPRECATION")
            program.drawer.reset()
            program.drawer.ortho()
            program.drawImpl()
        }
        for (extension in program.extensions) {
            extension.shutdown(program)
        }

        program.ended.trigger(ProgramEvent(ProgramEventType.ENDED))

        driver.processMainThreadExecutables()
        Driver.instance.destroyContext(context)
        eglMakeCurrent(factory.display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)
        factory.destroy(surface, context)
        eglTerminate(factory.display)
    }

    override var clipboardContents: String?
        get() = ""
        set(value) {}
    override var windowTitle: String
        get() = ""
        set(value) {}
    override var windowPosition: Vector2
        get() = Vector2.ZERO
        set(value) {}
    override var windowSize: Vector2
        get() = Vector2(configuration.width.toDouble(), configuration.height.toDouble())
        set(value) {}

    override var windowMultisample: WindowMultisample
        get() = WindowMultisample.Disabled
        set(value) {
            logger.warn { "Setting window multisampling is not supported" }
        }
    override var windowResizable: Boolean
        get() = false
        set(value) {
            if (value) {
                logger.warn { "Resizable windows are not supported" }
            }
        }

    override val seconds: Double
        get() = (System.nanoTime() - startTime) / 1E9

    /** [PresentationMode.MANUAL] draws a frame for each [requestDraw], [PresentationMode.AUTOMATIC] as fast as it can */
    override var presentationMode: PresentationMode = PresentationMode.AUTOMATIC

    override var windowContentScale: Double
        get() = 1.0
        set(value) {}

    override fun createChildWindow(configuration: WindowConfiguration, program: Program): ApplicationWindow {
        TODO("Not yet implemented")
    }
}

/**
 * A thread with a context that shares objects with the main context, drawing what is dispatched to it
 */
private class DrawThreadEGL(factory: EGLContextFactory, surface: Long, context: Long) : DrawThread {
    private lateinit var realDrawer: Drawer
    private lateinit var realDispatcher: Dispatcher
    private val ready = java.util.concurrent.CountDownLatch(1)

    override val dispatcher: Dispatcher
        get() {
            ready.await()
            return realDispatcher
        }

    override val drawer: Drawer
        get() {
            ready.await()
            return realDrawer
        }

    init {
        thread(isDaemon = true, name = "DrawThreadEGL") {
            factory.makeCurrent(surface, context)
            GL.createCapabilities()
            realDispatcher = Dispatcher()
            realDrawer = Drawer(Driver.instance)
            val vaos = IntArray(1)
            glGenVertexArrays(vaos)
            glBindVertexArray(vaos[0])
            NullRenderTargetGL3().bind()
            ready.countDown()
            while (true) {
                realDispatcher.execute()
                Thread.sleep(1)
            }
        }
    }
}
