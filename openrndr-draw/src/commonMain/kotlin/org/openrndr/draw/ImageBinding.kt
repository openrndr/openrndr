package org.openrndr.draw

import org.openrndr.draw.font.BufferAccess
import org.openrndr.draw.font.BufferFlag

typealias ImageAccess = BufferAccess
typealias ImageFlag = BufferFlag

sealed class ImageBinding(val level: Int, val access: ImageAccess)
class ColorBufferImageBinding(val colorBuffer: ColorBuffer, level: Int, imageAccess: ImageAccess) :
    ImageBinding(level, imageAccess)

class CubemapImageBinding(val cubemap: Cubemap, level: Int, imageAccess: ImageAccess) : ImageBinding(level, imageAccess)
class CubemapLayerImageBinding(val cubemap: Cubemap, val side: CubemapSide, level: Int, imageAccess: ImageAccess) :
    ImageBinding(level, imageAccess)

class VolumeTextureImageBinding(val volumeTexture: VolumeTexture, level: Int, imageAccess: ImageAccess) :
    ImageBinding(level, imageAccess)

class VolumeTextureLayerImageBinding(
    val volumeTexture: VolumeTexture,
    val layer: Int,
    level: Int,
    imageAccess: ImageAccess
) : ImageBinding(level, imageAccess)

class ArrayTextureImageBinding(val arrayTexture: ArrayTexture, level: Int, imageAccess: ImageAccess) :
    ImageBinding(level, imageAccess)

class ArrayTextureLayerImageBinding(val arrayTexture: ArrayTexture, level: Int, imageAccess: ImageAccess) :
    ImageBinding(level, imageAccess)

class ArrayCubemapImageBinding(val arrayCubemap: ArrayCubemap, level: Int, imageAccess: ImageAccess) :
    ImageBinding(level, imageAccess)

class BufferTextureImageBinding(val bufferTexture: BufferTexture, imageAccess: ImageAccess) :
    ImageBinding(0, imageAccess)


fun BufferTexture.imageBinding(imageAccess: ImageAccess) = BufferTextureImageBinding(this, imageAccess)
fun ColorBuffer.imageBinding(level: Int = 0, imageAccess: ImageAccess) =
    ColorBufferImageBinding(this, level, imageAccess)

fun Cubemap.imageBinding(level: Int = 0, imageAccess: ImageAccess) = CubemapImageBinding(this, level, imageAccess)
fun ArrayTexture.imageBinding(level: Int = 0, imageAccess: ImageAccess) =
    ArrayTextureImageBinding(this, level, imageAccess)

fun VolumeTexture.imageBinding(level: Int = 0, imageAccess: ImageAccess) : VolumeTextureImageBinding {
    require(level in 0 until levels) { "requested level $level but volume texture has only $levels mip-levels" }
    return VolumeTextureImageBinding(this, level, imageAccess)
}


fun ArrayCubemap.imageBinding(level: Int = 0, imageAccess: ImageAccess) =
    ArrayCubemapImageBinding(this, level, imageAccess)

interface ShaderImageBindings {
    /**
     * Bind image unit to imageBinding
     */
    fun image(name: String, image: Int, imageBinding: ImageBinding)
}
