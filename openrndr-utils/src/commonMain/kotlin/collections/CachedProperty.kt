package org.openrndr.collections

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class CachedProperty<out T>(val loader: () -> T) : ReadOnlyProperty<Any, T> {
    private var value: CachedValue<T> = CachedValue.Invalid

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return when (val result = value) {
            CachedValue.Invalid -> {
                val newValue = loader()
                value = CachedValue.Value(newValue)
                newValue
            }
            is CachedValue.Value<T> -> result.value
        }
    }

    fun invalidate() {
        value = CachedValue.Invalid
    }

    @Suppress("unused")
    private sealed class CachedValue<out T> {
        object Invalid : CachedValue<Nothing>()
        class Value<out T>(val value: T) : CachedValue<T>()
    }
}