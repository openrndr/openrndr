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


/**
 * The Program class, this is where most user implementations start.
 */
open class Program(val suspend: Boolean = false) {
    var width = 0
    var height = 0

    var name = rootClassName()

    private val animator by lazy { Animatable() }

    lateinit var drawer: Drawer
    lateinit var driver: Driver

    lateinit var application: Application

    /** This is checked at runtime to disallow nesting [extend] blocks. */
    protected var isNested: Boolean = false

    /**
     * background color that is used to clear the background every frame
     */
    var backgroundColor: ColorRGBa? = ColorRGBa.BLACK
    val dispatcher = Dispatcher()

    /**
     * program ended event
     *
     * The [ended] event is emitted when the program is ended by closing the application window
     */
    var ended = Event<ProgramEvent>()

    private var firstFrameTime = Double.POSITIVE_INFINITY

    /**
     * clock function. defaults to returning the application time.
     */
    var clock =
        { if (firstFrameTime == Double.POSITIVE_INFINITY || frameCount <= 0) 0.0 else (application.seconds - firstFrameTime) }

    var assetProperties = mutableMapOf<String, String>()
    var assetMetadata = {
        AssetMetadata(this.name, namedTimestamp(), assetProperties)
    }

    val requestAssets = Event<RequestAssetsEvent>()
    val produceAssets = Event<ProduceAssetsEvent>()

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

    /**
     * list of installed extensions
     */
    val extensions = mutableListOf<Extension>()
        get() {
            if (field.isEmpty()) isNested = false
            return field
        }

    /**
     * install an [Extension]
     * @param extension the [Extension] to install
     */
    fun <T : Extension> extend(extension: T): T {
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
    fun <T : Extension> extend(extension: T, configure: T.() -> Unit): T {
        extensions.add(extension)
        extension.configure()
        extension.setup(this)
        return extension
    }

    /**
     * install an extension function for the given [ExtensionStage]
     */
    fun extend(stage: ExtensionStage = ExtensionStage.BEFORE_DRAW, userDraw: Program.() -> Unit) {
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
    inner class Window {
        var title: String
            get() = application.windowTitle
            set(value) {
                application.windowTitle = value
            }

        var size
            get() = application.windowSize
            set(value) {
                application.windowSize = value
            }

        var contentScale
            get() = application.windowContentScale
            set(value) {
                application.windowContentScale = value
            }

        var presentationMode: PresentationMode
            get() = application.presentationMode
            set(value) {
                application.presentationMode = value
            }

        var multisample: WindowMultisample
            get() {
                return application.windowMultisample
            }
            set(value) {
                application.windowMultisample = value
            }

        var resizable: Boolean
            get() {
                return application.windowResizable
            }
            set(value) {
                application.windowResizable = value
            }

        fun requestFocus() = application.requestFocus()

        fun requestDraw() = application.requestDraw()

        /**
         * Window focused event, triggered when the window receives focus
         */
        val focused = Event<WindowEvent>("window-focused", postpone = true)

        /**
         * Window focused event, triggered when the window loses focus
         */
        val unfocused = Event<WindowEvent>("window-unfocused", postpone = true)

        /**
         * Window moved event
         */
        val moved = Event<WindowEvent>("window-moved", postpone = true)

        /**
         * Window sized event
         */
        val sized = Event<WindowEvent>("window-sized", postpone = true)

        /**
         * Window minimized event
         */
        val minimized = Event<WindowEvent>("window-minimized", postpone = true)

        /**
         * Window restored (from minimization) event
         */
        val restored = Event<WindowEvent>("window-restored", postpone = true)

        /**
         * Window restored (from minimization) event
         */
        val closed = Event<WindowEvent>("window-closed", postpone = true)

        /**
         * Drop event, triggered when a file is dropped on the window
         */
        val drop = Event<DropEvent>("window-drop", postpone = true)

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


    val keyboard by lazy { Keyboard() }
    val mouse by lazy { Mouse({ application }) }
    val pointers by lazy { Pointers({ application }) }

    /**
     * This is ran exactly once before the first call to draw()
     */
    open suspend fun setup() {}

    /**
     * This is the draw call that is called by Application. It takes care of handling extensions.
     */
    fun drawImpl() {
        if (frameCount == 0) {
            firstFrameTime = application.seconds
        }
        animator.updateAnimation()
        frameSeconds = clock()

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
    open fun draw() {}
}

expect fun Program.namedTimestamp(extension: String = "", path: String? = null): String