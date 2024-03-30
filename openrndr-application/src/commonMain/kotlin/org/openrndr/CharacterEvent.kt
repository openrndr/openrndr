package org.openrndr

data class CharacterEvent(
    val character: Char,
    val modifiers: Set<KeyModifier>,
) {
    var propagationCancelled: Boolean = false

    fun cancelPropagation() {
        propagationCancelled = true
    }
}