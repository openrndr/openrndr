package org.openrndr

import org.openrndr.draw.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Create an image [ColorBuffer] by drawing it
 * @since 0.4.3
 * @param width the width of the created image
 * @param height the height of the created image
 * @param format the color format of the created image
 * @param type the color type of the created image
 * @param contentScale the content scale of the created image. When set to null the content scale setting is taken from [RenderTarget.active].
 * @param multisample the multisampling used for drawing the image, the resulting image has multisampling disabled. When set to null the multisampling setting is taken from [RenderTarget.active].
 * @param drawFunction the user supplied function that is used to draw on the created image
 */
@OptIn(ExperimentalContracts::class)
fun Program.drawImage(
    width: Int,
    height: Int,
    contentScale: Double? = null,
    format: ColorFormat = ColorFormat.RGBa,
    type: ColorType = ColorType.UINT8,
    multisample: BufferMultisample? = null,
    drawFunction: Drawer.() -> Unit
): ColorBuffer {
    contract {
        callsInPlace(drawFunction, InvocationKind.EXACTLY_ONCE)
    }

    val resolvedContentScale = contentScale ?: RenderTarget.active.contentScale
    val resolvedMultisample = multisample ?: RenderTarget.active.multisample
    val result = colorBuffer(width, height, contentScale = resolvedContentScale, format = format, type = type)
    val tempTarget = renderTarget(width, height, contentScale = resolvedContentScale,
        multisample = resolvedMultisample) {

        if (resolvedMultisample == BufferMultisample.Disabled) {
            colorBuffer(result)
        } else {
            colorBuffer()
        }
        depthBuffer()
    }

    drawer.isolatedWithTarget(tempTarget) {
        drawer.defaults()
        drawer.ortho(tempTarget)
        drawer.drawFunction()
    }

    if (resolvedMultisample != BufferMultisample.Disabled) {
        tempTarget.colorBuffer(0).copyTo(result)
        tempTarget.colorBuffer(0).destroy()
    }

    tempTarget.detachColorAttachments()
    tempTarget.depthBuffer?.destroy()
    tempTarget.detachDepthBuffer()
    tempTarget.destroy()

    result.generateMipmaps()
    return result
}