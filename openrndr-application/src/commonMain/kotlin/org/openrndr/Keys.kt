package org.openrndr

import org.openrndr.events.Event

/**
 * Key modifier enumeration
 */
enum class KeyModifier(val mask: Int) {
    SHIFT(1),
    CTRL(2),
    ALT(4),
    SUPER(8)
}

/**
 * Mouse button enumeration
 */
enum class MouseButton {
    LEFT,
    RIGHT,
    CENTER,
    NONE
}

/**
 * Mouse event type enumeration
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
 * Key event type enumeration
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
class KeyEvent(
        val type: KeyEventType,
        val key: Int,
        val name: String,
        val modifiers: Set<KeyModifier>,
        var propagationCancelled: Boolean = false) {
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

interface KeyEvents {
    val keyDown: Event<KeyEvent>
    val keyUp: Event<KeyEvent>
    val keyRepeat: Event<KeyEvent>
    val character: Event<CharacterEvent>
}

/**
 * Keyboard events in a single class
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

class KeyTracker(keyEvents: KeyEvents) {
    private val mutablePressedKeys = mutableSetOf<String>()

    /**
     * set containing the names of the currently pressed keys
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
