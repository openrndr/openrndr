package org.openrndr.awt

import org.openrndr.*
import org.openrndr.internal.KeyboardDriver
import java.awt.event.InputEvent
import java.awt.event.KeyEvent as AwtKeyEvent

/**
 * Maps key names to AWT virtual key codes, key events of the AWT back-end carry AWT virtual key codes
 */
class KeyboardDriverAWT : KeyboardDriver {
    private val keys = mapOf(
        KEYNAME_SPACEBAR to AwtKeyEvent.VK_SPACE,
        KEYNAME_ESCAPE to AwtKeyEvent.VK_ESCAPE,
        KEYNAME_ENTER to AwtKeyEvent.VK_ENTER,
        KEYNAME_TAB to AwtKeyEvent.VK_TAB,
        KEYNAME_BACKSPACE to AwtKeyEvent.VK_BACK_SPACE,
        KEYNAME_INSERT to AwtKeyEvent.VK_INSERT,
        KEYNAME_DELETE to AwtKeyEvent.VK_DELETE,
        KEYNAME_ARROW_RIGHT to AwtKeyEvent.VK_RIGHT,
        KEYNAME_ARROW_LEFT to AwtKeyEvent.VK_LEFT,
        KEYNAME_ARROW_DOWN to AwtKeyEvent.VK_DOWN,
        KEYNAME_ARROW_UP to AwtKeyEvent.VK_UP,
        KEYNAME_PAGE_UP to AwtKeyEvent.VK_PAGE_UP,
        KEYNAME_PAGE_DOWN to AwtKeyEvent.VK_PAGE_DOWN,
        KEYNAME_HOME to AwtKeyEvent.VK_HOME,
        KEYNAME_END to AwtKeyEvent.VK_END,
        KEYNAME_CAPSLOCK to AwtKeyEvent.VK_CAPS_LOCK,
        KEYNAME_PRINT_SCREEN to AwtKeyEvent.VK_PRINTSCREEN,
        KEYNAME_F1 to AwtKeyEvent.VK_F1,
        KEYNAME_F2 to AwtKeyEvent.VK_F2,
        KEYNAME_F3 to AwtKeyEvent.VK_F3,
        KEYNAME_F4 to AwtKeyEvent.VK_F4,
        KEYNAME_F5 to AwtKeyEvent.VK_F5,
        KEYNAME_F6 to AwtKeyEvent.VK_F6,
        KEYNAME_F7 to AwtKeyEvent.VK_F7,
        KEYNAME_F8 to AwtKeyEvent.VK_F8,
        KEYNAME_F9 to AwtKeyEvent.VK_F9,
        KEYNAME_F10 to AwtKeyEvent.VK_F10,
        KEYNAME_F11 to AwtKeyEvent.VK_F11,
        KEYNAME_F12 to AwtKeyEvent.VK_F12,
    )

    /** AWT has one virtual key code for the left and right modifier keys, they are told apart by their location */
    private val sidedKeys = mapOf(
        KEYNAME_LEFT_SHIFT to AwtKeyEvent.VK_SHIFT,
        KEYNAME_RIGHT_SHIFT to AwtKeyEvent.VK_SHIFT,
        KEYNAME_LEFT_ALT to AwtKeyEvent.VK_ALT,
        KEYNAME_RIGHT_ALT to AwtKeyEvent.VK_ALT,
        KEYNAME_LEFT_CONTROL to AwtKeyEvent.VK_CONTROL,
        KEYNAME_RIGHT_CONTROL to AwtKeyEvent.VK_CONTROL,
        KEYNAME_LEFT_SUPER to AwtKeyEvent.VK_META,
        KEYNAME_RIGHT_SUPER to AwtKeyEvent.VK_META,
    )

    private val names = keys.entries.associate { (name, code) -> code to name }

    override fun getKeyId(key: String): Int = keys[key] ?: sidedKeys[key] ?: error("unknown key: $key")

    override fun getKeyName(keyId: Int): String = names[keyId] ?: when (keyId) {
        AwtKeyEvent.VK_SHIFT -> KEYNAME_LEFT_SHIFT
        AwtKeyEvent.VK_ALT -> KEYNAME_LEFT_ALT
        AwtKeyEvent.VK_CONTROL -> KEYNAME_LEFT_CONTROL
        AwtKeyEvent.VK_META -> KEYNAME_LEFT_SUPER
        else -> AwtKeyEvent.getKeyText(keyId).lowercase()
    }

    /** the name of the key of [event], modifier keys on the right side get their right-side names */
    internal fun keyName(event: AwtKeyEvent): String {
        val right = event.keyLocation == AwtKeyEvent.KEY_LOCATION_RIGHT
        return when (event.keyCode) {
            AwtKeyEvent.VK_SHIFT -> if (right) KEYNAME_RIGHT_SHIFT else KEYNAME_LEFT_SHIFT
            AwtKeyEvent.VK_ALT -> if (right) KEYNAME_RIGHT_ALT else KEYNAME_LEFT_ALT
            AwtKeyEvent.VK_CONTROL -> if (right) KEYNAME_RIGHT_CONTROL else KEYNAME_LEFT_CONTROL
            AwtKeyEvent.VK_META -> if (right) KEYNAME_RIGHT_SUPER else KEYNAME_LEFT_SUPER
            else -> getKeyName(event.keyCode)
        }
    }
}

/** the key modifiers of an AWT input event */
internal fun modifiersOf(event: InputEvent): Set<KeyModifier> {
    val modifiers = mutableSetOf<KeyModifier>()
    if (event.isShiftDown) modifiers.add(KeyModifier.SHIFT)
    if (event.isControlDown) modifiers.add(KeyModifier.CTRL)
    if (event.isAltDown) modifiers.add(KeyModifier.ALT)
    if (event.isMetaDown) modifiers.add(KeyModifier.SUPER)
    return modifiers
}
