package org.openrndr.internal

import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.CubemapSide
import org.openrndr.utils.buffer.MPPBuffer

/**
 * Represents image data with specific properties such as dimensions, format, type, and optional mipmap levels.
 *
 * This is an abstract class used as a base for implementing various image data-related structures or functionalities.
 *
 * @property width The width of the image in pixels.
 * @property height The height of the image in pixels.
 * @property format The color format of the image, which defines its color channels and their arrangement.
 * @property type The color type of the image, which determines the data type and bit-depth of each component.
 * @property flipV Indicates if the image data is vertically flipped or not.
 * @property data An optional buffer containing the raw image data.
 * @property mipmapData A list of buffers for mipmap levels of the image, defaulting to an empty list.
 */
abstract class ImageData(
    val width: Int,
    val height: Int,
    val format: ColorFormat,
    val type: ColorType,
    val flipV: Boolean,
    var data: MPPBuffer?,
    val mipmapData: List<MPPBuffer> = emptyList(),
) : AutoCloseable

/**
 * Represents the image data of a cubemap, a texture with six sides corresponding to the faces of a cube.
 * The data is stored for each side and its corresponding mipmap levels if applicable.
 *
 * @property width The width of each side of the cubemap.
 * @property height The height of each side of the cubemap.
 * @property format The color format used for the cubemap image data.
 * @property type The color type used for the cubemap image data, defining the bit depth and other characteristics.
 * @property mipmaps The number of mipmap levels available for the cubemap image.
 * @property sides The list of data buffers for each cubemap face, corresponding to the base level (level 0).
 * @property mipmapSides The list of data buffers for mipmap levels beyond the base level for each cubemap face.
 */
abstract class CubemapImageData(
    val width: Int,
    val height: Int,
    val format: ColorFormat,
    val type: ColorType,
    val mipmaps: Int,
    val sides: List<MPPBuffer>,
    val mipmapSides: List<MPPBuffer>,
)  : AutoCloseable {

    /**
     * Retrieves the data buffer for a specific side of the cubemap at the specified mipmap level.
     * For the base level (level 0), the data is retrieved from the `sides` property,
     * and for other mipmap levels, the data is retrieved from the `mipmapSides` property.
     *
     * @param cubemapSide The side of the cubemap to retrieve the data for, represented by the `CubemapSide` enum.
     * @param level Mipmap level of the cubemap to retrieve the data for. Level 0 corresponds to the base level,
     * while higher levels correspond to additional mipmap levels.
     * @return The data buffer corresponding to the specified cubemap side and mipmap level.
     */
    fun side(cubemapSide: CubemapSide, level: Int) =
        when (cubemapSide) {
            CubemapSide.POSITIVE_X -> if (level == 0) sides[0] else mipmapSides[(level - 1) + 0 * (mipmaps - 1)]
            CubemapSide.POSITIVE_Y -> if (level == 0) sides[2] else mipmapSides[(level - 1) + 2 * (mipmaps - 1)]
            CubemapSide.POSITIVE_Z -> if (level == 0) sides[4] else mipmapSides[(level - 1) + 4 * (mipmaps - 1)]
            CubemapSide.NEGATIVE_X -> if (level == 0) sides[1] else mipmapSides[(level - 1) + 1 * (mipmaps - 1)]
            CubemapSide.NEGATIVE_Y -> if (level == 0) sides[3] else mipmapSides[(level - 1) + 3 * (mipmaps - 1)]
            CubemapSide.NEGATIVE_Z -> if (level == 0) sides[5] else mipmapSides[(level - 1) + 5 * (mipmaps - 1)]
        }
}