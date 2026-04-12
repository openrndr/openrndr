@file:JvmName("CubemapFunctions")

package org.openrndr.draw

import org.openrndr.internal.CubemapImageData
import org.openrndr.internal.Driver
import org.openrndr.math.Vector3
import org.openrndr.utils.buffer.MPPBuffer
import kotlin.jvm.JvmName

/**
 * Represents the six sides of a cubemap, each associated with a forward and up direction.
 *
 * @property forward The forward direction vector for the cubemap side.
 * @property up The up direction vector for the cubemap side.
 */
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

/**
 * Represents a cubemap, which is a collection of six 2D textures arranged to form the faces of a cube.
 * It is often used for environment mapping and skyboxes in 3D rendering.
 *
 * Properties:
 * @property session The session associated with this cubemap, or null if no session is associated.
 * @property width The width of each side of the cubemap in pixels.
 * @property format The color format of the cubemap sides.
 * @property type The color data type of the cubemap sides.
 * @property levels The number of mipmap levels of the cubemap.
 */
expect interface Cubemap: Texture, AutoCloseable {
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

/**
 * Creates a cubemap texture with the specified parameters.
 *
 * @param width The width of the cubemap texture. All six faces will have this width and height (i.e., width x width).
 * @param format The color format of the texture. Defaults to [ColorFormat.RGBa].
 * @param type The color type of the texture. Defaults to the value determined by [defaultColorType] for the given format.
 * @param levels The number of mipmap levels. Defaults to 1.
 * @param session The [Session] associated with this texture. Defaults to the currently active session.
 * @return A new [Cubemap] texture with the specified configuration.
 */
fun cubemap(
    width: Int,
    format: ColorFormat = ColorFormat.RGBa,
    type: ColorType = defaultColorType(format),
    levels: Int = 1,
    session: Session? = Session.active
): Cubemap {
    return Driver.instance.createCubemap(width, format, type, levels, session)
}


/**
 * Loads a cubemap from the given CubemapImageData and returns the generated Cubemap instance.
 * This function handles writing the six faces of the cubemap and can optionally use a specified rendering session.
 *
 * @param data The cubemap image data containing parameters such as width, format, type, mipmaps, and the actual image data for each face of the cubemap.
 * @param session The rendering session in which the cubemap will be created and managed. If omitted, the active session will be used.
 * @return The created Cubemap instance initialized with the provided image data.
 */
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