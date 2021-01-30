package org.openrndr.utils

import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KProperty

// via https://stackoverflow.com/a/53921217/130168

fun <T> resettableLazy(initializer: () -> T) = ResettableDelegate(initializer)

class ResettableDelegate<T>(private val initializer: () -> T) {
    private val lazyRef: AtomicReference<Lazy<T>> = AtomicReference(
            lazy(
                    initializer
            )
    )

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return lazyRef.get().getValue(thisRef, property)
    }

    fun reset() {
        lazyRef.set(lazy(initializer))
    }
}