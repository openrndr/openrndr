package org.openrndr.internal

/**
 * Interface representing a keyboard driver for handling key identification.
 * Provides functionality to retrieve a unique identifier for a specific key.
 */
interface KeyboardDriver {

    /**
     * Retrieves a unique identifier corresponding to the provided key.
     *
     * @param key The string representation of the key for which the identifier is needed.
     * @return An integer representing the unique identifier of the specified key.
     */
    fun getKeyId(key: String): Int

    fun getKeyName(keyId: Int): String

    companion object {
        var driver: KeyboardDriver? = null

        /**
         * The instance singleton
         */
        val instance: KeyboardDriver
            get() {
                return driver ?: error("KeyboardDriver not initialized")
            }
    }
}