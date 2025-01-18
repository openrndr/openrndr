package org.openrndr.draw

/**
 * A class representing a resizable color buffer, which can dynamically resize itself while maintaining color properties.
 *
 * This wrapper manages a `ColorBuffer` instance and provides methods to easily resize it when needed.
 *
 * @constructor
 * @param width Initial width of the color buffer.
 * @param height Initial height of the color buffer.
 * @param contentScale Scale factor of the buffer's content (default is 1.0).
 * @param format The color format of the buffer (default is `ColorFormat.RGBa`).
 * @param type The color type specifying the data type of the color components (default is determined using [defaultColorType]).
 * @param multisample Multisample configuration for the buffer (default is `BufferMultisample.Disabled`).
 * @param levels Number of mipmap levels (default is 1).
 * @param session Graphics session to which the buffer belongs (default is the active session).
 */
class ResizableColorBuffer(
    width: Int,
    height: Int,
    contentScale: Double = 1.0,
    format: ColorFormat = ColorFormat.RGBa,
    type: ColorType = defaultColorType(format),
    multisample: BufferMultisample = BufferMultisample.Disabled,
    levels: Int = 1,
    session: Session? = Session.active
) : AutoCloseable {
    var colorBuffer = colorBuffer(width, height, contentScale, format, type, multisample, levels, session)
    fun resize(
        newWidth: Int = colorBuffer.width,
        newHeight: Int = colorBuffer.height,
        newContentScale: Double = colorBuffer.contentScale
    ) {
        if (newWidth != colorBuffer.width || newHeight != colorBuffer.height || newContentScale != colorBuffer.contentScale) {
            colorBuffer.destroy()
            colorBuffer = colorBuffer(
                newWidth, newHeight, newContentScale,
                colorBuffer.format, colorBuffer.type, colorBuffer.multisample, colorBuffer.levels, colorBuffer.session
            )
        }
    }

    fun resize(renderTarget: RenderTarget) {
        resize(renderTarget.width, renderTarget.height, renderTarget.contentScale)
    }

    override fun close() {
        colorBuffer.close()
    }
}

/**
 * Creates a resizable color buffer with the specified dimensions and properties.
 *
 * @param width The initial width of the color buffer in pixels.
 * @param height The initial height of the color buffer in pixels.
 * @param contentScale The content scale factor for the color buffer, default is 1.0.
 * @param format The color format of the buffer (e.g., RGBa), default is [ColorFormat.RGBa].
 * @param type The type of color data in the buffer (e.g., UINT8, FLOAT16). Defaults to the type as determined
 *             by the given [format] using [defaultColorType].
 * @param multisample Specifies whether multisampling is enabled and the sample count, default is [BufferMultisample.Disabled].
 * @param levels The number of mipmap levels, default is 1.
 * @param session The session to which this buffer is associated, default is the currently active session ([Session.active]).
 */
fun resizableColorBuffer(
    width: Int,
    height: Int,
    contentScale: Double = 1.0,
    format: ColorFormat = ColorFormat.RGBa,
    type: ColorType = defaultColorType(format),
    multisample: BufferMultisample = BufferMultisample.Disabled,
    levels: Int = 1,
    session: Session? = Session.active
) = ResizableColorBuffer(width, height, contentScale, format, type, multisample, levels, session)