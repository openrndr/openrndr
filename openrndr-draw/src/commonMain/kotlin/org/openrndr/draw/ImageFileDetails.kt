@file:JvmName("ImageFileDetailsJVM")
package org.openrndr.draw

import org.openrndr.internal.ImageDriver
import kotlin.jvm.JvmName

/**
 * Details for an image
 * @param width the width of the image in pixels
 * @param height the height of the image in pixels
 * @param channels the number of channels in the image
 * @since 0.4.3
 */
data class ImageFileDetails(val width: Int, val height: Int, val channels: Int)

/**
 * Probe an image located at [fileOrUrl]
 * @param fileOrUrl a string encoded file location or url
 * @since 0.4.3
 */
fun probeImage(fileOrUrl: String): ImageFileDetails? {
    return ImageDriver.instance.probeImage(fileOrUrl)
}