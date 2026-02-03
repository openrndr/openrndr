package org.openrndr

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

object AndroidAppRegistry {
    @OptIn(ExperimentalAtomicApi::class)
    private val builderRef = AtomicReference<AndroidApplicationBuilder?>(null)

    @OptIn(ExperimentalAtomicApi::class)
    fun set(builder: AndroidApplicationBuilder) {
        builderRef.exchange(builder)
    }

    @OptIn(ExperimentalAtomicApi::class)
    fun consume(): AndroidApplicationBuilder {
        while (true) {
            val current = builderRef.load()
            if (current != null) {
                // try to swap null in place of current
                if (builderRef.compareAndSet(current, null)) {
                    return current
                }
            } else {
                error("No ApplicationBuilder was provided. Did you call androidApplication { … } before creating the SurfaceView?")
            }
        }
    }
}

actual fun androidApplication(block: AndroidApplicationBuilder.() -> Unit) {
    val builder = AndroidApplicationBuilder().apply(block)
    AndroidAppRegistry.set(builder)
}