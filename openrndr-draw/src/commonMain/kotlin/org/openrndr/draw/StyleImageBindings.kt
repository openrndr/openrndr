package org.openrndr.draw

interface StyleImageBindings {

    val imageValues: MutableMap<String, ImageBinding>
    val imageTypes : MutableMap<String, String>
    val imageAccess: MutableMap<String, ImageAccess>

    @Deprecated("renamed to image", ReplaceWith("image"))
    fun parameter(name: String, value: ImageBinding) {
        image(name, value)
    }

    fun StyleImageBindings.registerImageBinding(name: String, access: ImageAccess = ImageAccess.READ_WRITE) {
        imageAccess[name] = access
    }

    fun image(name: String, value: ImageBinding) {
        if (imageAccess[name] != null) {
            require(imageAccess[name] == value.access) {
                "expected image access ${imageAccess[name]}, got ${value.access}"
            }
        } else {
            imageAccess[name] = value.access
        }
        imageValues[name] = value
        imageTypes[name] = imageBindingType(value)
    }

    fun image(name: String, colorBuffer: ColorBuffer, level: Int = 0) {
        require(imageAccess[name] != null) {
            "image binding '$name' is not registered"
        }
        colorBuffer.imageBinding(level, imageAccess[name]!!)
    }

    fun image(name: String, volumeTexture: VolumeTexture, level: Int = 0) {
        require(imageAccess[name] != null) {
            "image binding '$name' is not registered"
        }
        volumeTexture.imageBinding(level, imageAccess[name]!!)
    }


    fun imageBindingType(value: ImageBinding): String {
        return when (value) {
            is BufferTextureImageBinding -> {
                "ImageBuffer,${value.bufferTexture.format.name},${value.bufferTexture.type.name}"
            }

            is CubemapImageBinding -> {
                "ImageCube,${value.cubemap.format.name},${value.cubemap.type.name}"
            }

            is ArrayCubemapImageBinding -> {
                "ImageCubeArray,${value.arrayCubemap.format.name},${value.arrayCubemap.type.name}"
            }

            is ColorBufferImageBinding -> {
                "Image2D,${value.colorBuffer.format.name},${value.colorBuffer.type.name}"
            }

            is ArrayTextureImageBinding -> {
                "Image2DArray,${value.arrayTexture.format.name},${value.arrayTexture.type.name}"
            }

            is VolumeTextureImageBinding -> {
                "Image3D,${value.volumeTexture.format.name},${value.volumeTexture.type.name}"
            }

            else -> error("unsupported image binding")
        }
    }
}

/*
inline fun <reified T:ImageBinding> imageBindingName() : String {
    return when (T::class) {
        BufferTextureImageBinding::class -> "ImageBuffer"
        CubemapImageBinding::class -> "ImageCube"
        ArrayCubemapImageBinding::class -> "ImageCubeArray"
        ColorBufferImageBinding::class -> "Image2D"
        ArrayTextureImageBinding::class -> "Image2DArray"
        VolumeTextureImageBinding::class -> "Image3D"
        else -> error("no image binding type translation for '${T::class.simpleName}'")
    }
}*/
