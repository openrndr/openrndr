package org.openrndr.internal

import org.openrndr.draw.FontImageMap

abstract class FontMapManager {
    abstract fun fontMapFromUrl(url:String, size:Double, contentScale:Double=1.0): FontImageMap
}