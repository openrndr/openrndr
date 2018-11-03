package org.openrndr.draw

import kotlinx.coroutines.CoroutineDispatcher

interface DrawThread {
    val drawer: Drawer
    val dispatcher: CoroutineDispatcher
}