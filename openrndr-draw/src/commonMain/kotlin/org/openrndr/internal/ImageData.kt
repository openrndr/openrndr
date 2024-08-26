package org.openrndr.internal

import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.CubemapSide
import org.openrndr.utils.buffer.MPPBuffer

abstract class ImageData(
    val width: Int,
    val height: Int,
    val format: ColorFormat,
    val type: ColorType,
    val flipV: Boolean,
    var data: MPPBuffer?,
    val mipmapData: List<MPPBuffer> = emptyList(),
) : AutoCloseable

abstract class CubemapImageData(
    val width: Int,
    val height: Int,
    val format: ColorFormat,
    val type: ColorType,
    val mipmaps: Int,
    val sides: List<MPPBuffer>,
    val mipmapSides: List<MPPBuffer>,
)  : AutoCloseable {

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