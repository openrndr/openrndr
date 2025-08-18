@file:JvmName("ImageFileDetailsJVM")
package org.openrndr.draw

import org.openrndr.internal.ImageDriver
import kotlin.jvm.JvmName
import kotlin.jvm.JvmRecord

/**
 * Details for an image
 * @param width the width of the image in pixels
 * @param height the height of the image in pixels
 * @param channels the number of channels in the image
 * @since 0.4.3
 */
@JvmRecord
@Suppress("unused")
data class ImageFileDetails(val width: Int, val height: Int, val channels: Int)

/**
 * Probes an image file or URL to retrieve its details such as dimensions and channels.
 *
 * @param fileOrUrl a string representing the file path or a URL to the image resource.
 * @return an instance of [ImageFileDetails] containing width, height, and channels of the image,
 *         or `null` if the image could not be probed or does not exist.
 * @since 0.4.3
 */
fun probeImage(fileOrUrl: String): ImageFileDetails? {
    return ImageDriver.instance.probeImage(fileOrUrl)
}