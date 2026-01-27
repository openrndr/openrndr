package org.openrndr

import org.openrndr.events.Event
import org.openrndr.internal.KeyboardDriver

/**
 * Represents key modifiers that can be used in combination with keyboard or mouse input.
 *
 * Each key modifier is associated with a unique bitmask value, allowing combinations of modifiers
 * to be represented as a single integer.
 *
 * @property mask The bitmask value for this key modifier.
 */
enum class KeyModifier(val mask: Int) {
    SHIFT(1),
    CTRL(2),
    ALT(4),
    SUPER(8)
}

/**
 * Represents mouse buttons commonly used in interaction with graphical user interfaces.
 *
 * The enum provides the following values:
 * - `LEFT`: Represents the primary mouse button, typically the left button.
 * - `RIGHT`: Represents the secondary mouse button, typically the right button.
 * - `CENTER`: Represents the middle mouse button, typically the wheel button.
 * - `NONE`: Represents an absence of any mouse button interaction.
 */
enum class MouseButton {
    LEFT,
    RIGHT,
    CENTER,
    NONE
}

/**
 * Represents the types of mouse events that can occur in the system.
 *
 * This enum class categorizes the different interactions that the mouse can have
 * within the application, such as movement, button clicks, or scrolling actions.
 */
enum class MouseEventType {
    MOVED,
    DRAGGED,
    CLICKED,
    BUTTON_UP,
    BUTTON_DOWN,
    SCROLLED,
    ENTERED,
    EXITED
}

/**
 * Represents the type of a key event in an application.
 *
 * Key events are typically emitted when a key on a keyboard is pressed, released,
 * or held down. This enum class categorizes such events into the following types:
 *
 * - `KEY_DOWN`: Indicates that a key has been pressed.
 * - `KEY_UP`: Indicates that a key has been released.
 * - `KEY_REPEAT`: Indicates that a key is being continuously held down, causing repeated events.
 */
enum class KeyEventType {
    KEY_DOWN,
    KEY_UP,
    KEY_REPEAT,
}

/**
 * Key event describes key events.
 * @property type the type of event
 * @property key physical key identifier, don't use this for layout-sensitive queries
 * @property name the layout-sensitive name of the key
 * @property modifiers a set of key modifiers that are active/pressed
 * @property propagationCancelled a flag that can be set to indicate that this event is handled and should not
 * be processed further
 */
data class KeyEvent(
        val type: KeyEventType,
        val key: Int,
        val name: String,
        val modifiers: Set<KeyModifier>,
) {
    var propagationCancelled: Boolean = false
    /**
     * Marks the event's propagation as cancelled.
     *
     * Calling this method sets the `propagationCancelled` property to `true`.
     * This serves as an indication that the event has been handled and should
     * not be processed further by other event listeners.
     *
     * It is important to note that cancelling propagation is only a suggestion,
     * and all event listeners should check the status of `propagationCancelled`
     * to determine if the event has already been consumed.
     *
     * @see propagationCancelled
     */
    fun cancelPropagation() {
        propagationCancelled = true
    }
}

const val KEYNAME_SPACEBAR = "spacebar"
const val KEYNAME_ESCAPE = "escape"
const val KEYNAME_ENTER = "enter"
const val KEYNAME_TAB = "tab"
const val KEYNAME_BACKSPACE = "backspace"
const val KEYNAME_INSERT = "insert"
const val KEYNAME_DELETE = "delete"
const val KEYNAME_ARROW_RIGHT = "arrow-right"
const val KEYNAME_ARROW_LEFT = "arrow-left"
const val KEYNAME_ARROW_DOWN = "arrow-down"
const val KEYNAME_ARROW_UP = "arrow-up"
const val KEYNAME_PAGE_UP = "page-up"
const val KEYNAME_PAGE_DOWN = "page-down"
const val KEYNAME_HOME = "home"
const val KEYNAME_END = "end"
const val KEYNAME_LEFT_SHIFT = "left-shift"
const val KEYNAME_RIGHT_SHIFT = "right-shift"
const val KEYNAME_LEFT_ALT = "left-alt"
const val KEYNAME_RIGHT_ALT = "right-alt"
const val KEYNAME_LEFT_CONTROL = "left-control"
const val KEYNAME_RIGHT_CONTROL = "right-control"
const val KEYNAME_LEFT_SUPER = "left-super"
const val KEYNAME_RIGHT_SUPER = "right-super"
const val KEYNAME_CAPSLOCK = "caps-lock"
const val KEYNAME_PRINT_SCREEN = "print-screen"
const val KEYNAME_F1 = "f1"
const val KEYNAME_F2 = "f2"
const val KEYNAME_F3 = "f3"
const val KEYNAME_F4 = "f4"
const val KEYNAME_F5 = "f5"
const val KEYNAME_F6 = "f6"
const val KEYNAME_F7 = "f7"
const val KEYNAME_F8 = "f8"
const val KEYNAME_F9 = "f9"
const val KEYNAME_F10 = "f10"
const val KEYNAME_F11 = "f11"
const val KEYNAME_F12 = "f12"


val KEY_SPACEBAR by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_SPACEBAR) }
val KEY_ESCAPE by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_ESCAPE) }
val KEY_ENTER by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_ENTER) }
val KEY_TAB by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_TAB) }
val KEY_BACKSPACE by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_BACKSPACE)}
val KEY_INSERT by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_INSERT)}
val KEY_DELETE by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_DELETE)}
val KEY_ARROW_RIGHT by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_ARROW_RIGHT) }
val KEY_ARROW_LEFT by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_ARROW_LEFT) }
val KEY_ARROW_DOWN by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_ARROW_DOWN) }
val KEY_ARROW_UP by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_ARROW_UP) }
val KEY_PAGE_UP by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_PAGE_UP) }
val KEY_PAGE_DOWN by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_PAGE_DOWN) }
val KEY_HOME by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_HOME) }
val KEY_END by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_END) }

val KEY_CAPSLOCK by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_CAPSLOCK) }
val KEY_PRINT_SCREEN by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_PRINT_SCREEN) }

val KEY_F1 by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_F1) }
val KEY_F2 by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_F2) }
val KEY_F3 by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_F3) }
val KEY_F4 by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_F4) }
val KEY_F5 by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_F5) }
val KEY_F6 by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_F6) }
val KEY_F7 by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_F7) }
val KEY_F8 by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_F8) }
val KEY_F9 by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_F9) }
val KEY_F10 by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_F10) }
val KEY_F11 by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_F11) }
val KEY_F12 by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_F12) }

val KEY_LEFT_SHIFT by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_LEFT_SHIFT) }
val KEY_RIGHT_SHIFT by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_RIGHT_SHIFT) }
val KEY_LEFT_CONTROL by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_LEFT_CONTROL) }
val KEY_RIGHT_CONTROL by lazy { KeyboardDriver.instance.getKeyId(KEYNAME_RIGHT_CONTROL) }

/**
 * Interface for handling keyboard events. This interface provides access
 * to events related to keyboard interactions such as key presses, releases,
 * repetitions, and character inputs.
 */
interface KeyEvents {
    val keyDown: Event<KeyEvent>
    val keyUp: Event<KeyEvent>
    val keyRepeat: Event<KeyEvent>
    val character: Event<CharacterEvent>
}

/**
 * The `Keyboard` class provides event-based interaction for keyboard inputs. It implements
 * the `KeyEvents` interface to handle various types of keyboard-related events such as
 * key presses, releases, repetitions, and character inputs. These events are typically
 * triggered by the `Application` interacting with the underlying system.
 */
class Keyboard: KeyEvents {
    /**
     * key down event
     *
     * This event is triggered from [Application] whenever a key is pressed.
     */
    override val keyDown = Event<KeyEvent>("keyboard-key-down", postpone = true)
    /**
     * key up event
     *
     * This event is triggered from [Application] whenever a key is released.
     */
    override val keyUp = Event<KeyEvent>("keyboard-key-up", postpone = true)
    /**
     * key repeat event
     *
     * This event is triggered from [Application] whenever a key is held down for a while.
     */
    override val keyRepeat = Event<KeyEvent>("keyboard-key-repeat", postpone = true)
    /**
     * character event
     *
     * This event is triggered from [Application] whenever an input character is generated.
     */
    override val character = Event<CharacterEvent>("keyboard-character", postpone = true)

}

/**
 * Tracks the keys currently pressed on the keyboard.
 *
 * This class listens to key down and key up events from a provided [KeyEvents] instance
 * and maintains a set of the currently pressed keys. Keys are identified by their names.
 *
 * @constructor Creates a new KeyTracker instance by subscribing to the provided [KeyEvents] object.
 * @param keyEvents The source of the key events to listen to.
 */
class KeyTracker(keyEvents: KeyEvents) {
    private val mutablePressedKeys = mutableSetOf<String>()

    /**
     * A read-only set containing the names of the keys currently pressed on the keyboard.
     *
     * This property is dynamically updated based on key press (key down) and release (key up) events,
     * providing an up-to-date view of active keyboard input.
     */
    val pressedKeys: Set<String> = mutablePressedKeys

    init {
        keyEvents.keyDown.listen {
            mutablePressedKeys.add(it.name)
        }

        keyEvents.keyUp.listen {
            mutablePressedKeys.remove(it.name)
        }
    }
}
