@file:OptIn(ExperimentalAtomicApi::class)

package org.openrndr.utils

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.reflect.KProperty

// via https://stackoverflow.com/a/53921217/130168
actual class ResettableDelegate<T> actual constructor(private val initializer: () -> T) {
    @OptIn(ExperimentalAtomicApi::class)
    private val lazyRef: AtomicReference<Lazy<T>> = AtomicReference(
            lazy(
                    initializer
            )
    )

    actual operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return lazyRef.load().getValue(thisRef, property)
    }

    actual fun reset() {
        lazyRef.store(lazy(initializer))
    }
}