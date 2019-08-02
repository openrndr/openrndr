package org.openrndr.internal.gl3

import mu.KotlinLogging
import org.lwjgl.BufferUtils

import java.lang.IllegalStateException
import java.lang.RuntimeException
import java.io.File

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryUtil.NULL

import org.lwjgl.system.MemoryStack.stackPush

import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFW.glfwSetWindowPos
import org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor
import org.lwjgl.glfw.GLFW.glfwGetVideoMode
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose
import org.lwjgl.glfw.GLFW.GLFW_RELEASE
import org.lwjgl.glfw.GLFW.glfwSetKeyCallback
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.opengl.GLUtil
import org.lwjgl.opengl.GL.createCapabilities
import org.lwjgl.opengl.GL30.*

import org.openrndr.*
import org.openrndr.draw.Drawer
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2
import org.openrndr.WindowMultisample.*
import java.nio.Buffer
import java.nio.ByteBuffer

import java.util.*


private val logger = KotlinLogging.logger {}
internal var primaryWindow: Long = NULL

class ApplicationGLFWGL3(private val program: Program, private val configuration: Configuration) : Application() {
    private var windowFocused = true
    private var window: Long = NULL
    private var driver: DriverGL3
    private var realWindowTitle = configuration.title
    private var exitRequested = false
    private val fixWindowSize = System.getProperty("os.name").contains("windows", true)
    private var setupCalled = false
    override var presentationMode: PresentationMode = PresentationMode.AUTOMATIC

    private var realCursorPosition = Vector2(0.0, 0.0)

    override var cursorPosition: Vector2
        get() {
            return realCursorPosition
        }
        set(value) {
            realCursorPosition = value
            glfwSetCursorPos(window, value.x, value.y)
        }

    private var realCursorVisible = true
    override var cursorVisible: Boolean
    get() {
        return realCursorVisible
    }
    set(value) {
        if (value) {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL)
        } else {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED)
        }
    }

    override var windowPosition: Vector2
        get() {
            val x = IntArray(1)
            val y = IntArray(1)
            glfwGetWindowPos(window, x, y)
            return Vector2(
                    if (fixWindowSize) (x[0].toDouble() / program.window.scale.x) else x[0].toDouble(),
                    if (fixWindowSize) (y[0].toDouble() / program.window.scale.y) else y[0].toDouble())
        }
        set(value) {
            glfwSetWindowPos(window,
                    if (fixWindowSize) (value.x * program.window.scale.x).toInt() else value.x.toInt(),
                    if (fixWindowSize) (value.y * program.window.scale.y).toInt() else value.y.toInt())
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
        logger.debug { "debug output enabled" }
        logger.trace { "trace level enabled" }

        driver = DriverGL3()
        Driver.driver = driver
        program.application = this
        createPrimaryWindow()
    }

    override fun setup() {
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, if (configuration.windowResizable) GLFW_TRUE else GLFW_FALSE)
        glfwWindowHint(GLFW_DECORATED, if (configuration.hideWindowDecorations) GLFW_FALSE else GLFW_TRUE)

        when (val c = configuration.multisample) {
            is SampleCount -> glfwWindowHint(GLFW_SAMPLES, c.count)
            SystemDefault -> glfwWindowHint(GLFW_SAMPLES, GLFW_DONT_CARE)
            Disabled -> glfwWindowHint(GLFW_SAMPLES, 0)
        }


        glfwWindowHint(GLFW_RED_BITS, 8)
        glfwWindowHint(GLFW_GREEN_BITS, 8)
        glfwWindowHint(GLFW_BLUE_BITS, 8)
        glfwWindowHint(GLFW_STENCIL_BITS, 8)
        glfwWindowHint(GLFW_DEPTH_BITS, 24)

        if (configuration.windowAlwaysOnTop) {
            glfwWindowHint(GLFW_FLOATING, GLFW_TRUE)
        }

        println(glfwGetVersionString())

        if (useDebugContext) {
            glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE)
        }

        val xscale = FloatArray(1)
        val yscale = FloatArray(1)
        glfwGetMonitorContentScale(glfwGetPrimaryMonitor(), xscale, yscale)

        if (configuration.fullscreen == Fullscreen.SET_DISPLAY_MODE) {
            xscale[0] = 1.0f
            yscale[0] = 1.0f
        }

        logger.debug { "content scale ${xscale[0]} ${yscale[0]}" }
        program.window.scale = Vector2(xscale[0].toDouble(), yscale[0].toDouble())

        logger.debug { "creating window" }
        window = if (configuration.fullscreen == Fullscreen.DISABLED) {
            val adjustedWidth = if (fixWindowSize) (xscale[0] * configuration.width).toInt() else configuration.width
            val adjustedHeight = if (fixWindowSize) (yscale[0] * configuration.height).toInt() else configuration.height

            glfwCreateWindow(adjustedWidth,
                    adjustedHeight,
                    configuration.title, NULL, primaryWindow)
        } else {
            logger.info { "creating fullscreen window" }

            var requestWidth = configuration.width
            var requestHeight = configuration.height

            if (configuration.fullscreen == Fullscreen.CURRENT_DISPLAY_MODE) {
                val mode = glfwGetVideoMode(glfwGetPrimaryMonitor())
                if (mode != null) {
                    requestWidth = mode.width()
                    requestHeight = mode.height()
                } else {
                    throw RuntimeException("failed to determine current video mode")
                }
            }

            glfwCreateWindow(requestWidth,
                    requestHeight,
                    configuration.title, glfwGetPrimaryMonitor(), primaryWindow)
        }

        val buf = BufferUtils.createByteBuffer(128 * 128 * 4)
        (buf as Buffer).rewind()
        for (y in 0 until 128) {
            for (x in 0 until 128) {
                buf.putInt(0xffc0cbff.toInt())
            }
        }
        (buf as Buffer).flip()

        stackPush().use {
            glfwSetWindowIcon(window, GLFWImage.mallocStack(1, it)
                    .width(128)
                    .height(128)
                    .pixels(buf)
            );
        }

        logger.debug { "window created: $window" }

        if (window == NULL) {
            throw RuntimeException("Failed to create the GLFW window")
        }

        // Get the thread stack and push a new frame
        stackPush().let { stack ->
            val pWidth = stack.mallocInt(1) // int*
            val pHeight = stack.mallocInt(1) // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight)

            // Get the resolution of the primary monitor
            val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor())

            if (configuration.position == null) {
                if (vidmode != null) {
                    // Center the window
                    glfwSetWindowPos(
                            window,
                            (vidmode.width() - pWidth.get(0)) / 2,
                            (vidmode.height() - pHeight.get(0)) / 2
                    )
                }
            } else {
                configuration.position?.let {
                    glfwSetWindowPos(window,
                            it.x,
                            it.y)
                }
            }
            Unit
        }

        logger.debug { "making context current" }
        glfwMakeContextCurrent(window)

        val enableTearControl = false
        if (enableTearControl && (glfwExtensionSupported("GLX_EXT_swap_control_tear") || glfwExtensionSupported("WGL_EXT_swap_control_tear"))) {
            glfwSwapInterval(-1)
        } else {
            glfwSwapInterval(1)
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
            logger.debug { "resizing window to ${width}x${height} " }

            if (readyFrames > 0) {
                setupSizes()
                program.window.sized.trigger(WindowEvent(WindowEventType.RESIZED, program.window.position, program.window.size, true))
            }

            readyFrames++
            logger.debug { "all ok" }
        }

        glfwSetWindowPosCallback(window) { _, x, y ->
            logger.debug { "window has moved to $x $y" }
            program.window.moved.trigger(WindowEvent(WindowEventType.MOVED, Vector2(x.toDouble(), y.toDouble()), Vector2(0.0, 0.0), true))
        }

        glfwSetWindowFocusCallback(window) { _, focused ->
            logger.debug { "window focus has changed; focused=$focused" }
            windowFocused = focused
            if (focused) {
                program.window.focused.trigger(
                        WindowEvent(WindowEventType.FOCUSED, program.window.position, program.window.size, true))
            } else {
                program.window.unfocused.trigger(
                        WindowEvent(WindowEventType.FOCUSED, program.window.position, program.window.size, false))
            }
        }
        logger.debug { "glfw version: ${glfwGetVersionString()}" }
        logger.debug { "showing window" }


        run {
            val adjustedMinimumWidth = if (fixWindowSize) (xscale[0] * configuration.minimumWidth).toInt() else configuration.minimumWidth
            val adjustedMinimumHeight = if (fixWindowSize) (yscale[0] * configuration.minimumHeight).toInt() else configuration.minimumHeight

            val adjustedMaximumWidth = if (fixWindowSize && configuration.maximumWidth != Int.MAX_VALUE) (xscale[0] * configuration.maximumWidth).toInt() else configuration.maximumWidth
            val adjustedMaximumHeight = if (fixWindowSize  && configuration.maximumHeight != Int.MAX_VALUE) (yscale[0] * configuration.maximumHeight).toInt() else configuration.maximumHeight

            glfwSetWindowSizeLimits(window, adjustedMinimumWidth, adjustedMinimumHeight, adjustedMaximumWidth, adjustedMaximumHeight)
        }

        glfwShowWindow(window)
    }

    private fun createPrimaryWindow() {
        if (primaryWindow == NULL) {
            glfwSetErrorCallback(GLFWErrorCallback.create { error, description ->
                logger.error(
                        "LWJGL Error - Code: {}, Description: {}",
                        Integer.toHexString(error),
                        GLFWErrorCallback.getDescription(description)
                )
            })
            if (!glfwInit()) {
                throw IllegalStateException("Unable to initialize GLFW")
            }

            glfwDefaultWindowHints()
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE)
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
            glfwWindowHint(GLFW_RED_BITS, 8)
            glfwWindowHint(GLFW_GREEN_BITS, 8)
            glfwWindowHint(GLFW_BLUE_BITS, 8)
            glfwWindowHint(GLFW_STENCIL_BITS, 8)
            glfwWindowHint(GLFW_DEPTH_BITS, 24)
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
            primaryWindow = glfwCreateWindow(640, 480, "OPENRNDR primary window", NULL, NULL)
        }
    }

    private val vaos = IntArray(1)

    fun preloop() {
        createCapabilities()

        if (useDebugContext) {
            GLUtil.setupDebugMessageCallback()
        }

        driver = DriverGL3()
        program.driver = driver
        program.drawer = Drawer(driver)

        val defaultRenderTarget = ProgramRenderTargetGL3(program)
        defaultRenderTarget.bind()

        setupSizes()
        program.drawer.ortho()
    }

    private var drawRequested = true

    override fun loop() {
        logger.debug { "starting loop" }
        preloop()

        var lastDragPosition = Vector2.ZERO
        var globalModifiers = setOf<KeyboardModifier>()

        glfwSetKeyCallback(window) { _, key, scancode, action, mods ->
            val modifiers = modifierSet(mods)
            val name = glfwGetKeyName(key, scancode) ?: "<null>"

            globalModifiers = modifiers
            when (action) {
                GLFW_PRESS -> program.keyboard.keyDown.trigger(KeyEvent(KeyEventType.KEY_DOWN, key, scancode, name, modifiers))
                GLFW_RELEASE -> program.keyboard.keyUp.trigger(KeyEvent(KeyEventType.KEY_UP, key, scancode, name, modifiers))
                GLFW_REPEAT -> program.keyboard.keyRepeat.trigger(KeyEvent(KeyEventType.KEY_REPEAT, key, scancode, name, modifiers))
            }
        }

        glfwSetCharCallback(window) { window, codepoint ->
            program.keyboard.character.trigger(Program.CharacterEvent(codepoint.toChar(), emptySet()))
        }

        glfwSetDropCallback(window) { _, count, names ->
            logger.debug { "$count file(s) have been dropped" }
            val pointers = PointerBuffer.create(names, count)
            val files = (0 until count).map {
                File(pointers.getStringUTF8(it))
            }
            program.window.drop.trigger(DropEvent(program.mouse.position, files))
        }

        var down = false
        glfwSetScrollCallback(window) { _, xoffset, yoffset ->
            program.mouse.scrolled.trigger(MouseEvent(program.mouse.position, Vector2(xoffset, yoffset), Vector2.ZERO, MouseEventType.SCROLLED, MouseButton.NONE, globalModifiers))
        }

        glfwSetWindowIconifyCallback(window) { _, iconified ->
            if (iconified) {
                program.window.minimized.trigger(WindowEvent(WindowEventType.MINIMIZED, Vector2.ZERO, Vector2.ZERO, false))
            } else {
                program.window.restored.trigger(WindowEvent(WindowEventType.RESTORED,  program.window.position, program.window.size, true))
            }
        }

        glfwSetMouseButtonCallback(window) { _, button, action, mods ->
            val mouseButton = when (button) {
                GLFW_MOUSE_BUTTON_LEFT -> MouseButton.LEFT
                GLFW_MOUSE_BUTTON_RIGHT -> MouseButton.RIGHT
                GLFW_MOUSE_BUTTON_MIDDLE -> MouseButton.CENTER
                else -> MouseButton.NONE
            }

            val modifiers = mutableSetOf<KeyboardModifier>()
            val buttonsDown = BitSet()

            if (mods and GLFW_MOD_SHIFT != 0) {
                modifiers.add(KeyboardModifier.SHIFT)
            }
            if (mods and GLFW_MOD_ALT != 0) {
                modifiers.add(KeyboardModifier.ALT)
            }
            if (mods and GLFW_MOD_CONTROL != 0) {
                modifiers.add(KeyboardModifier.CTRL)
            }
            if (mods and GLFW_MOD_SUPER != 0) {
                modifiers.add(KeyboardModifier.SUPER)
            }

            if (action == GLFW_PRESS) {
                down = true
                lastDragPosition = program.mouse.position
                program.mouse.buttonDown.trigger(
                        MouseEvent(program.mouse.position, Vector2.ZERO, Vector2.ZERO, MouseEventType.BUTTON_DOWN, mouseButton, modifiers)
                )
                buttonsDown.set(button, true)
            }

            if (action == GLFW_RELEASE) {
                down = false
                program.mouse.buttonUp.trigger(
                        MouseEvent(program.mouse.position, Vector2.ZERO, Vector2.ZERO, MouseEventType.BUTTON_UP, mouseButton, modifiers)
                )
                buttonsDown.set(button, false)

                program.mouse.clicked.trigger(
                        MouseEvent(program.mouse.position, Vector2.ZERO, Vector2.ZERO, MouseEventType.CLICKED, mouseButton, modifiers)
                )
            }
        }

        glfwSetCursorPosCallback(window) { _, xpos, ypos ->
            val position = if (fixWindowSize) Vector2(xpos, ypos) / program.window.scale else Vector2(xpos, ypos)
            logger.trace { "mouse moved $xpos $ypos -- $position" }
            realCursorPosition = position
            program.mouse.moved.trigger(MouseEvent(position, Vector2.ZERO, Vector2.ZERO, MouseEventType.MOVED, MouseButton.NONE, globalModifiers))
            if (down) {
                program.mouse.dragged.trigger(MouseEvent(position, Vector2.ZERO, position - lastDragPosition, MouseEventType.DRAGGED, MouseButton.NONE, globalModifiers))
                lastDragPosition = position
            }
        }

        glfwSetCursorEnterCallback(window) { window, entered ->
            logger.debug { "cursor state changed; inside window = $entered" }
        }


        if (configuration.showBeforeSetup) {
            logger.debug { "clearing and displaying pre-setup" }

            // clear the front buffer
            glDepthMask(true)
            glClearColor(0.5f, 0.5f, 0.5f, 0.0f)
            glClear(GL_COLOR_BUFFER_BIT or GL_STENCIL_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            // swap the color buffers
            glfwSwapBuffers(window)

            // clear the back buffer
            glClearColor(0.5f, 0.5f, 0.5f, 0.0f)
            glClear(GL_COLOR_BUFFER_BIT or GL_STENCIL_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
            glDepthMask(false)

            glfwPollEvents()
        }
        logger.debug { "opengl vendor: ${glGetString(GL_VENDOR)}" }
        logger.debug { "opengl version: ${glGetString(GL_VERSION)}" }

        println("OpenGL vendor: ${glGetString(GL_VENDOR)}")
        println("OpenGL version: ${glGetString(GL_VERSION)}")

        if (configuration.hideCursor) {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED)
        }

        logger.debug { "calling program.setup" }
        program.setup()
        setupCalled = true

        if (glfwExtensionSupported("GLX_EXT_swap_control_tear") || glfwExtensionSupported("WGL_EXT_swap_control_tear")) {
            glfwSwapInterval(-1)
        } else {
            glfwSwapInterval(1)
        }

        var exception: Throwable? = null
        while (!exitRequested && !glfwWindowShouldClose(window)) {
            if (presentationMode == PresentationMode.AUTOMATIC || drawRequested) {
                drawRequested = false
                exception = drawFrame()
                if (exception != null) {
                    break
                }
                glfwSwapBuffers(window)
            }

            if (!windowFocused && configuration.unfocusBehaviour == UnfocusBehaviour.THROTTLE) {
                Thread.sleep(100)
            }

            if (presentationMode == PresentationMode.AUTOMATIC) {
                glfwPollEvents()
            } else {
                Thread.sleep(1)
                glfwPollEvents()
                deliverEvents()
                program.dispatcher.pump()
            }
        }
        logger.info { "exiting loop" }

        glfwFreeCallbacks(window)
        glfwDestroyWindow(window)

        // TODO: take care of these when all windows are closed
        //glfwTerminate()
        //glfwSetErrorCallback(null)?.free()
        logger.info { "done" }

        exception?.let {
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
        program.mouse.clicked.deliver()
        program.mouse.buttonDown.deliver()
        program.mouse.buttonUp.deliver()
        program.mouse.dragged.deliver()
    }

    private fun drawFrame(): Throwable? {
        setupSizes()
        glBindVertexArray(vaos[0])
        program.drawer.reset()
        program.drawer.ortho()
        deliverEvents()
        program.dispatcher.pump()
        try {
            logger.trace { "window: ${program.window.size.x.toInt()}x${program.window.size.y.toInt()} program: ${program.width}x${program.height}" }
            program.drawImpl()
        } catch (e: Throwable) {
            logger.error { "caught exception, breaking animation loop" }
            e.printStackTrace()
            return e
        }
        return null
    }

    private fun setupSizes() {
        val wcsx = FloatArray(1)
        val wcsy = FloatArray(1)
        glfwGetWindowContentScale(window, wcsx, wcsy)
        program.window.scale = Vector2(wcsx[0].toDouble(), wcsy[0].toDouble())

        val fbw = IntArray(1)
        val fbh = IntArray(1)
        glfwGetFramebufferSize(window, fbw, fbh)

        glViewport(0, 0, fbw[0], fbh[0])
        program.width = Math.ceil(fbw[0] / program.window.scale.x).toInt()
        program.height = Math.ceil(fbh[0] / program.window.scale.y).toInt()
        program.window.size = Vector2(program.width.toDouble(), program.height.toDouble())
        program.drawer.width = program.width
        program.drawer.height = program.height
    }

    private fun modifierSet(mods: Int): Set<KeyboardModifier> {
        val modifiers = mutableSetOf<KeyboardModifier>()
        if (mods and GLFW_MOD_SHIFT != 0) {
            modifiers.add(KeyboardModifier.SHIFT)
        }
        if (mods and GLFW_MOD_ALT != 0) {
            modifiers.add(KeyboardModifier.ALT)
        }
        if (mods and GLFW_MOD_CONTROL != 0) {
            modifiers.add(KeyboardModifier.CTRL)
        }
        if (mods and GLFW_MOD_SUPER != 0) {
            modifiers.add(KeyboardModifier.SUPER)
        }
        return modifiers
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
