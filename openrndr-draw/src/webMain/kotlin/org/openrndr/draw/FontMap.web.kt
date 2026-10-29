package org.openrndr.draw

/**
 * The url of the font of [defaultFontMap], set by the application, null for no default font
 */
var defaultFontUrl: String? = null

actual val defaultFontMap: FontImageMap? by lazy {
    defaultFontUrl?.let { FontImageMap.fromUrl(it, 16.0, contentScale = RenderTarget.active.contentScale) }
}
