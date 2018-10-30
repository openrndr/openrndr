@file:Suppress("unused")

package org.openrndr

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.internal.Driver
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2

import java.io.File


enum class WindowEventType {
    MOVED,
    RESIZED,
    FOCUSED,
    UNFOCUSED,
}

class WindowEvent(val type: WindowEventType, val position: Vector2, val size: Vector2, val focused: Boolean)

class DropEvent(val position: Vector2, val files: List<File>)


/**
The Program class, this is where most user implementations start
 **/
open class Program {

    var width = 0
    var height = 0

    lateinit var drawer: Drawer
    lateinit var driver: Driver

    lateinit var application: Application

    var backgroundColor: ColorRGBa? = ColorRGBa.BLACK


    val dispatcher = PumpDispatcher()

    /**
     * The number of [seconds] since program start
     */
    val seconds: Double
        get() = application.seconds

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
    private val extensions = mutableListOf<Extension>()


    /**
     * Install an [extension].
     */
    fun extend(extension: Extension): Extension {
        extensions.add(extension)
        extension.setup(this)
        return extension
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

    class Keyboard {
        val keyDown = Event<KeyEvent>().postpone(true)
        val keyUp = Event<KeyEvent>().postpone(true)
        val keyRepeat = Event<KeyEvent>().postpone(true)
        val character = Event<CharacterEvent>().postpone(true)
    }

    val keyboard = Keyboard()

    class Mouse {
        class MouseEvent(val position: Vector2, val rotation: Vector2, val dragDisplacement: Vector2, val type: MouseEventType, val button: MouseButton, val modifiers: Set<KeyboardModifier>, var propagationCancelled: Boolean = false) {
            fun cancelPropagation() {
                propagationCancelled = true
            }
        }

        /**
         * The current mouse position
         */
        var position = Vector2(0.0, 0.0)

        val buttonDown = Event<MouseEvent>().postpone(true)
        val buttonUp = Event<MouseEvent>().postpone(true)
        val dragged = Event<MouseEvent>().postpone(true)
        val moved = Event<MouseEvent>().postpone(true)
        val scrolled = Event<MouseEvent>().postpone(true)
        val clicked = Event<MouseEvent>().postpone(true)
    }

    val mouse = Mouse()

    /**
     * This is ran exactly once before the first call to draw()
     */
    open fun setup() {}

    /**
     * This is the draw call that is called by Application. It takes care of handling extensions.
     */
    fun drawImpl() {
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