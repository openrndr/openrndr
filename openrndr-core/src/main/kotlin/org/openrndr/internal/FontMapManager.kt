package org.openrndr.internal

import org.openrndr.draw.FontImageMap

abstract class FontMapManager {
    abstract fun fontMapFromUrl(url:String, size:Double, characterSet: Set<Char>, contentScale:Double=1.0): FontImageMap
}