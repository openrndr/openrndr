package org.openrndr

/**
 * Represents an event triggered by a character input, typically used in text input scenarios.
 *
 * @property character the character associated with the input event.
 * @property modifiers the set of key modifiers (e.g., SHIFT, CTRL) active during the event.
 */
data class CharacterEvent(
    val character: Char,
    val modifiers: Set<KeyModifier>,
) {
    var propagationCancelled: Boolean = false

    fun cancelPropagation() {
        propagationCancelled = true
    }
}