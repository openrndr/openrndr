@file:Suppress("unused")

package org.openrndr

import org.openrndr.animatable.Animatable
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2
import kotlin.jvm.JvmRecord

expect fun rootClassName(): String

/**
 * Represents the types of events that can occur on a window.
 *
 * These events cover common actions and state changes that a window can experience.
 */
enum class WindowEventType {
    MOVED,
    RESIZED,
    FOCUSED,
    UNFOCUSED,
    MINIMIZED,
    RESTORED,
    CLOSED
}

/**
 * window event message
 */
@JvmRecord
data class WindowEvent(val type: WindowEventType, val position: Vector2, val size: Vector2, val focused: Boolean)

/**
 * window drop item event message
 */
@JvmRecord
data class DropEvent(val position: Vector2, val files: List<String>)

/**
 * program event type
 */
enum class ProgramEventType {
    /**
     * indicates the program has ended
     */
    ENDED
}


/**
 * Represents an event occurring within a program.
 *
 * @property type The type of the program event, defined by [ProgramEventType].
 */
@JvmRecord
data class ProgramEvent(val type: ProgramEventType)

@JvmRecord
data class RequestAssetsEvent(val origin: Any, val program: Program)

@JvmRecord
data class ProduceAssetsEvent(val origin: Any, val program: Program, val assetMetadata: AssetMetadata)

@JvmRecord
data class AssetMetadata(
    val programName: String,
    val assetBaseName: String,
    val assetProperties: Map<String, String>
)

/**
 * Interface representing input events in an application.
 * Combines events related to mouse, keyboard, and pointers.
 */
interface InputEvents {
    val mouse: MouseEvents
    val keyboard: KeyEvents
}

/**
 * Interface representing a clipboard for accessing and modifying its contents.
 *
 * The `Clipboard` interface provides a way to interact with the system clipboard.
 * It allows getting and setting of clipboard contents in the form of a string.
 *
 * @property contents The textual contents of the clipboard. Can be set to store a string
 * in the clipboard, or retrieved to access the current contents. If no content exists, it may return null.
 */
interface Clipboard {
    var contents: String?
}

interface Clock {
    val seconds: Double
}

/**
 * Represents a program interface that combines input event handling, extension hosting, and clock functionalities.
 * It provides common properties and methods for managing the lifecycle of a program, drawing operations,
 * and asset management.
 */
interface Program : InputEvents, ExtensionHost, Clock {

    /**
     * A map that can be used to store arbitrary data, including functions
     */
    var userProperties: MutableMap<String, Any>

    var name: String

    var width: Int
    var height: Int
    var isNested: Boolean
    var drawer: Drawer
    var driver: Driver

    val dispatcher: Dispatcher
    val window: Window
    var application: Application

    suspend fun setup()

    fun drawImpl()


    /**
     * The `draw` method is responsible for rendering the current state of the program.
     * This method is called automatically for each frame during the runtime of the program.
     * It serves as the main function to perform all visual updates, including drawing shapes,
     * updating visuals, and handling animations based on the current state.
     *
     * This function is typically implemented to contain logic for rendering content on the screen.
     */
    fun draw()

    /**
     * An event triggered when assets need to be produced or generated.
     *
     * This event is intended for managing asset production workflows in programs. It allows
     * listeners to handle the generation or preparation of assets by receiving a [ProduceAssetsEvent] message.
     * The [ProduceAssetsEvent] contains details about the origin of the event, the associated program instance,
     * and metadata describing the assets to be produced.
     *
     * Usage:
     * - Add listeners to this event to respond to asset production requests.
     * - Listeners can access event details and perform the desired asset production logic.
     */
    val produceAssets: Event<ProduceAssetsEvent>


    /**
     * An event triggered when there is a request for assets to be accessed or provided.
     *
     * This event is designed to facilitate workflows related to asset requests in programs.
     * It enables listeners to handle asset-related requirements by responding to a [RequestAssetsEvent] message.
     * The [RequestAssetsEvent] contains details such as the origin of the event and the associated program instance.
     *
     * Usage:
     * - Listeners can be added to this event to handle asset requests.
     * - Listeners can access event details and implement logic to fulfill asset needs.
     */
    val requestAssets: Event<RequestAssetsEvent>
    var assetMetadata: () -> AssetMetadata
    var assetProperties: MutableMap<String, String>


    var clock: () -> Double
    /**
     * Event that is triggered when the program ends.
     *
     * This variable represents a program lifecycle event and can be used to listen for or handle
     * the termination of the program. Listeners registered to this event will be invoked with
     * a [ProgramEvent] carrying details about the termination.
     */
    var ended: Event<ProgramEvent>
    var backgroundColor: ColorRGBa?
    val frameCount: Int


    /**
     * Represents the clipboard functionality for the application.
     *
     * Provides an interface to access and modify the clipboard's content.
     * The contents can be read from or written to the system clipboard.
     */
    val clipboard: ProgramImplementation.ApplicationClipboard

    fun updateFrameSecondsFromClock()
}

/**
 * Represents a window with configurable properties and behavior.
 */
interface Window {
    var title: String
    var size: Vector2
    var contentScale: Double
    var presentationMode: PresentationMode
    var multisample: WindowMultisample
    var resizable: Boolean

    /**
     * Requests focus for the window, making it the active window.
     *
     * This method ensures that the window receives input events such as
     * keyboard and mouse interactions. The focus request may not always
     * be successful, depending on the underlying operating system and
     * application state.
     */
    fun requestFocus()

    /**
     * Requests a redraw of the window's contents.
     *
     * This method can be called manually when the window's content needs to
     * be updated outside of the main rendering loop. It signals the framework
     * that a new frame should be rendered.
     */
    fun requestDraw()

    /**
     * Window focused event, triggered when the window receives focus
     */
    val focused: Event<WindowEvent>

    /**
     * Window focused event, triggered when the window loses focus
     */
    val unfocused: Event<WindowEvent>

    /**
     * Window moved event
     */
    val moved: Event<WindowEvent>

    /**
     * Window sized event
     */
    val sized: Event<WindowEvent>

    /**
     * Window minimized event
     */
    val minimized: Event<WindowEvent>

    /**
     * Window restored (from minimization) event
     */
    val restored: Event<WindowEvent>

    /**
     * Window restored (from minimization) event
     */
    val closed: Event<WindowEvent>

    /**
     * Drop event, triggered when a file is dropped on the window
     */
    val drop: Event<DropEvent>

    /**
     * Window position
     */
    var position: Vector2
}

/**
 * The Program class, this is where most user implementations start.
 */
open class ProgramImplementation(val suspend: Boolean = false) : Program {
    override var width = 0
    override var height = 0

    override val program: Program by lazy { this }
    override var userProperties: MutableMap<String, Any> = mutableMapOf()


    override var name = rootClassName()

    private val animator by lazy { Animatable() }

    override lateinit var drawer: Drawer
    override lateinit var driver: Driver

    override lateinit var application: Application

    /** This is checked at runtime to disallow nesting [extend] blocks. */
    override var isNested: Boolean = false

    /**
     * background color that is used to clear the background every frame
     */
    override var backgroundColor: ColorRGBa? = ColorRGBa.BLACK
    override val dispatcher = Dispatcher()

    /**
     * program ended event
     *
     * The [ended] event is emitted when the program is ended by closing the application window
     */
    override var ended = Event<ProgramEvent>()


    private var firstFrameTime = Double.POSITIVE_INFINITY

    /**
     * clock function. defaults to returning the application time.
     */
    override var clock =
        { if (firstFrameTime == Double.POSITIVE_INFINITY || frameCount <= 0) 0.0 else (application.seconds - firstFrameTime) }

    override var assetProperties = mutableMapOf<String, String>()
    override var assetMetadata = {
        AssetMetadata(this.name, namedTimestamp(), assetProperties)
    }

    final override val requestAssets = Event<RequestAssetsEvent>()
    final override val produceAssets = Event<ProduceAssetsEvent>()

    init {
        requestAssets.listen {
            produceAssets.trigger(
                ProduceAssetsEvent(
                    it.origin, it.program,
                    assetMetadata()
                )
            )
        }
    }


    private var frameSeconds = 0.0
    private var deltaSeconds: Double = 0.0
    private var lastSeconds: Double = -1.0

    override var frameCount = 0

    /**
     * The number of [seconds] since program start, or the time from a custom [clock].
     * value is updated at the beginning of the frame only.
     */
    override val seconds: Double
        get() = frameSeconds

    /**
     * The elapsed time since the last draw loop
     */
    val deltaTime: Double
        get() = deltaSeconds

    inner class ApplicationClipboard : Clipboard {
        override var contents: String?
            get() {
                return application.clipboardContents
            }
            set(value) {
                application.clipboardContents = value
            }
    }

    override val clipboard = ApplicationClipboard()


    override val extensions = mutableListOf<Extension>()
        get() {
            if (field.isEmpty()) isNested = false
            return field
        }

    override fun <T : Extension> extend(extension: T): T {
        extensions.add(extension)
        extension.setup(this)
        return extension
    }

    override fun <T : Extension> extend(extension: T, configure: T.() -> Unit): T {
        extensions.add(extension)
        extension.configure()
        extension.setup(this)
        return extension
    }

    override fun extend(stage: ExtensionStage, userDraw: Program.() -> Unit) {
        if (isNested) error("Cannot nest extend blocks within extend blocks")
        val functionExtension = when (stage) {
            ExtensionStage.SETUP ->
                object : Extension {
                    override var enabled: Boolean = true
                    override fun setup(program: Program) {
                        program.isNested = true
                        program.userDraw()
                    }
                }

            ExtensionStage.BEFORE_DRAW ->
                object : Extension {
                    override var enabled: Boolean = true
                    override fun beforeDraw(drawer: Drawer, program: Program) {
                        program.isNested = true
                        program.userDraw()
                    }
                }

            ExtensionStage.AFTER_DRAW ->
                object : Extension {
                    override var enabled: Boolean = true
                    override fun afterDraw(drawer: Drawer, program: Program) {
                        program.isNested = true
                        program.userDraw()
                    }
                }
        }
        extensions.add(functionExtension)
    }

    /**
     * Simplified window interface
     */
    inner class Window : org.openrndr.Window {
        override var title: String
            get() = application.windowTitle
            set(value) {
                application.windowTitle = value
            }

        override var size
            get() = application.windowSize
            set(value) {
                application.windowSize = value
            }

        override var contentScale
            get() = application.windowContentScale
            set(value) {
                application.windowContentScale = value
            }

        override var presentationMode: PresentationMode
            get() = application.presentationMode
            set(value) {
                application.presentationMode = value
            }

        override var multisample: WindowMultisample
            get() {
                return application.windowMultisample
            }
            set(value) {
                application.windowMultisample = value
            }

        override var resizable: Boolean
            get() {
                return application.windowResizable
            }
            set(value) {
                application.windowResizable = value
            }

        override fun requestFocus() = application.requestFocus()

        override fun requestDraw() = application.requestDraw()

        override val focused = Event<WindowEvent>("window-focused", postpone = true)

        override val unfocused = Event<WindowEvent>("window-unfocused", postpone = true)

        override val moved = Event<WindowEvent>("window-moved", postpone = true)

        override val sized = Event<WindowEvent>("window-sized", postpone = true)

        override val minimized = Event<WindowEvent>("window-minimized", postpone = true)

        override val restored = Event<WindowEvent>("window-restored", postpone = true)

        override val closed = Event<WindowEvent>("window-closed", postpone = true)

        override val drop = Event<DropEvent>("window-drop", postpone = true)

        override var position: Vector2
            get() = application.windowPosition
            set(value) {
                application.windowPosition = value
            }
    }

    override val window = Window()

    override val keyboard by lazy { Keyboard() }
    override val mouse by lazy { ApplicationMouse(application = { application }) }

    override suspend fun setup() {}

    override fun drawImpl() {
        if (frameCount == 0) {
            firstFrameTime = application.seconds
        }
        animator.updateAnimation()
        updateFrameSecondsFromClock()

        if (lastSeconds == -1.0) lastSeconds = seconds

        deltaSeconds = frameSeconds - lastSeconds
        lastSeconds = frameSeconds


        backgroundColor?.let {
            drawer.clear(it)
        }
        extensions.filter { it.enabled }.forEach { it.beforeDraw(drawer, this) }
        draw()
        extensions.reversed().filter { it.enabled }.forEach { it.afterDraw(drawer, this) }
        frameCount++
    }

    fun animate(animationFunction: Animatable.() -> Unit) {
        animator.animationFunction()
    }


    override fun draw() {}

    override fun updateFrameSecondsFromClock() {
        frameSeconds = clock()
    }
}

/**
 * Generates a timestamped name for the program, optionally including a file extension and path.
 *
 * @param extension an optional file extension to be appended to the timestamped name, default is an empty string.
 * @param path an optional file path to be prepended to the timestamped name, default is null.
 * @return a string representing the generated timestamped name, potentially including the specified path and extension.
 */
expect fun Program.namedTimestamp(extension: String = "", path: String? = null): String