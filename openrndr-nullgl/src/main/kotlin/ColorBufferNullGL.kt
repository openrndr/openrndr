package org.openrndr.internal.nullgl

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.internal.ImageSaveConfiguration
import org.openrndr.shape.IntRectangle
import org.openrndr.utils.buffer.MPPBuffer
import java.io.File
import java.nio.ByteBuffer

class ColorBufferNullGL(
    override val width: Int,
    override val height: Int,
    override val contentScale: Double,
    override val format: ColorFormat,
    override val type: ColorType,
    override val levels: Int,
    override val multisample: BufferMultisample,
    override val session: Session?
) : ColorBuffer() {

    override fun close() {
        destroy()
    }

    override fun saveToFile(file: File, imageFileFormat: ImageFileFormat, async: Boolean) {

    }

    override fun saveToFile(file: File, async: Boolean, configuration: ImageSaveConfiguration) {

    }

    override fun toDataUrl(imageFileFormat: ImageFileFormat): String {
        return ""
    }

    override fun destroy() {

    }

    override fun bind(unit: Int) {

    }

    override fun write(sourceBuffer: ByteBuffer, sourceFormat: ColorFormat, sourceType: ColorType, level: Int) {

    }

    override fun write(
        sourceBuffer: MPPBuffer,
        sourceFormat: ColorFormat,
        sourceType: ColorType,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        level: Int
    ) {

    }

    override fun read(targetBuffer: ByteBuffer, targetFormat: ColorFormat, targetType: ColorType, level: Int) {

    }

    override fun generateMipmaps() {

    }

    override fun copyTo(target: ColorBuffer, fromLevel: Int, toLevel: Int, filter: MagnifyingFilter) {

    }

    override fun copyTo(
        target: ColorBuffer,
        fromLevel: Int,
        toLevel: Int,
        sourceRectangle: IntRectangle,
        targetRectangle: IntRectangle,
        filter: MagnifyingFilter
    ) {

    }

    override fun copyTo(target: ArrayTexture, layer: Int, fromLevel: Int, toLevel: Int) {

    }

    override fun fill(color: ColorRGBa, level: Int) {

    }

    override var wrapU = WrapMode.CLAMP_TO_EDGE
    override var wrapV = WrapMode.CLAMP_TO_EDGE

    override var flipV = false

    override var filterMin = MinifyingFilter.LINEAR
    override var filterMag = MagnifyingFilter.LINEAR

    override val shadow: ColorBufferShadow
        get() = TODO("Not yet implemented")

    override fun filter(filterMin: MinifyingFilter, filterMag: MagnifyingFilter) {

    }

    override var anisotropy = 0.0


}