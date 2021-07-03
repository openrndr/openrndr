package org.openrndr.draw

class ResizableColorBuffer(
    width: Int,
    height: Int,
    contentScale: Double = 1.0,
    format: ColorFormat = ColorFormat.RGBa,
    type: ColorType = ColorType.UINT8,
    multisample: BufferMultisample = BufferMultisample.Disabled,
    levels: Int = 1,
    session: Session? = Session.active
) {
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
}

fun resizableColorBuffer(
    width: Int,
    height: Int,
    contentScale: Double = 1.0,
    format: ColorFormat = ColorFormat.RGBa,
    type: ColorType = ColorType.UINT8,
    multisample: BufferMultisample = BufferMultisample.Disabled,
    levels: Int = 1,
    session: Session? = Session.active
) = ResizableColorBuffer(width, height, contentScale, format, type, multisample, levels, session)