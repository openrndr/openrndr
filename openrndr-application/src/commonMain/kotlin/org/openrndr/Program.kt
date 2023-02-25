@file:Suppress("unused")

package org.openrndr

import org.openrndr.animatable.Animatable
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2

expect fun rootClassName(): String

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
data class WindowEvent(val type: WindowEventType, val position: Vector2, val size: Vector2, val focused: Boolean)

/**
 * window drop item event message
 */
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
 * program event message
 */
data class ProgramEvent(val type: ProgramEventType)

data class RequestAssetsEvent(val origin: Any, val program: Program)
data class ProduceAssetsEvent(val origin: Any, val program: Program, val assetMetadata: AssetMetadata)

data class AssetMetadata(
    val programName: String,
    val assetBaseName: String,
    val assetProperties: Map<String, String>
)

interface InputEvents {
    val mouse: MouseEvents
    val keyboard: KeyEvents
    val pointers: Pointers
}

interface Clipboard {
    var contents: String?
}

interface Program: InputEvents, ExtensionHost {

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
    fun draw()

    val produceAssets: Event<ProduceAssetsEvent>
    val requestAssets: Event<RequestAssetsEvent>
    var assetMetadata: () -> AssetMetadata
    var assetProperties: MutableMap<String, String>
    var clock: () -> Double
    var ended: Event<ProgramEvent>
    var backgroundColor: ColorRGBa?
    val seconds: Double
    val frameCount: Int
    val clipboard: ProgramImplementation.ApplicationClipboard

    fun updateFrameSecondsFromClock()
}

interface Window {
    var title: String
    var size: Vector2
    var contentScale: Double
    var presentationMode: PresentationMode
    var multisample: WindowMultisample
    var resizable: Boolean

    fun requestFocus()

    fun requestDraw()

    /**
     * Window focused event, triggered when the window receives focus
     */
    val focused : Event<WindowEvent>

    /**
     * Window focused event, triggered when the window loses focus
     */
    val unfocused : Event<WindowEvent>

    /**
     * Window moved event
     */
    val moved : Event<WindowEvent>

    /**
     * Window sized event
     */
    val sized : Event<WindowEvent>

    /**
     * Window minimized event
     */
    val minimized : Event<WindowEvent>

    /**
     * Window restored (from minimization) event
     */
    val restored : Event<WindowEvent>

    /**
     * Window restored (from minimization) event
     */
    val closed : Event<WindowEvent>

    /**
     * Drop event, triggered when a file is dropped on the window
     */
    val drop : Event<DropEvent>

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

    override val requestAssets = Event<RequestAssetsEvent>()
    override val produceAssets = Event<ProduceAssetsEvent>()

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

    inner class ApplicationClipboard  : Clipboard{
        override var contents: String?
            get() {
                return application.clipboardContents
            }
            set(value) {
                application.clipboardContents = value
            }
    }

    override val clipboard = ApplicationClipboard()

    /**
     * list of installed extensions
     */
    override val extensions = mutableListOf<Extension>()
        get() {
            if (field.isEmpty()) isNested = false
            return field
        }

    /**
     * install an [Extension]
     * @param extension the [Extension] to install
     */
    override fun <T : Extension> extend(extension: T): T {
        extensions.add(extension)
        extension.setup(this)
        return extension
    }

    /**
     * install an [Extension] and configure it
     * @param extension the [Extension] to install
     * @param configure a configuration function to called with [extension] as its receiver
     * @return the installed [Extension]
     */
    override fun <T : Extension> extend(extension: T, configure: T.() -> Unit): T {
        extensions.add(extension)
        extension.configure()
        extension.setup(this)
        return extension
    }

    /**
     * install an extension function for the given [ExtensionStage]
     */
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
    inner class Window:org.openrndr.Window {
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

        /**
         * Window focused event, triggered when the window receives focus
         */
        override val focused = Event<WindowEvent>("window-focused", postpone = true)

        /**
         * Window focused event, triggered when the window loses focus
         */
        override val unfocused = Event<WindowEvent>("window-unfocused", postpone = true)

        /**
         * Window moved event
         */
        override val moved = Event<WindowEvent>("window-moved", postpone = true)

        /**
         * Window sized event
         */
        override val sized = Event<WindowEvent>("window-sized", postpone = true)

        /**
         * Window minimized event
         */
        override val minimized = Event<WindowEvent>("window-minimized", postpone = true)

        /**
         * Window restored (from minimization) event
         */
        override val restored = Event<WindowEvent>("window-restored", postpone = true)

        /**
         * Window restored (from minimization) event
         */
        override val closed = Event<WindowEvent>("window-closed", postpone = true)

        /**
         * Drop event, triggered when a file is dropped on the window
         */
        override val drop = Event<DropEvent>("window-drop", postpone = true)

        /**
         * Window position
         */
        override var position: Vector2
            get() = application.windowPosition
            set(value) {
                application.windowPosition = value
            }
    }

    override val window = Window()


    override val keyboard by lazy { Keyboard() }
    override val mouse by lazy { ApplicationMouse({ application }) }
    override val pointers by lazy { Pointers({ application }) }

    /**
     * This runs exactly once before the first call to draw()
     */
    override suspend fun setup() {}

    /**
     * This is the draw call that is called by Application. It takes care of handling extensions.
     */
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

    /**
     * This is the user facing draw call. It should be overridden by the user.
     */
    override fun draw() {}

    override fun updateFrameSecondsFromClock() {
        frameSeconds = clock()
    }
}

expect fun Program.namedTimestamp(extension: String = "", path: String? = null): String