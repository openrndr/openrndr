package org.openrndr.draw

import org.openrndr.internal.Driver
import org.openrndr.utils.buffer.MPPBuffer
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.nio.ByteBuffer

actual interface Cubemap {

    companion object {
        fun create(
            width: Int,
            format: ColorFormat = ColorFormat.RGBa,
            type: ColorType = ColorType.UINT8,
            levels: Int = -1,
            session: Session? = Session.active
        ): Cubemap {
            return Driver.instance.createCubemap(width, format, type, levels, session)
        }

        fun fromUrl(url: String, formatHint: ImageFileFormat?, session: Session? = Session.active): Cubemap {
            return Driver.instance.createCubemapFromUrls(listOf(url), formatHint, session)
        }

        fun fromUrls(urls: List<String>, formatHint: ImageFileFormat?, session: Session? = Session.active): Cubemap {
            return Driver.instance.createCubemapFromUrls(urls, formatHint, session)
        }

        fun fromFile(file: File, formatHint: ImageFileFormat?, session: Session? = Session.active): Cubemap {
            return Driver.instance.createCubemapFromFiles(listOf(file.absolutePath), formatHint, session)
        }

        fun fromFiles(filenames: List<File>, formatHint: ImageFileFormat?, session: Session? = Session.active): Cubemap {
            return Driver.instance.createCubemapFromFiles(filenames.map { it.absolutePath }, formatHint, session)
        }
    }



    fun read(side: CubemapSide, target: ByteBuffer, targetFormat: ColorFormat = format, targetType: ColorType = type, level: Int = 0)
    fun write(side: CubemapSide, source: ByteBuffer, sourceFormat: ColorFormat = format, sourceType: ColorType = type, level: Int = 0)
    actual val session: Session?
    actual val width: Int
    actual val format: ColorFormat
    actual val type: ColorType
    actual val levels: Int
    actual fun copyTo(
        target: ArrayCubemap,
        layer: Int,
        fromLevel: Int,
        toLevel: Int
    )

    actual fun copyTo(target: Cubemap, fromLevel: Int, toLevel: Int)
    actual fun copyTo(
        target: ColorBuffer,
        fromSide: CubemapSide,
        fromLevel: Int,
        toLevel: Int
    )

    actual fun filter(min: MinifyingFilter, mag: MagnifyingFilter)
    actual fun bind(textureUnit: Int)
    actual fun generateMipmaps()
    actual fun destroy()
    actual fun write(
        side: CubemapSide,
        source: MPPBuffer,
        sourceFormat: ColorFormat,
        sourceType: ColorType,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        level: Int
    )
}

fun loadCubemap(fileOrUrl: String, formatHint: ImageFileFormat?, session: Session? = Session.active): Cubemap {
    return try {
        if (!fileOrUrl.startsWith("data:")) {
            URL(fileOrUrl)
        }
        Cubemap.fromUrl(fileOrUrl, formatHint, session)
    } catch (e: MalformedURLException) {
        Cubemap.fromFile(File(fileOrUrl), formatHint, session)
    }
}