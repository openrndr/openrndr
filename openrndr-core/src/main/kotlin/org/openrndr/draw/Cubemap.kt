package org.openrndr.draw

import org.openrndr.internal.Driver
import org.openrndr.math.Vector3
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.nio.ByteBuffer

enum class CubemapSide(val forward: Vector3, val up: Vector3) {
    POSITIVE_X(Vector3.UNIT_X, -Vector3.UNIT_Y),
    NEGATIVE_X(-Vector3.UNIT_X, -Vector3.UNIT_Y),
    POSITIVE_Y(Vector3.UNIT_Y, Vector3.UNIT_Z),
    NEGATIVE_Y(-Vector3.UNIT_Y, -Vector3.UNIT_Z),
    POSITIVE_Z(Vector3.UNIT_Z, -Vector3.UNIT_Y),
    NEGATIVE_Z(-Vector3.UNIT_Z, -Vector3.UNIT_Y)
    ;

    val right
        get() = forward cross up
}

interface Cubemap {

    companion object {
        fun create(width: Int, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8, levels: Int = -1, session: Session? = Session.active): Cubemap {
            val cubemap = Driver.instance.createCubemap(width, format, type, levels, session)
            return cubemap
        }

        fun fromUrl(url: String, session: Session? = Session.active): Cubemap {
            val cubemap = Driver.instance.createCubemapFromUrls(listOf(url), session)
            return cubemap
        }

        fun fromUrls(urls: List<String>, session: Session? = Session.active): Cubemap {
            return Driver.instance.createCubemapFromUrls(urls, session)
        }

        fun fromFile(file: File, session: Session? = Session.active): Cubemap {
            return Driver.instance.createCubemapFromFiles(listOf(file.absolutePath), session)
        }

        fun fromFiles(filenames: List<File>, session: Session? = Session.active): Cubemap {
            return Driver.instance.createCubemapFromFiles(filenames.map { it.absolutePath }, session)
        }
    }

    val session: Session?

    val width: Int
    val format: ColorFormat
    val type: ColorType
    val levels: Int

    fun copyTo(target: ArrayCubemap, layer: Int, fromLevel: Int = 0, toLevel: Int = 0)
    fun copyTo(target: Cubemap, fromLevel: Int = 0, toLevel: Int = 0)
    fun copyTo(target: ColorBuffer, fromSide: CubemapSide, fromLevel: Int = 0, toLevel: Int = 0)

    fun filter(min: MinifyingFilter, mag: MagnifyingFilter)
    fun bind(textureUnit: Int = 0)
    fun generateMipmaps()
    fun destroy()

    fun read(side: CubemapSide, target: ByteBuffer, targetFormat: ColorFormat = format, targetType: ColorType = type, level: Int = 0)
    fun write(side: CubemapSide, source: ByteBuffer, sourceFormat: ColorFormat = format, sourceType: ColorType = type, level: Int = 0)
}

fun cubemap(width: Int, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8, levels: Int = 1, session: Session? = Session.active): Cubemap {
    val cubemap = Cubemap.create(width, format, type, levels, session)
    return cubemap
}

fun loadCubemap(fileOrUrl: String, session: Session? = Session.active): Cubemap {
    return try {
        if (!fileOrUrl.startsWith("data:")) {
            URL(fileOrUrl)
        }
        Cubemap.fromUrl(fileOrUrl, session)
    } catch (e: MalformedURLException) {
        Cubemap.fromFile(File(fileOrUrl), session)
    }
}