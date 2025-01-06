package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.lwjgl.BufferUtils
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.opengl.GLUtil
import org.lwjgl.opengles.GLES
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import org.openrndr.*
import org.openrndr.WindowMultisample.*
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.Clock
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.Session
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.ApplicationGlfwConfiguration.fixWindowSize
import org.openrndr.internal.gl3.extensions.BackBuffer
import org.openrndr.math.Vector2
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType
import java.io.File
import java.nio.Buffer
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.ceil
import org.lwjgl.opengl.GL43C as GL

private val logger = KotlinLogging.logger {}
internal var primaryWindow: Long = NULL

object ApplicationGlfwConfiguration {
    val fixWindowSize by lazy {
        Platform.type == PlatformType.WINDOWS || Platform.type == PlatformType.GENERIC
    }
}

class ApplicationGLFWGL3(override var program: Program, override var configuration: Configuration) : Application() {

    internal val windows: CopyOnWriteArrayList<ApplicationWindowGLFW> = CopyOnWriteArrayList()

    private var pointerInput: PointerInputManager? = null
    private var windowFocused = true

    private var window: Long = NULL
    private var realWindowTitle = configuration.title
    private var exitRequested = false
    private var exitHandled = false
    private var setupCalled = false

    override var presentationMode: PresentationMode = PresentationMode.AUTOMATIC
    override var windowContentScale: Double
        get() {
            val wcsx = FloatArray(1)
            val wcsy = FloatArray(1)
            glfwGetWindowContentScale(window, wcsx, wcsy)
            return wcsx[0].toDouble()
        }
        set(_) {}

    private var realCursorPosition = Vector2(0.0, 0.0)

    private var requestedMultisample = configuration.multisample

    override var cursorPosition: Vector2
        get() {
            return realCursorPosition
        }
        set(value) {
            realCursorPosition = value
            glfwSetCursorPos(window, value.x, value.y)
        }

    override var cursorVisible: Boolean = true
        set(value) {
            field = value
            if (value) {
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL)
            } else {
                when (cursorHideMode) {
                    MouseCursorHideMode.HIDE -> glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN)
                    MouseCursorHideMode.DISABLE -> {
                        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED)
                    }
                }
            }
        }

    override var cursorHideMode: MouseCursorHideMode = MouseCursorHideMode.HIDE
        set(value) {
            field = value
            cursorVisible = cursorVisible
        }

    private val cursorCache by lazy { mutableMapOf<CursorType, Long>() }
    override var cursorType: CursorType = CursorType.ARROW_CURSOR
        set(value) {
            if (value != field) {
                val cursor = cursorCache.getOrPut(value) {
                    val glfwCursor = when (value) {
                        CursorType.ARROW_CURSOR -> GLFW_ARROW_CURSOR
                        CursorType.IBEAM_CURSOR -> GLFW_IBEAM_CURSOR
                        CursorType.HAND_CURSOR -> GLFW_HAND_CURSOR
                        CursorType.CROSSHAIR_CURSOR -> GLFW_CROSSHAIR_CURSOR
                        CursorType.HRESIZE_CURSOR -> GLFW_HRESIZE_CURSOR
                        CursorType.VRESIZE_CURSOR -> GLFW_VRESIZE_CURSOR
                    }
                    glfwCreateStandardCursor(glfwCursor)
                }
                glfwSetCursor(window, cursor)
                field = value
            }
        }
    override var pointers: List<Pointer> = mutableListOf()

    private var _windowSize: Vector2? = null
    override var windowSize: Vector2
        get() {
            if (_windowSize == null) {
                stackPush().use {
                    val w = it.mallocInt(1)
                    val h = it.mallocInt(1)
                    glfwGetWindowSize(window, w, h)
                    _windowSize = Vector2(
                        if (fixWindowSize) (w[0].toDouble() / program.window.contentScale) else w[0].toDouble(),
                        if (fixWindowSize) (h[0].toDouble() / program.window.contentScale) else h[0].toDouble()
                    )
                }
            }
            return _windowSize ?: error("window size unknown")
        }
        set(value) {
            glfwSetWindowSize(
                window,
                if (fixWindowSize) (value.x * program.window.contentScale).toInt() else value.x.toInt(),
                if (fixWindowSize) (value.y * program.window.contentScale).toInt() else value.y.toInt()
            )
            _windowSize = null
        }


    override var windowPosition: Vector2
        get() {
            stackPush().use {
                val x = it.mallocInt(1)
                val y = it.mallocInt(1)
                glfwGetWindowPos(window, x, y)
                return Vector2(
                    if (fixWindowSize) (x[0].toDouble() / program.window.contentScale) else x[0].toDouble(),
                    if (fixWindowSize) (y[0].toDouble() / program.window.contentScale) else y[0].toDouble()
                )
            }
        }
        set(value) {
            glfwSetWindowPos(
                window,
                if (fixWindowSize) (value.x * program.window.contentScale).toInt() else value.x.toInt(),
                if (fixWindowSize) (value.y * program.window.contentScale).toInt() else value.y.toInt()
            )
        }

    override var windowResizable: Boolean
        get() {
            val attrib = glfwGetWindowAttrib(window, GLFW_RESIZABLE)
            return attrib != 0
        }
        set(value) {
            glfwSetWindowAttrib(window, GLFW_RESIZABLE, if (value) GLFW_TRUE else GLFW_FALSE)
        }

    override var windowMultisample: WindowMultisample
        get() = requestedMultisample
        set(_) {
            error("Changing window multisampling is not supported.")
        }

    override var clipboardContents: String?
        get() {
            return try {
                val result = glfwGetClipboardString(window)
                result
            } catch (e: Exception) {
                ""
            }
        }
        set(value) {
            if (value != null) {
                glfwSetClipboardString(window, value)
            } else {
                throw RuntimeException("clipboard contents can't be null")
            }
        }

    override val seconds: Double
        get() = glfwGetTime()

    override var windowTitle: String
        get() = realWindowTitle
        set(value) {
            glfwSetWindowTitle(window, value)
            realWindowTitle = value
        }

    init {
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                logger.debug { "Program interrupted" }
                exitRequested = true
                val start = System.currentTimeMillis()
                while (!exitHandled && System.currentTimeMillis() - start < 2000) {
                    sleep(10)
                }
            }
        })
        logger.debug { "debug output enabled" }
        logger.trace { "trace level enabled" }


        createPrimaryWindow()
        program.application = this
    }

    override suspend fun setup() {
        glfwDefaultWindowHints()
        when (DriverGL3Configuration.driverType) {
            DriverTypeGL.GL -> {
                glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL.GL_TRUE)
                glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
            }

            DriverTypeGL.GLES -> {
                glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_ES_API)
                glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_EGL_CONTEXT_API)
            }
        }

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_AUTO_ICONIFY, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, if (configuration.windowResizable) GLFW_TRUE else GLFW_FALSE)
        glfwWindowHint(GLFW_DECORATED, if (configuration.hideWindowDecorations) GLFW_FALSE else GLFW_TRUE)

        when (val c = configuration.multisample) {
            is SampleCount -> glfwWindowHint(GLFW_SAMPLES, c.count)
            SystemDefault -> glfwWindowHint(GLFW_SAMPLES, GLFW_DONT_CARE)
            Disabled -> glfwWindowHint(GLFW_SAMPLES, 0)
            else -> error("unsupported value $c")
        }

        glfwWindowHint(GLFW_RED_BITS, 8)
        glfwWindowHint(GLFW_GREEN_BITS, 8)
        glfwWindowHint(GLFW_BLUE_BITS, 8)
        glfwWindowHint(GLFW_STENCIL_BITS, 8)
        glfwWindowHint(GLFW_DEPTH_BITS, 24)
        glfwWindowHint(GLFW_SRGB_CAPABLE, GLFW_TRUE)

        if (configuration.windowAlwaysOnTop) {
            glfwWindowHint(GLFW_FLOATING, GLFW_TRUE)
        }

        logger.info { glfwGetVersionString() }

        if (DriverGL3Configuration.useDebugContext) {
            glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE)
        }

        val display = (configuration.display as? DisplayGLFWGL3)?.pointer ?: glfwGetPrimaryMonitor()

        val xscale = FloatArray(1)

        run {
            val yscale = FloatArray(1)
            glfwGetMonitorContentScale(display, xscale, yscale)
            logger.debug { "content scale ${xscale[0]} ${yscale[0]}" }
            if (xscale[0] != yscale[0]) {
                logger.debug {
                    """non uni-form scaling factors: ${xscale[0]} ${yscale[0]}"""
                }
            }
        }


        if (configuration.fullscreen == Fullscreen.SET_DISPLAY_MODE) {
            xscale[0] = 1.0f
        }

        program.window.contentScale = xscale[0].toDouble()

        logger.debug { "creating window" }

        if (configuration.windowTransparent) {
            glfwWindowHint(GLFW_TRANSPARENT_FRAMEBUFFER, 1)
        }


        val versions = DriverGL3.candidateVersions()
        var versionIndex = 0
        while (window == NULL && versionIndex < versions.size) {

            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, versions[versionIndex].majorVersion)
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, versions[versionIndex].minorVersion)

            window = if (configuration.fullscreen == Fullscreen.DISABLED) {
                /**
                 * We will be creating the [window] by passing [NULL] as the monitor
                 * for [glfwCreateWindow]. This will create the window on the user's primary display.
                 * The glfw docs suggest that if you want to specify the position as well, one should
                 * create the window and then move it. The trouble occurs when the user has specified to
                 * use a non-primary display and the two displays have different content scales.
                 * To remedy this we will be using primary display's scaling for the initial window
                 * and let glfw rescale it after we move the window to the specified display.
                 */
                val primaryDisplayScale = FloatArray(1)
                glfwGetMonitorContentScale(glfwGetPrimaryMonitor(), primaryDisplayScale, null)

                logger.debug { "primary display content scale: ${primaryDisplayScale[0]}" }

                val adjustedWidth =
                    if (fixWindowSize) (primaryDisplayScale[0] * configuration.width).toInt() else configuration.width
                val adjustedHeight =
                    if (fixWindowSize) (primaryDisplayScale[0] * configuration.height).toInt() else configuration.height

                logger.debug { "adjusted width x height $adjustedWidth x $adjustedHeight" }

                glfwCreateWindow(
                    adjustedWidth,
                    adjustedHeight,
                    configuration.title, NULL, primaryWindow
                )

            } else {
                logger.info { "creating fullscreen window" }
                var requestWidth = configuration.width
                var requestHeight = configuration.height


                if (configuration.fullscreen == Fullscreen.CURRENT_DISPLAY_MODE) {
                    val mode = glfwGetVideoMode(display)
                    if (mode != null) {
                        requestWidth = mode.width()
                        requestHeight = mode.height()
                        val refreshRate = mode.refreshRate()
                        logger.info { "creating fullscreen window at $requestWidth x $requestHeight @ ${refreshRate}hz" }
                        glfwWindowHint(GLFW_REFRESH_RATE, refreshRate)
                        mode.refreshRate()
                    } else {
                        throw RuntimeException("failed to determine current video mode")
                    }
                }
                glfwCreateWindow(
                    requestWidth,
                    requestHeight,
                    configuration.title, display, primaryWindow
                )
            }
            versionIndex++
        }
        if (window == NULL) {
            throw IllegalStateException("Window creation failed. ${DriverGL3Configuration.driverType}")
        }


        if (System.getProperty("os.name")
                .contains("windows", true) && System.getProperty("org.openrndr.pointerevents") != null
        ) {
            logger.info { "experimental touch input enabled" }
            pointerInput = PointerInputManagerWin32(window, this)
        }
        if (configuration.windowSetIcon) {
            val buf = BufferUtils.createByteBuffer(128 * 128 * 4)
            (buf as Buffer).rewind()
            for (y in 0 until 128) {
                for (x in 0 until 128) {
                    buf.putInt(0xffc0cbff.toInt())
                }
            }
            (buf as Buffer).flip()

            stackPush().use {
                glfwSetWindowIcon(
                    window, GLFWImage.malloc(1, it)
                        .width(128)
                        .height(128)
                        .pixels(buf)
                )
            }
        }

        logger.debug { "window created: $window" }

        // This is a workaround for the i3 window manager on linux ignoring
        // window placement if the window is not visible. But not hiding the window
        // can produce artifacts during the rest of the setup phase.
        // So we'll special case "i3" for now.
        if (System.getenv("DESKTOP_SESSION") == "i3") {
            logger.debug { "showing window early: $window" }
            glfwShowWindow(window)
        }

        // Get the thread stack and push a new frame
        stackPush().use { stack ->
            val px = stack.mallocInt(1) // int*
            val py = stack.mallocInt(1) // int*
            glfwGetMonitorPos(display, px, py)
            // We will set the window position onto the specified display so the
            // window gets resized according to the content scale of said display and
            // [glfwGetVideoMode] can return the expected dimensions for the window.
            // TODO: Can we calculate this ourselves and match glfw's behavior?
            glfwSetWindowPos(window, px.get(0), py.get(0))

            val pWidth = stack.mallocInt(1) // int*
            val pHeight = stack.mallocInt(1) // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight)

            // Get the resolution of the display
            val vidmode = glfwGetVideoMode(display)

            if (configuration.position == null) {
                if (vidmode != null) {
                    // Center the window
                    glfwSetWindowPos(
                        window,
                        px.get(0) + (vidmode.width() - pWidth.get(0)) / 2,
                        py.get(0) + (vidmode.height() - pHeight.get(0)) / 2
                    )
                }
                Unit
            } else {
                configuration.position?.let {
                    glfwSetWindowPos(
                        window,
                        px.get(0) + it.x,
                        py.get(0) + it.y
                    )
                }
            }
        }

        logger.debug { "making context current" }
        glfwMakeContextCurrent(window)

        if (configuration.vsync) {
            val enableTearControl = false
            if (enableTearControl && (glfwExtensionSupported("GLX_EXT_swap_control_tear") || glfwExtensionSupported("WGL_EXT_swap_control_tear"))) {
                glfwSwapInterval(-1)
            } else {
                glfwSwapInterval(1)
            }
        }
        var readyFrames = 0


        glfwSetWindowRefreshCallback(window) {
            if (readyFrames > 0) {
                if (setupCalled)
                    drawFrame()
                glfwSwapBuffers(window)
            }
            readyFrames++
        }

        glfwSetFramebufferSizeCallback(window) { window, width, height ->
            logger.debug { "resizing window ($window) to ${width}x$height " }
            _windowSize = null

            if (readyFrames > 0) {
                setupSizes()
                program.window.sized.trigger(
                    WindowEvent(
                        WindowEventType.RESIZED,
                        program.window.position,
                        program.window.size,
                        true
                    )
                )
            }
            readyFrames++
        }

        glfwSetWindowPosCallback(window) { _, x, y ->
            logger.trace { "window ($window) has moved to $x $y" }
            program.window.moved.trigger(
                WindowEvent(
                    WindowEventType.MOVED,
                    Vector2(x.toDouble(), y.toDouble()),
                    Vector2(0.0, 0.0),
                    true
                )
            )
        }

        glfwSetWindowFocusCallback(window) { _, focused ->
            logger.trace { "window ($window) focus has changed; focused=$focused" }
            windowFocused = focused
            if (focused) {
                program.window.focused.trigger(
                    WindowEvent(WindowEventType.FOCUSED, program.window.position, program.window.size, true)
                )
            } else {
                program.window.unfocused.trigger(
                    WindowEvent(WindowEventType.FOCUSED, program.window.position, program.window.size, false)
                )
            }
        }

        glfwSetWindowCloseCallback(window) { window ->
            logger.debug { "window ($window) closed" }
            program.window.closed.trigger(
                WindowEvent(
                    WindowEventType.CLOSED,
                    program.window.position,
                    program.window.size,
                    focused = true
                )
            )
            exitRequested = true
        }

        logger.debug { "showing window" }

        run {
            val adjustedMinimumWidth =
                if (fixWindowSize) (xscale[0] * configuration.minimumWidth).toInt() else configuration.minimumWidth
            val adjustedMinimumHeight =
                if (fixWindowSize) (xscale[0] * configuration.minimumHeight).toInt() else configuration.minimumHeight

            val adjustedMaximumWidth =
                if (fixWindowSize && configuration.maximumWidth != Int.MAX_VALUE) (xscale[0] * configuration.maximumWidth).toInt() else configuration.maximumWidth
            val adjustedMaximumHeight =
                if (fixWindowSize && configuration.maximumHeight != Int.MAX_VALUE) (xscale[0] * configuration.maximumHeight).toInt() else configuration.maximumHeight

            glfwSetWindowSizeLimits(
                window,
                adjustedMinimumWidth,
                adjustedMinimumHeight,
                adjustedMaximumWidth,
                adjustedMaximumHeight
            )
        }

        Animatable.clock(object : Clock {
            override val time: Long
                get() = (program.seconds * 1E3).toLong()
            override val timeNanos: Long
                get() = (program.seconds * 1E6).toLong()
        })

        glfwShowWindow(window)
        glfwSwapBuffers(window)
    }

    override fun createChildWindow(configuration: WindowConfiguration, program: Program): ApplicationWindow {
        // acquire current context, we may be calling this from another context that we want to return to
        val currentActiveContext = glfwGetCurrentContext()
        try {
            val window = createApplicationWindowGlfw(this, configuration, program)
            program.application = this
            (program as WindowProgram).applicationWindow = window
            program.drawer = this@ApplicationGLFWGL3.program.drawer

            window.setupGlfwEvents(this)

            window.updateSize()
            window.setupRenderTarget()
            if (DriverGL3Configuration.useBackBufferExtension) {
                program.extend(BackBuffer())
            }

            runBlocking {
                program.setup()
            }

            synchronized(windows) {
                windows.add(window)
            }
            return window
        } finally {
            if (currentActiveContext != 0L) {
                glfwMakeContextCurrent(currentActiveContext)
            }
        }
    }

    private fun createPrimaryWindow() {
        if (primaryWindow == NULL) {
            glfwSetErrorCallback(GLFWErrorCallback.create { error, description ->
                logger.debug {
                    "LWJGL Error - Code: ${Integer.toHexString(error)}, Description: ${
                        GLFWErrorCallback.getDescription(
                            description
                        )
                    }"
                }
            })
            val title = "OPENRNDR primary window"
            val versions = DriverGL3.candidateVersions()
            glfwDefaultWindowHints()
            when (DriverGL3Configuration.driverType) {
                DriverTypeGL.GL -> {
                    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL.GL_TRUE)
                    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
                }

                DriverTypeGL.GLES -> {
                    glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_ES_API)
                    glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_EGL_CONTEXT_API)
                }
            }
            glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_TRUE)
            glfwWindowHint(GLFW_RED_BITS, 8)
            glfwWindowHint(GLFW_GREEN_BITS, 8)
            glfwWindowHint(GLFW_BLUE_BITS, 8)
            glfwWindowHint(GLFW_STENCIL_BITS, 8)
            glfwWindowHint(GLFW_DEPTH_BITS, 24)
            glfwWindowHint(GLFW_SRGB_CAPABLE, GLFW_TRUE)
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)

            var foundVersion = null as? DriverVersionGL?
            for (version in versions) {
                glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, version.majorVersion)
                glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, version.minorVersion)

                primaryWindow = glfwCreateWindow(640, 480, title, NULL, NULL)
                foundVersion = version
                if (primaryWindow != 0L) {
                    foundVersion = version
                    break
                }
            }

            if (primaryWindow == 0L) {
                stackPush().use {
                    val pb = it.mallocPointer(1)
                    glfwGetError(pb)
                    val error = pb.stringASCII
                    error("primary window could not be created using ${DriverGL3Configuration.driverType} context. $error")
                }
            }
            Driver.driver = DriverGL3(foundVersion ?: error("no version found"))
        }
    }

    private val vaos = IntArray(1)

    val defaultRenderTarget by lazy { ProgramRenderTargetGL3(program) }
    fun preloop() {
        when (Driver.glType) {
            DriverTypeGL.GL -> org.lwjgl.opengl.GL.createCapabilities()
            DriverTypeGL.GLES -> GLES.createCapabilities()
        }
        logger.info { "OpenGL vendor: ${glGetString(GL.GL_VENDOR)}" }
        logger.info { "OpenGL renderer: ${glGetString(GL.GL_RENDERER)}" }
        logger.info { "OpenGL version: ${glGetString(GL.GL_VERSION)}" }

        if (DriverGL3Configuration.useDebugContext) {
            if (Driver.glType == DriverTypeGL.GL) {
                GLUtil.setupDebugMessageCallback()
                glEnable(GL.GL_DEBUG_OUTPUT_SYNCHRONOUS)
            }
            if (Driver.glType == DriverTypeGL.GLES) {
                GLESUtil.setupDebugMessageCallback()
                glEnable(GL.GL_DEBUG_OUTPUT_SYNCHRONOUS)
            }
        }

        program.driver = Driver.instance
        program.drawer = Drawer(Driver.instance)


        when (Driver.glType) {
            DriverTypeGL.GL -> {}
            DriverTypeGL.GLES -> (Driver.instance as DriverGL3).setupExtensions(
                GLES.getFunctionProvider() ?: error("no function provider")
            )
        }

        defaultRenderTarget.bind()

        setupSizes()
        if (DriverGL3Configuration.useBackBufferExtension) {
            program.extend(BackBuffer())
        }

        setupPreload(program, configuration)
        program.drawer.ortho()
    }

    private var drawRequested = true

    override fun loop() {
        logger.debug { "starting loop" }
        preloop()

        var lastDragPosition = Vector2.ZERO
        var lastMouseButtonDown = MouseButton.NONE
        val modifiers = mutableSetOf<KeyModifier>()

        glfwSetKeyCallback(window) { _, key, scancode, action, _ ->
            val name = when (key) {
                GLFW_KEY_SPACE -> "space"
                GLFW_KEY_ENTER -> "enter"
                GLFW_KEY_TAB -> "tab"
                GLFW_KEY_ESCAPE -> "escape"
                GLFW_KEY_UP -> "arrow-up"
                GLFW_KEY_DOWN -> "arrow-down"
                GLFW_KEY_LEFT -> "arrow-left"
                GLFW_KEY_RIGHT -> "arrow-right"
                GLFW_KEY_PRINT_SCREEN -> "print-screen"
                GLFW_KEY_PAGE_DOWN -> "page-down"
                GLFW_KEY_PAGE_UP -> "page-up"
                GLFW_KEY_HOME -> "home"
                GLFW_KEY_END -> "end"
                GLFW_KEY_BACKSPACE -> "backspace"
                GLFW_KEY_LEFT_ALT -> "left-alt"
                GLFW_KEY_RIGHT_ALT -> "right-alt"
                GLFW_KEY_LEFT_CONTROL -> "left-control"
                GLFW_KEY_RIGHT_CONTROL -> "right-control"
                GLFW_KEY_INSERT -> "insert"
                GLFW_KEY_DELETE -> "delete"
                GLFW_KEY_LEFT_SHIFT -> "left-shift"
                GLFW_KEY_RIGHT_SHIFT -> "right-shift"
                GLFW_KEY_LEFT_SUPER -> "left-super"
                GLFW_KEY_RIGHT_SUPER -> "right-super"
                GLFW_KEY_F1 -> "f1"
                GLFW_KEY_F2 -> "f2"
                GLFW_KEY_F3 -> "f3"
                GLFW_KEY_F4 -> "f4"
                GLFW_KEY_F5 -> "f5"
                GLFW_KEY_F6 -> "f6"
                GLFW_KEY_F7 -> "f7"
                GLFW_KEY_F8 -> "f8"
                GLFW_KEY_F9 -> "f9"
                GLFW_KEY_F10 -> "f10"
                GLFW_KEY_F11 -> "f11"
                GLFW_KEY_F12 -> "f12"
                GLFW_KEY_CAPS_LOCK -> "caps-lock"
                else -> glfwGetKeyName(key, scancode) ?: "<null>"
            }

            when (action) {
                GLFW_PRESS -> {
                    // This works around an issue in GLFW: https://github.com/glfw/glfw/issues/1630
                    if (key == GLFW_KEY_LEFT_ALT || key == GLFW_KEY_RIGHT_ALT) {
                        modifiers += KeyModifier.ALT
                    }
                    if (key == GLFW_KEY_LEFT_CONTROL || key == GLFW_KEY_RIGHT_CONTROL) {
                        modifiers += KeyModifier.CTRL
                    }
                    if (key == GLFW_KEY_LEFT_SHIFT || key == GLFW_KEY_RIGHT_SHIFT) {
                        modifiers += KeyModifier.SHIFT
                    }
                    if (key == GLFW_KEY_LEFT_SUPER || key == GLFW_KEY_RIGHT_SUPER) {
                        modifiers += KeyModifier.SUPER
                    }
                    program.keyboard.keyDown.trigger(KeyEvent(KeyEventType.KEY_DOWN, key, name, modifiers))

                }

                GLFW_RELEASE -> {
                    if (key == GLFW_KEY_LEFT_ALT || key == GLFW_KEY_RIGHT_ALT) {
                        modifiers -= KeyModifier.ALT
                    }
                    if (key == GLFW_KEY_LEFT_CONTROL || key == GLFW_KEY_RIGHT_CONTROL) {
                        modifiers -= KeyModifier.CTRL
                    }
                    if (key == GLFW_KEY_LEFT_SHIFT || key == GLFW_KEY_RIGHT_SHIFT) {
                        modifiers -= KeyModifier.SHIFT
                    }
                    if (key == GLFW_KEY_LEFT_SUPER || key == GLFW_KEY_RIGHT_SUPER) {
                        modifiers -= KeyModifier.SUPER
                    }
                    program.keyboard.keyUp.trigger(KeyEvent(KeyEventType.KEY_UP, key, name, modifiers))

                }

                GLFW_REPEAT ->
                    program.keyboard.keyRepeat.trigger(KeyEvent(KeyEventType.KEY_REPEAT, key, name, modifiers))
            }
        }

        glfwSetCharCallback(window) { _, codepoint ->
            program.keyboard.character.trigger(CharacterEvent(codepoint.toChar(), emptySet()))
        }

        glfwSetDropCallback(window) { _, count, names ->
            logger.debug { "$count file(s) have been dropped" }
            val pointers = PointerBuffer.create(names, count)
            val files = (0 until count).map {
                File(pointers.getStringUTF8(it))
            }
            program.window.drop.trigger(DropEvent(program.mouse.position, files.map { it.toString() }))
        }

        var down = false
        glfwSetScrollCallback(window) { _, xoffset, yoffset ->
            program.mouse.scrolled.trigger(
                MouseEvent(
                    program.mouse.position,
                    Vector2(xoffset, yoffset),
                    Vector2.ZERO,
                    MouseEventType.SCROLLED,
                    MouseButton.NONE,
                    modifiers
                )
            )
        }

        glfwSetWindowIconifyCallback(window) { _, iconified ->
            if (iconified) {
                program.window.minimized.trigger(
                    WindowEvent(
                        WindowEventType.MINIMIZED,
                        Vector2.ZERO,
                        Vector2.ZERO,
                        false
                    )
                )
            } else {
                program.window.restored.trigger(
                    WindowEvent(
                        WindowEventType.RESTORED,
                        program.window.position,
                        program.window.size,
                        true
                    )
                )
            }
        }

        glfwSetCursorEnterCallback(window) { _, entered ->
            if (entered) {
                program.mouse.entered.trigger(
                    MouseEvent(
                        program.mouse.position,
                        Vector2.ZERO,
                        Vector2.ZERO,
                        MouseEventType.ENTERED,
                        MouseButton.NONE,
                        emptySet()
                    )
                )
            } else {
                program.mouse.exited.trigger(
                    MouseEvent(
                        program.mouse.position,
                        Vector2.ZERO,
                        Vector2.ZERO,
                        MouseEventType.EXITED,
                        MouseButton.NONE,
                        emptySet()
                    )
                )
            }
        }

        glfwSetMouseButtonCallback(window) { _, button, action, _ ->
            val mouseButton = when (button) {
                GLFW_MOUSE_BUTTON_LEFT -> MouseButton.LEFT
                GLFW_MOUSE_BUTTON_RIGHT -> MouseButton.RIGHT
                GLFW_MOUSE_BUTTON_MIDDLE -> MouseButton.CENTER
                else -> MouseButton.NONE
            }
            val buttonsDown = BitSet()

            when (action) {
                GLFW_PRESS -> {
                    down = true
                    lastDragPosition = program.mouse.position
                    lastMouseButtonDown = mouseButton
                    program.mouse.buttonDown.trigger(
                        MouseEvent(
                            program.mouse.position,
                            Vector2.ZERO,
                            Vector2.ZERO,
                            MouseEventType.BUTTON_DOWN,
                            mouseButton,
                            modifiers
                        )
                    )
                    buttonsDown.set(button, true)
                }

                GLFW_RELEASE -> {
                    down = false
                    program.mouse.buttonUp.trigger(
                        MouseEvent(
                            program.mouse.position,
                            Vector2.ZERO,
                            Vector2.ZERO,
                            MouseEventType.BUTTON_UP,
                            mouseButton,
                            modifiers
                        )
                    )
                    buttonsDown.set(button, false)
                }
            }
        }

        glfwSetCursorPosCallback(window) { _, xpos, ypos ->
            val position = if (fixWindowSize) Vector2(xpos, ypos) / program.window.contentScale else Vector2(xpos, ypos)
            logger.trace { "mouse moved $xpos $ypos -- $position" }
            realCursorPosition = position
            program.mouse.moved.trigger(
                MouseEvent(
                    position,
                    Vector2.ZERO,
                    Vector2.ZERO,
                    MouseEventType.MOVED,
                    MouseButton.NONE,
                    modifiers
                )
            )
            if (down) {
                program.mouse.dragged.trigger(
                    MouseEvent(
                        position, Vector2.ZERO, position - lastDragPosition,
                        MouseEventType.DRAGGED, lastMouseButtonDown, modifiers
                    )
                )
                lastDragPosition = position
            }
        }

        if (configuration.showBeforeSetup) {
            logger.debug { "clearing and displaying pre-setup" }

            // clear the front buffer
            glDepthMask(true)
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
            glClear(GL.GL_COLOR_BUFFER_BIT or GL.GL_STENCIL_BUFFER_BIT or GL.GL_DEPTH_BUFFER_BIT)

            // swap the color buffers
            glfwSwapBuffers(window)

            // clear the back buffer
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
            glClear(GL.GL_COLOR_BUFFER_BIT or GL.GL_STENCIL_BUFFER_BIT or GL.GL_DEPTH_BUFFER_BIT)
            glDepthMask(false)
            glfwSwapBuffers(window)

            pointerInput?.pollEvents()
            glfwPollEvents()
        }

        if (configuration.hideCursor) {
            cursorVisible = false
        }
        cursorHideMode = configuration.cursorHideMode

        if (configuration.vsync) {
            if (glfwExtensionSupported("GLX_EXT_swap_control_tear") || glfwExtensionSupported("WGL_EXT_swap_control_tear")) {
                glfwSwapInterval(-1)
            } else {
                glfwSwapInterval(1)
            }
        }

        logger.debug { "calling program.setup" }
        var setupException: Throwable? = null
        try {
            runBlocking {
                program.setup()
            }
        } catch (t: Throwable) {
            setupException = t
        }

        setupException?.let {
            logger.error { "An error occurred inside the program setup" }
            postloop(setupException)
        }

        setupCalled = true

        var exception: Throwable? = null
        while (!exitRequested && !glfwWindowShouldClose(window)) {
            glfwMakeContextCurrent(window)

            if (presentationMode == PresentationMode.AUTOMATIC || drawRequested) {
                drawRequested = false
                exception = drawFrame()
                if (exception != null) {
                    logger.error { "An exception was thrown inside the OPENRNDR program" }
                    break
                }
                glfwSwapBuffers(window)
            }

            if (!windowFocused && configuration.unfocusBehaviour == UnfocusBehaviour.THROTTLE) {
                Thread.sleep(100)
            }

            if (presentationMode == PresentationMode.AUTOMATIC) {
                pointerInput?.pollEvents()
                glfwPollEvents()
            } else {
                Thread.sleep(10)
                pointerInput?.pollEvents()
                glfwPollEvents()
                deliverEvents()
                program.dispatcher.execute()
            }

            for (window in windows) {
                window.update()
            }

        }
        logger.debug { "Exiting draw loop" }

        postloop(exception)
    }

    fun postloop(exception: Throwable? = null) {
        // a child window's context may be current at this point
        glfwMakeContextCurrent(window)

        if (RenderTarget.active != defaultRenderTarget) {
            defaultRenderTarget.bindTarget()
        }

        for (window in windows) {
            window.destroy()
        }

        logger.debug { "Shutting down extensions" }
        synchronized(program.extensions) {
            for (extension in program.extensions) {
                extension.shutdown(program)
            }
            program.extensions.clear()
        }
        logger.debug { "Triggering program ended event" }
        program.ended.trigger(ProgramEvent(ProgramEventType.ENDED))

        if (exception == null) {
            Session.root.end()
        }
        Driver.instance.destroyContext(Driver.instance.contextID)

        glfwFreeCallbacks(window)
        glfwDestroyWindow(window)

        exitHandled = true
        logger.debug { "Exit handled" }

        glfwSetErrorCallback(null)?.free()
        if (!DriverGL3Configuration.skipGlfwTermination) {
            glfwTerminate()
        }

        logger.debug { "done" }

        exception?.let {
            logger.error { "OPENRNDR program ended with exception. (${exception.message})}" }
            throw it
        }
    }


    private fun deliverEvents() {
        program.window.drop.deliver()
        program.window.sized.deliver()
        program.window.unfocused.deliver()
        program.window.focused.deliver()
        program.window.minimized.deliver()
        program.window.restored.deliver()
        program.keyboard.keyDown.deliver()
        program.keyboard.keyUp.deliver()
        program.keyboard.keyRepeat.deliver()
        program.keyboard.character.deliver()
        program.mouse.moved.deliver()
        program.mouse.scrolled.deliver()
        program.mouse.buttonDown.deliver()
        program.mouse.buttonUp.deliver()
        program.mouse.dragged.deliver()
        program.mouse.entered.deliver()
        program.mouse.exited.deliver()
    }

    private fun drawFrame(): Throwable? {
        // reset cached values
        _windowSize = null
        setupSizes()

        defaultRenderTarget.bindTarget()

        glBindVertexArray(vaos[0])
        @Suppress("DEPRECATION")
        program.drawer.reset()
        program.drawer.ortho()
        deliverEvents()
        program.dispatcher.execute()
        try {
            logger.trace { "window: ${program.window.size.x.toInt()}x${program.window.size.y.toInt()} program: ${program.width}x${program.height}" }
            program.drawImpl()
        } catch (e: Throwable) {
            logger.error { "Caught exception inside the program loop. (${e.message})" }
            return e
        }
        return null
    }

    private fun setupSizes() {
        stackPush().use { stack ->
            val wcsx = stack.mallocFloat(1)
            val wcsy = stack.mallocFloat(1)
            glfwGetWindowContentScale(window, wcsx, wcsy)
            program.window.contentScale = wcsx[0].toDouble()

            val fbw = stack.mallocInt(1)
            val fbh = stack.mallocInt(1)
            glfwGetFramebufferSize(window, fbw, fbh)

            glViewport(0, 0, fbw[0], fbh[0])
            program.width = ceil(fbw[0] / program.window.contentScale).toInt()
            program.height = ceil(fbh[0] / program.window.contentScale).toInt()
        }
    }

    override fun exit() {
        exitRequested = true
    }

    override fun requestDraw() {
        drawRequested = true
    }

    override fun requestFocus() {
        glfwFocusWindow(window)
    }
}