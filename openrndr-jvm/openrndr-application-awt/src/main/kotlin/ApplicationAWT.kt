package org.openrndr.awt

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.openrndr.*
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.Clock
import org.openrndr.draw.Drawer
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.ProgramRenderTargetGL3
import org.openrndr.internal.gl3.glViewport
import org.openrndr.math.Vector2
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import java.awt.event.*
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import java.awt.event.KeyEvent as AwtKeyEvent
import java.awt.event.MouseEvent as AwtMouseEvent
import org.openrndr.WindowEvent as ProgramWindowEvent

private val logger = KotlinLogging.logger { }

/**
 * The application of a program that is drawn in an AWT component. The program is set up, drawn and receives its
 * events on the render thread of the AWT back-end.
 *
 * @param standalone true when the application shows its program in its own frame, false when it is embedded in a
 * component of another user interface
 */
class ApplicationAWT internal constructor(
    override var program: Program,
    override var configuration: Configuration,
    private val standalone: Boolean
) : Application() {

    init {
        AWTBackend.initialize()
        program.application = this
    }

    internal val surface: CanvasSurface = if (AWTBackend.angle) {
        AngleCanvas(configuration.vsync)
    } else {
        GLCanvas(glData(configuration.vsync, configuration.multisample))
    }

    private val canvas: Canvas get() = surface.canvas
    private var frame: JFrame? = null

    /** events from AWT's event thread, delivered on the render thread */
    private val events = ConcurrentLinkedQueue<() -> Unit>()

    private var setUp = false
    private lateinit var defaultRenderTarget: ProgramRenderTargetGL3

    @Volatile
    private var exitRequested = false
    private val ended = CountDownLatch(1)
    private var registered = false
    private val startTime = System.nanoTime()

    @Volatile
    private var screenPosition = Vector2.ZERO
    private var lastMousePosition = Vector2.ZERO
    private val pressedKeys = mutableSetOf<Int>()
    private val keyboardDriver = KeyboardDriverAWT()

    init {
        canvas.isFocusable = true
        canvas.focusTraversalKeysEnabled = false
        canvas.preferredSize = Dimension(configuration.width, configuration.height)
        installListeners()
    }

    /** starts drawing the program on the render thread, once */
    internal fun start() {
        if (!registered) {
            registered = true
            AWTRenderLoop.add(this)
        }
    }

    /**
     * Draws a frame of the program, sets the program up first. Called by the render thread.
     * @return true when a frame was drawn
     */
    internal fun frame(): Boolean {
        if (exitRequested) {
            end()
            return false
        }
        return surface.render {
            AWTBackend.ensureDriver()
            if (!setUp) {
                setupProgram()
            }
            drawProgram()
        }
    }

    /** ends the program after it failed */
    internal fun fail(e: Throwable) {
        exitRequested = true
        end()
    }

    private fun setupProgram() {
        setUp = true
        Program.active = program
        program.driver = Driver.instance
        program.drawer = Drawer(Driver.instance)
        if (standalone) {
            setupPreload(program, configuration)
        }
        if (clockOwner == null) {
            clockOwner = this
            Animatable.clock(object : Clock {
                override val time: Long
                    get() = (program.seconds * 1E3).toLong()
                override val timeNanos: Long
                    get() = (program.seconds * 1E9).toLong()
            })
        }
        defaultRenderTarget = ProgramRenderTargetGL3(program)
        defaultRenderTarget.bind()
        setupSizes()
        @Suppress("DEPRECATION")
        program.drawer.reset()
        program.drawer.ortho()
        runBlocking {
            program.setup()
        }
    }

    private fun drawProgram() {
        Program.active = program
        defaultRenderTarget.bind()
        setupSizes()
        while (true) {
            val event = events.poll() ?: break
            event()
        }
        deliverEvents()
        if (exitRequested) return
        @Suppress("DEPRECATION")
        program.drawer.reset()
        program.drawer.ortho()
        program.dispatcher.execute()
        program.drawImpl()
    }

    /** delivers the postponed events of the program, in the order of the other back-ends */
    private fun deliverEvents() {
        program.window.drop.deliver()
        program.window.sized.deliver()
        program.window.unfocused.deliver()
        program.window.focused.deliver()
        program.keyboard.keyDown.deliver()
        program.keyboard.keyUp.deliver()
        program.keyboard.keyRepeat.deliver()
        program.keyboard.character.deliver()
        program.mouse.entered.deliver()
        program.mouse.moved.deliver()
        program.mouse.scrolled.deliver()
        program.mouse.buttonDown.deliver()
        program.mouse.dragged.deliver()
        program.mouse.buttonUp.deliver()
        program.mouse.exited.deliver()
    }

    private fun setupSizes() {
        val framebufferWidth = surface.pixelWidth
        val framebufferHeight = surface.pixelHeight
        val width = canvas.width.coerceAtLeast(1)
        windowContentScale = framebufferWidth.toDouble() / width
        glViewport(0, 0, framebufferWidth, framebufferHeight)
        program.width = canvas.width
        program.height = canvas.height
    }

    /** ends the program on the render thread: shuts its extensions down and stops drawing it */
    private fun end() {
        if (ended.count == 0L) return
        AWTRenderLoop.remove(this)
        if (setUp) {
            val shutdown = {
                Program.active = program
                for (extension in program.extensions) {
                    extension.shutdown(program)
                }
                program.ended.trigger(ProgramEvent(ProgramEventType.ENDED))
            }
            if (!surface.render { shutdown() }) {
                shutdown()
            }
        }
        if (clockOwner === this) {
            clockOwner = null
        }
        ended.countDown()
        frame?.let { SwingUtilities.invokeLater { it.dispose() } }
    }

    private fun deliver(event: () -> Unit) {
        events.add(event)
    }

    private fun installListeners() {
        canvas.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: AwtMouseEvent) {
                canvas.requestFocusInWindow()
                val position = e.position
                val button = e.mouseButton
                val modifiers = modifiersOf(e)
                deliver {
                    cursorPosition = position
                    program.mouse.buttonDown.trigger(
                        MouseEvent(position, Vector2.ZERO, Vector2.ZERO, MouseEventType.BUTTON_DOWN, button, modifiers)
                    )
                }
            }

            override fun mouseReleased(e: AwtMouseEvent) {
                val position = e.position
                val button = e.mouseButton
                val modifiers = modifiersOf(e)
                deliver {
                    cursorPosition = position
                    program.mouse.buttonUp.trigger(
                        MouseEvent(position, Vector2.ZERO, Vector2.ZERO, MouseEventType.BUTTON_UP, button, modifiers)
                    )
                }
            }

            override fun mouseEntered(e: AwtMouseEvent) {
                val position = e.position
                deliver {
                    program.mouse.entered.trigger(
                        MouseEvent(position, Vector2.ZERO, Vector2.ZERO, MouseEventType.ENTERED, MouseButton.NONE, emptySet())
                    )
                }
            }

            override fun mouseExited(e: AwtMouseEvent) {
                val position = e.position
                deliver {
                    program.mouse.exited.trigger(
                        MouseEvent(position, Vector2.ZERO, Vector2.ZERO, MouseEventType.EXITED, MouseButton.NONE, emptySet())
                    )
                }
            }
        })

        canvas.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: AwtMouseEvent) = moved(e, MouseButton.NONE)
            override fun mouseDragged(e: AwtMouseEvent) = moved(e, e.mouseButton)

            /** the position of the last move, AWT on macOS sends each move twice */
            private var lastPosition: Vector2? = null

            private fun moved(e: AwtMouseEvent, button: MouseButton) {
                val position = e.position
                if (position == lastPosition) return
                lastPosition = position
                val modifiers = modifiersOf(e)
                deliver {
                    val displacement = position - lastMousePosition
                    lastMousePosition = position
                    cursorPosition = position
                    program.mouse.moved.trigger(
                        MouseEvent(position, Vector2.ZERO, displacement, MouseEventType.MOVED, MouseButton.NONE, modifiers)
                    )
                    if (button != MouseButton.NONE) {
                        program.mouse.dragged.trigger(
                            MouseEvent(position, Vector2.ZERO, displacement, MouseEventType.DRAGGED, button, modifiers)
                        )
                    }
                }
            }
        })

        canvas.addMouseWheelListener { e ->
            val position = e.position
            val modifiers = modifiersOf(e)
            // AWT rotates towards the user, OPENRNDR away from the user, shift scrolls horizontally
            val amount = -e.preciseWheelRotation
            val rotation = if (e.isShiftDown) Vector2(amount, 0.0) else Vector2(0.0, amount)
            deliver {
                program.mouse.scrolled.trigger(
                    MouseEvent(position, rotation, Vector2.ZERO, MouseEventType.SCROLLED, MouseButton.NONE, modifiers)
                )
            }
        }

        canvas.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: AwtKeyEvent) {
                val code = e.keyCode
                val name = keyboardDriver.keyName(e)
                val modifiers = modifiersOf(e)
                deliver {
                    if (pressedKeys.add(code)) {
                        program.keyboard.keyDown.trigger(KeyEvent(KeyEventType.KEY_DOWN, code, name, modifiers))
                    } else {
                        program.keyboard.keyRepeat.trigger(KeyEvent(KeyEventType.KEY_REPEAT, code, name, modifiers))
                    }
                }
            }

            override fun keyReleased(e: AwtKeyEvent) {
                val code = e.keyCode
                val name = keyboardDriver.keyName(e)
                val modifiers = modifiersOf(e)
                deliver {
                    pressedKeys.remove(code)
                    program.keyboard.keyUp.trigger(KeyEvent(KeyEventType.KEY_UP, code, name, modifiers))
                }
            }

            override fun keyTyped(e: AwtKeyEvent) {
                val character = e.keyChar
                if (character == AwtKeyEvent.CHAR_UNDEFINED || character.isISOControl()) return
                val modifiers = modifiersOf(e)
                deliver {
                    program.keyboard.character.trigger(CharacterEvent(character, modifiers))
                }
            }
        })

        canvas.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) {
                deliver {
                    program.window.focused.trigger(ProgramWindowEvent(WindowEventType.FOCUSED, screenPosition, windowSize, true))
                }
            }

            override fun focusLost(e: FocusEvent) {
                deliver {
                    pressedKeys.clear()
                    program.window.unfocused.trigger(ProgramWindowEvent(WindowEventType.UNFOCUSED, screenPosition, windowSize, false))
                }
            }
        })

        canvas.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                val size = Vector2(canvas.width.toDouble(), canvas.height.toDouble())
                deliver {
                    program.window.sized.trigger(ProgramWindowEvent(WindowEventType.RESIZED, screenPosition, size, true))
                }
            }

            override fun componentMoved(e: ComponentEvent) = updateScreenPosition()
            override fun componentShown(e: ComponentEvent) = updateScreenPosition()
        })
        canvas.addHierarchyBoundsListener(object : HierarchyBoundsAdapter() {
            override fun ancestorMoved(e: HierarchyEvent) = updateScreenPosition()
        })

        canvas.dropTarget = DropTarget(canvas, object : DropTargetAdapter() {
            override fun drop(e: DropTargetDropEvent) {
                if (!e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    e.rejectDrop()
                    return
                }
                e.acceptDrop(DnDConstants.ACTION_COPY)
                @Suppress("UNCHECKED_CAST")
                val files = (e.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>).map { it.absolutePath }
                val position = Vector2(e.location.x.toDouble(), e.location.y.toDouble())
                e.dropComplete(true)
                deliver {
                    program.window.drop.trigger(DropEvent(position, files))
                }
            }
        })
    }

    /** the position of the canvas on the screen, read on the event thread, the render thread does not lock AWT */
    private fun updateScreenPosition() {
        if (canvas.isShowing) {
            val location = canvas.locationOnScreen
            screenPosition = Vector2(location.x.toDouble(), location.y.toDouble())
        }
    }

    private val AwtMouseEvent.position: Vector2 get() = Vector2(x.toDouble(), y.toDouble())

    private val AwtMouseEvent.mouseButton: MouseButton
        get() = when {
            SwingUtilities.isLeftMouseButton(this) -> MouseButton.LEFT
            SwingUtilities.isRightMouseButton(this) -> MouseButton.RIGHT
            SwingUtilities.isMiddleMouseButton(this) -> MouseButton.CENTER
            else -> MouseButton.NONE
        }

    override suspend fun setup() {
        if (!standalone) return
        SwingUtilities.invokeAndWait {
            val frame = JFrame(configuration.title)
            frame.defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
            frame.addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: java.awt.event.WindowEvent) {
                    exit()
                }
            })
            frame.isResizable = configuration.windowResizable
            frame.isAlwaysOnTop = configuration.windowAlwaysOnTop
            frame.isUndecorated = configuration.hideWindowDecorations
            frame.contentPane.add(canvas)
            frame.pack()
            val position = configuration.position
            if (position != null) {
                frame.setLocation(position.x, position.y)
            } else {
                frame.setLocationRelativeTo(null)
            }
            frame.isVisible = true
            canvas.requestFocusInWindow()
            this.frame = frame
        }
        start()
    }

    override fun loop() {
        ended.await()
    }

    override fun requestDraw() {}

    override fun requestFocus() {
        SwingUtilities.invokeLater { canvas.requestFocusInWindow() }
    }

    override fun exit() {
        exitRequested = true
    }

    override fun windowClose() = exit()

    override fun windowMinimize() {
        SwingUtilities.invokeLater { frame?.extendedState = Frame.ICONIFIED }
    }

    override fun windowMaximize() {
        SwingUtilities.invokeLater { frame?.extendedState = Frame.MAXIMIZED_BOTH }
    }

    override fun windowFullscreen(mode: Fullscreen) {
        logger.warn { "fullscreen windows are not supported by the AWT application back-end" }
    }

    override var clipboardContents: String?
        get() = try {
            Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String
        } catch (e: Exception) {
            null
        }
        set(value) {
            if (value != null) {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
            }
        }

    private var title = configuration.title
    override var windowTitle: String
        get() = title
        set(value) {
            title = value
            SwingUtilities.invokeLater { frame?.title = value }
        }

    override var windowPosition: Vector2
        get() = screenPosition
        set(value) {
            SwingUtilities.invokeLater { frame?.setLocation(value.x.toInt(), value.y.toInt()) }
        }

    override var windowSize: Vector2
        get() = Vector2(canvas.width.toDouble(), canvas.height.toDouble())
        set(value) {
            SwingUtilities.invokeLater {
                canvas.preferredSize = Dimension(value.x.toInt(), value.y.toInt())
                frame?.pack()
                canvas.parent?.revalidate()
            }
        }

    /** the content scale follows the display of the canvas, it is set on each frame */
    override var windowContentScale: Double = 1.0

    override var windowMultisample: WindowMultisample
        get() = configuration.multisample
        set(value) {
            logger.warn { "the multisampling of an AWT canvas can not be changed" }
        }

    override var windowResizable: Boolean
        get() = frame?.isResizable ?: false
        set(value) {
            SwingUtilities.invokeLater { frame?.isResizable = value }
        }

    override var windowHitTest: ((Vector2) -> Hit)?
        get() = null
        set(value) {
            logger.warn { "window hit tests are not supported by the AWT application back-end" }
        }

    override var cursorPosition: Vector2 = Vector2.ZERO
        set(value) {
            if (field != value && Thread.currentThread().name != "openrndr-awt-render") {
                logger.warn { "the cursor can not be moved by the AWT application back-end" }
            }
            field = value
        }

    override var cursorVisible: Boolean = true
        set(value) {
            field = value
            updateCursor()
        }

    override var cursorHideMode: MouseCursorHideMode = MouseCursorHideMode.HIDE

    override var cursorType: CursorType = CursorType.ARROW_CURSOR
        set(value) {
            field = value
            updateCursor()
        }

    private fun updateCursor() {
        val cursor = if (!cursorVisible) blankCursor else Cursor.getPredefinedCursor(
            when (cursorType) {
                CursorType.ARROW_CURSOR -> Cursor.DEFAULT_CURSOR
                CursorType.IBEAM_CURSOR -> Cursor.TEXT_CURSOR
                CursorType.CROSSHAIR_CURSOR -> Cursor.CROSSHAIR_CURSOR
                CursorType.HAND_CURSOR -> Cursor.HAND_CURSOR
                CursorType.HRESIZE_CURSOR -> Cursor.E_RESIZE_CURSOR
                CursorType.VRESIZE_CURSOR -> Cursor.N_RESIZE_CURSOR
            }
        )
        SwingUtilities.invokeLater { canvas.cursor = cursor }
    }

    override val seconds: Double
        get() = (System.nanoTime() - startTime) / 1E9

    override var presentationMode: PresentationMode = PresentationMode.AUTOMATIC
        set(value) {
            if (value != PresentationMode.AUTOMATIC) {
                logger.warn { "the AWT application back-end only supports automatic presentation" }
            }
        }

    override fun createChildWindow(configuration: WindowConfiguration, program: Program): ApplicationWindow {
        error("child windows are not supported by the AWT application back-end, use more program panels")
    }

    private companion object {
        /** the application whose program drives the animation clock */
        var clockOwner: ApplicationAWT? = null

        val blankCursor: Cursor by lazy {
            Toolkit.getDefaultToolkit().createCustomCursor(
                BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), Point(0, 0), "blank"
            )
        }
    }
}
