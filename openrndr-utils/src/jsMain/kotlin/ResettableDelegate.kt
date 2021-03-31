package org.openrndr.utils

import kotlin.reflect.KProperty
actual class ResettableDelegate<T> actual constructor(private val initializer: () -> T) {
    private var lazyRef = lazy(initializer)

    actual operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return lazyRef.getValue(thisRef, property)
    }

    actual fun reset() {
        lazyRef = lazy(initializer)
    }
}