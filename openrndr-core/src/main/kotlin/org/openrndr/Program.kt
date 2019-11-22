@file:Suppress("unused")

package org.openrndr

import kotlinx.coroutines.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2

import java.io.File
import kotlin.coroutines.CoroutineContext


enum class WindowEventType {
    MOVED,
    RESIZED,
    FOCUSED,
    UNFOCUSED,
    MINIMIZED,
    RESTORED,
}

class WindowEvent(val type: WindowEventType, val position: Vector2, val size: Vector2, val focused: Boolean)

class DropEvent(val position: Vector2, val files: List<File>)


/**
The Program class, this is where most user implementations start
 **/
@ApplicationDslMarker
open class Program {

    var width = 0
    var height = 0

    lateinit var drawer: Drawer
    lateinit var driver: Driver

    lateinit var application: Application

    var backgroundColor: ColorRGBa? = ColorRGBa.BLACK
    val dispatcher = Dispatcher()

    /**
     * clock function. defaults to returning the application time.
     */
    var clock = { application.seconds }

    private var frameSeconds = 0.0
    private var deltaSeconds: Double = 0.0
    private var lastSeconds: Double = -1.0

    var frameCount = 0
        private set

    /**
     * The number of [seconds] since program start, or the time from a custom [clock].
     * value is updated at the beginning of the frame only.
     */
    val seconds: Double
        get() = frameSeconds

    /**
     * The elapsed time since the last draw loop
     */
    val deltaTime: Double
        get() = deltaSeconds

    inner class Clipboard {
        var contents: String?
            get() {
                return application.clipboardContents
            }
            set(value) {
                application.clipboardContents = value
            }
    }

    val clipboard = Clipboard()
    val extensions = mutableListOf<Extension>()

    /**
     * Install an [Extension].
     */
    fun extend(extension: Extension): Extension {
        extensions.add(extension)
        extension.setup(this)
        return extension
    }

    /**
     * Install an [Extension] and configure it
     */
    fun <T : Extension> extend(extension: T, configure: T.() -> Unit): Extension {
        extensions.add(extension)
        extension.configure()
        extension.setup(this)
        return extension
    }

    /**
     * Install an extension function for the given [ExtensionStage]
     */
    fun extend(stage: ExtensionStage = ExtensionStage.BEFORE_DRAW, userDraw: Program.() -> Unit) {
        val functionExtension = when (stage) {
            ExtensionStage.SETUP ->
                object : Extension {
                    override var enabled: Boolean = true
                    override fun setup(program: Program) {
                        program.userDraw()
                    }
                }
            ExtensionStage.BEFORE_DRAW ->
                object : Extension {
                    override var enabled: Boolean = true
                    override fun beforeDraw(drawer: Drawer, program: Program) {
                        program.userDraw()
                    }
                }
            ExtensionStage.AFTER_DRAW ->
                object : Extension {
                    override var enabled: Boolean = true
                    override fun afterDraw(drawer: Drawer, program: Program) {
                        program.userDraw()
                    }
                }
        }
        extensions.add(functionExtension)
    }


    /**
     * Simplified window interface
     */
    inner class Window {

        var title: String
            get() = application.windowTitle
            set(value) {
                application.windowTitle = value
            }

        var size = Vector2(0.0, 0.0)
        var scale = Vector2(1.0, 1.0)

        var presentationMode: PresentationMode
            get() = application.presentationMode
            set(value) {
                application.presentationMode = value
            }


        fun requestFocus() = application.requestFocus()

        fun requestDraw() = application.requestDraw()

        /**
         * Window focused event, triggered when the window receives focus
         */
        val focused = Event<WindowEvent>().postpone(true)

        /**
         * Window focused event, triggered when the window loses focus
         */
        val unfocused = Event<WindowEvent>().postpone(true)

        /**
         * Window moved event
         */
        val moved = Event<WindowEvent>().postpone(true)

        /**
         * Window sized event
         */
        val sized = Event<WindowEvent>().postpone(true)


        /**
         * Window minimized event
         */
        val minimized = Event<WindowEvent>().postpone(true)


        /**
         * Window restored (from minimization) event
         */
        val restored = Event<WindowEvent>().postpone(true)

        /**
         * Drop event, triggered when a file is dropped on the window
         */
        val drop = Event<DropEvent>().postpone(true)

        /**
         * Window position
         */
        var position: Vector2
            get() = application.windowPosition
            set(value) {
                application.windowPosition = value
            }


    }

    val window = Window()

    class CharacterEvent(val character: Char, val modifiers: Set<KeyboardModifier>, var propagationCancelled: Boolean = false) {
        fun cancelPropagation() {
            propagationCancelled = true
        }
    }

    val keyboard by lazy { Keyboard() }
    val mouse by lazy { Mouse(application) }

    /**
     * This is ran exactly once before the first call to draw()
     */
    open fun setup() {}

    /**
     * This is the draw call that is called by Application. It takes care of handling extensions.
     */
    fun drawImpl() {
        frameSeconds = clock()

        if (lastSeconds == -1.0) lastSeconds = seconds

        deltaSeconds = frameSeconds - lastSeconds
        lastSeconds = frameSeconds

        frameCount++

        backgroundColor?.let {
            drawer.background(it)
        }
        extensions.filter { it.enabled }.forEach { it.beforeDraw(drawer, this) }
        draw()
        extensions.reversed().filter { it.enabled }.forEach { it.afterDraw(drawer, this) }
    }

    /**
     * This is the user facing draw call. It should be overridden by the user.
     */
    open fun draw() {}

}

fun Program.launch(
        context: CoroutineContext = dispatcher,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
): Job = GlobalScope.launch(context, start, block)
