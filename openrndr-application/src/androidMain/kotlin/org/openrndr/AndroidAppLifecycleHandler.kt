package org.openrndr

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

interface AndroidAppLifecycleListener {
    fun onPause()
    fun onResume()
}

class AndroidAppLifecycleHandler(lifecycle: Lifecycle, listener: AndroidAppLifecycleListener) {
    init {
        val observer = object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                listener.onPause()
            }

            override fun onResume(owner: LifecycleOwner) {
                listener.onResume()
            }
        }
        lifecycle.addObserver(observer)
    }
}