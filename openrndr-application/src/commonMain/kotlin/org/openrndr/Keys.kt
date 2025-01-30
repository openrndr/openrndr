package org.openrndr

import org.openrndr.events.Event

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

const val KEY_SPACEBAR = 32

const val KEY_ESCAPE = 256
const val KEY_ENTER = 257

const val KEY_TAB = 258
const val KEY_BACKSPACE = 259
const val KEY_INSERT = 260
const val KEY_DELETE = 261
const val KEY_ARROW_RIGHT = 262
const val KEY_ARROW_LEFT = 263
const val KEY_ARROW_DOWN = 264
const val KEY_ARROW_UP = 265
const val KEY_PAGE_UP = 266
const val KEY_PAGE_DOWN = 267
const val KEY_HOME = 268
const val KEY_END = 269

const val KEY_CAPSLOCK = 280
const val KEY_PRINT_SCREEN = 283


const val KEY_F1 = 290
const val KEY_F2 = 291
const val KEY_F3 = 292
const val KEY_F4 = 293
const val KEY_F5 = 294
const val KEY_F6 = 295
const val KEY_F7 = 296
const val KEY_F8 = 297
const val KEY_F9 = 298
const val KEY_F10 = 299
const val KEY_F11 = 300
const val KEY_F12 = 301

const val KEY_LEFT_SHIFT = 340
const val KEY_RIGHT_SHIFT = 344

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
