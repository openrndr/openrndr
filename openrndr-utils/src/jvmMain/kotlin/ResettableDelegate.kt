package org.openrndr.utils
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KProperty

// via https://stackoverflow.com/a/53921217/130168
actual class ResettableDelegate<T> actual constructor(private val initializer: () -> T) {
    private val lazyRef: AtomicReference<Lazy<T>> = AtomicReference(
            lazy(
                    initializer
            )
    )

    actual operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return lazyRef.get().getValue(thisRef, property)
    }

    actual fun reset() {
        lazyRef.set(lazy(initializer))
    }
}