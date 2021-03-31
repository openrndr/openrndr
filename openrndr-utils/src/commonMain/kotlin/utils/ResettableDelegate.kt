package org.openrndr.utils

import kotlin.reflect.KProperty

fun <T> resettableLazy(initializer: () -> T) = ResettableDelegate(initializer)

expect class ResettableDelegate<T>(initializer: () -> T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T
    fun reset()
}