package org.openrndr.draw

import org.openrndr.internal.Driver
import org.openrndr.internal.shareGroupID

/**
 * The url of the font of [defaultFontMap], set by the application, null for no default font
 */
var defaultFontUrl: String? = null

/**
 * The default font maps per [share group][Driver.shareGroupID], every canvas has its own
 */
private val defaultFontMaps = mutableMapOf<Long, FontImageMap>()

actual val defaultFontMap: FontImageMap?
    get() = defaultFontUrl?.let { url ->
        defaultFontMaps.getOrPut(Driver.instance.shareGroupID) {
            FontImageMap.fromUrl(url, 16.0, contentScale = RenderTarget.active.contentScale)
        }
    }
