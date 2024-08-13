@file:JvmName("CubemapFunctions")

package org.openrndr.draw

import org.openrndr.internal.CubemapImageData
import org.openrndr.internal.Driver
import org.openrndr.math.Vector3
import org.openrndr.utils.buffer.MPPBuffer
import kotlin.jvm.JvmName

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

expect interface Cubemap {
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

    fun write(
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

fun cubemap(
    width: Int,
    format: ColorFormat = ColorFormat.RGBa,
    type: ColorType = defaultColorType(format),
    levels: Int = 1,
    session: Session? = Session.active
): Cubemap {
    return Driver.instance.createCubemap(width, format, type, levels, session)
}


fun loadCubemap(data: CubemapImageData, session: Session? = Session.active): Cubemap {
    try {
        val cm = cubemap(data.width, data.format, data.type, data.mipmaps, session)
        for (level in 0 until data.mipmaps) {
            val levelWidth = data.width / (1 shl level)
            for (side in CubemapSide.entries) {
                cm.write(
                    side,
                    data.side(side, level),
                    data.format,
                    data.type,
                    x = 0,
                    y = 0,
                    width = levelWidth,
                    height = levelWidth,
                    level = level
                )
            }
        }
        if (data.mipmaps == 1) {
            cm.generateMipmaps()
        }
        cm.filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.LINEAR)
        return cm
    } finally {
        data.close()
    }
}