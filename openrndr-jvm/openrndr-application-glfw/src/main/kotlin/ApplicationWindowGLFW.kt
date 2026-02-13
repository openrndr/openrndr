package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.BufferUtils
import org.lwjgl.PointerBuffer

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.opengl.GL43C
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.openrndr.*
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.ApplicationGlfwConfiguration.fixWindowSize
import org.openrndr.math.Vector2
import java.io.File
import java.nio.Buffer
import java.util.*
import kotlin.math.ceil

private val logger = KotlinLogging.logger { }

class ApplicationWindowGLFW(
    val application: ApplicationGLFWGL3,
    val window: Long,
    windowTitle: String,
    override val windowResizable: Boolean,
    override val windowMultisample: WindowMultisample,
    override val windowClosable: Boolean,
    program: Program,
) : ApplicationWindow(program) {
    private var defaultRenderTargetGL3: ProgramRenderTargetGL3? = null
    private var drawRequested = true

    override var unfocusBehaviour: UnfocusBehaviour
        get() = TODO("Not yet implemented")
        set(value) {}

    internal fun updateSize() {
        MemoryStack.stackPush().use { stack ->
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

    fun setupRenderTarget() {
        glfwMakeContextCurrent(window)
        if (defaultRenderTargetGL3 == null) {
            logger.debug { "creating default render target for context ${Driver.instance.contextID}" }
            defaultRenderTargetGL3 = ProgramRenderTargetGL3(program)
            defaultRenderTargetGL3!!.bind()
        }
    }

    fun update() {
        deliverEvents()
        if (presentationMode == PresentationMode.AUTOMATIC || drawRequested || program.dispatcher.shouldExecute) {
            //glfwMakeContextCurrent(window)
            setupRenderTarget()
            updateSize()
            @Suppress("DEPRECATION")
            program.drawer.reset()
            program.drawer.ortho()

            defaultRenderTargetGL3!!.bindTarget()
            program.dispatcher.execute()
        }
        if (presentationMode == PresentationMode.AUTOMATIC || drawRequested) {
            program.drawImpl()
            glfwSwapBuffers(window)
        }
    }

    override var windowContentScale: Double
        get() {
            val wcsx = FloatArray(1)
            val wcsy = FloatArray(1)
            glfwGetWindowContentScale(window, wcsx, wcsy)
            return wcsx[0].toDouble()
        }
        set(_) {}

    override var windowTitle: String = windowTitle
        set(value) {
            field = value
            glfwSetWindowTitle(window, value)
        }
    override var windowPosition: Vector2
        get() {
            MemoryStack.stackPush().use {
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

    /**
     * cached window size
     */
    private var _windowSize: Vector2? = null
    override var windowSize: Vector2
        get() {
            if (_windowSize == null) {
                MemoryStack.stackPush().use {
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

    override var cursorPosition: Vector2 = Vector2.ZERO
    override var cursorVisible: Boolean = true
        set(value) {
            field = value
            if (value) {
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL)
            } else {
                when (cursorHideMode) {
                    MouseCursorHideMode.HIDE -> glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN)
                    MouseCursorHideMode.DISABLE -> glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED)
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

    override var cursorInWindow: Boolean = false

    override var presentationMode: PresentationMode = PresentationMode.AUTOMATIC
    override fun requestDraw() {
        drawRequested = true
    }

    override var windowFocused: Boolean = false

    fun setDefaultIcon() {
        val buf = BufferUtils.createByteBuffer(128 * 128 * 4)
        (buf as Buffer).rewind()
        for (y in 0 until 128) {
            for (x in 0 until 128) {
                buf.putInt(0xffc0cbff.toInt())
            }
        }
        (buf as Buffer).flip()

        MemoryStack.stackPush().use {
            glfwSetWindowIcon(
                window, GLFWImage.malloc(1, it)
                    .width(128)
                    .height(128)
                    .pixels(buf)
            )
        }
    }

    fun setupGlfwEvents(application: ApplicationGLFWGL3) {
        val modifiers = mutableSetOf<KeyModifier>()

        var down = false
        var lastDragPosition = Vector2.ZERO
        var lastMouseButtonDown = MouseButton.NONE

        var readyFrames = 0

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


        glfwSetFramebufferSizeCallback(window) { window, width, height ->
            logger.trace { "resizing window ($window) to ${width}x$height " }
            _windowSize = null
            if (readyFrames > 0) {
                updateSize()
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

        glfwSetWindowCloseCallback(window) {
            if (windowClosable) {
                logger.debug { "window $window closed" }
                program.window.closed.postpone = false
                program.window.closed.trigger(WindowEvent(WindowEventType.CLOSED, Vector2.ZERO, Vector2.ZERO, false))
                destroy()
            }
        }

        glfwSetCursorEnterCallback(window) { _, entered ->
            cursorInWindow = entered
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
            cursorPosition = position
            logger.trace { "mouse moved $xpos $ypos -- $position" }
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
    }

    private fun deliverEvents() {
        /*
        Here we should check first if any events need to be delivered before switching the context
         */
        glfwMakeContextCurrent(window)
        program.window.drop.deliver()
        //program.window.dropTexts.deliver()
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

    override fun destroy() {
        logger.debug { "destroying window $window" }
        for (extension in program.extensions) {
            extension.shutdown(program)
        }
        Driver.instance.destroyContext(window)
        glfwDestroyWindow(window)
        application.windows.remove(this)
    }
}

fun createApplicationWindowGlfw(
    parentWindow: Long?,
    application: ApplicationGLFWGL3,
    configuration: WindowConfiguration,
    program: Program
): ApplicationWindowGLFW {

    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_FLOATING, GLFW_FALSE)
    glfwWindowHint(GLFW_AUTO_ICONIFY, GLFW_FALSE)
    glfwWindowHint(GLFW_RESIZABLE, if (configuration.resizable) GLFW_TRUE else GLFW_FALSE)
    glfwWindowHint(GLFW_DECORATED, if (configuration.hideDecorations) GLFW_FALSE else GLFW_TRUE)

    when (DriverGL3Configuration.driverType) {
        DriverTypeGL.GL -> {
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL43C.GL_TRUE)
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        }

        DriverTypeGL.GLES -> {
            glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_ES_API)
            val useAngle = DriverGL3Configuration.glesBackend == GlesBackend.ANGLE
            if (useAngle) {
                glfwWindowHint(GLFW_ANGLE_PLATFORM_TYPE, GLFW_ANGLE_PLATFORM_TYPE_METAL)
            }
            glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_EGL_CONTEXT_API)
        }
    }
    glfwWindowHint(GLFW_RED_BITS, 8)
    glfwWindowHint(GLFW_GREEN_BITS, 8)
    glfwWindowHint(GLFW_BLUE_BITS, 8)
    glfwWindowHint(GLFW_STENCIL_BITS, 8)
    glfwWindowHint(GLFW_DEPTH_BITS, 24)
    glfwWindowHint(GLFW_SRGB_CAPABLE, GLFW_TRUE)

    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)

    val position = configuration.position
    if (position != null) {
        glfwWindowHint(GLFW_POSITION_X, position.x)
        glfwWindowHint(GLFW_POSITION_Y, position.y)
    }

    when (val ms = configuration.multisample) {
        WindowMultisample.SystemDefault -> glfwWindowHint(GLFW_SAMPLES, GLFW_DONT_CARE)
        WindowMultisample.Disabled -> glfwWindowHint(GLFW_SAMPLES, 1)
        is WindowMultisample.SampleCount -> glfwWindowHint(GLFW_SAMPLES, ms.count)
    }

    val version = Driver.glVersion
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, version.majorVersion)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, version.minorVersion)

    if (configuration.alwaysOnTop) {
        glfwWindowHint(GLFW_FLOATING, GLFW_TRUE)
    }


    val displayScale = FloatArray(1)

    if (parentWindow != null) {
        glfwGetWindowContentScale(parentWindow, displayScale, null)
    } else {
        glfwGetMonitorContentScale(glfwGetPrimaryMonitor(), displayScale, null)
    }

    logger.debug { "primary display content scale: ${displayScale[0]}" }

    val adjustedWidth =
        if (fixWindowSize) (displayScale[0] * configuration.width).toInt() else configuration.width
    val adjustedHeight =
        if (fixWindowSize) (displayScale[0] * configuration.height).toInt() else configuration.height

    logger.debug { "adjusted width x height $adjustedWidth x $adjustedHeight" }

    val childWindow = glfwCreateWindow(
        adjustedWidth,
        adjustedHeight,
        configuration.title,
        MemoryUtil.NULL,
        primaryWindow
    )

    logger.debug { "created child window $childWindow" }

    glfwMakeContextCurrent(childWindow)
    glfwShowWindow(childWindow)
    glfwSwapBuffers(childWindow)

    val window = ApplicationWindowGLFW(
        application,
        childWindow,
        configuration.title,
        windowResizable = configuration.resizable,
        windowMultisample = configuration.multisample,
        windowClosable = configuration.closable,
        program
    )

    window.setDefaultIcon()
    return window
}