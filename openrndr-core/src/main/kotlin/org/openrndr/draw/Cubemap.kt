package org.openrndr.draw

import org.openrndr.internal.Driver

enum class CubemapSide {
    NEGATIVE_X,
    POSITIVE_X,
    NEGATIVE_Y,
    POSITIVE_Y,
    NEGATIVE_Z,
    POSITIVE_Z
}

interface Cubemap {

    companion object {
        fun create(width: Int, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8): Cubemap {
            return Driver.instance.createCubemap(width, format, type)
        }

        fun fromUrl(url:String):Cubemap {
            return Driver.instance.createCubemapFromUrls(listOf(url))
        }


        fun fromUrls(urls:List<String>):Cubemap {
            return Driver.instance.createCubemapFromUrls(urls)
        }
    }

    val width: Int
    fun filter(min: MinifyingFilter, mag: MagnifyingFilter)
    fun side(side: CubemapSide): ColorBuffer
    fun bind(textureUnit: Int = 0)
    fun generateMipmaps()
    fun destroy()
}

fun cubemap(width: Int, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8): Cubemap {
    return Cubemap.create(width, format, type)
}