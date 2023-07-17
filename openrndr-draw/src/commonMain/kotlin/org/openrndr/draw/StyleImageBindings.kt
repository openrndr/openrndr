package org.openrndr.draw

interface StyleImageBindings {
    val imageValues: MutableMap<String, ImageBinding>
    val imageTypes: MutableMap<String, String>
    val imageAccess: MutableMap<String, ImageAccess>
    val imageFlags: MutableMap<String, Set<ImageFlag>>

    @Deprecated("renamed to image", ReplaceWith("image"))
    fun parameter(name: String, value: ImageBinding) {
        image(name, value)
    }

    fun StyleImageBindings.registerImageBinding(
        name: String,
        access: ImageAccess = ImageAccess.READ_WRITE,
        flags: Set<ImageFlag>
    ) {
        imageAccess[name] = access
        imageFlags[name] = flags
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
        val imageBinding =
            colorBuffer.imageBinding(level, imageAccess[name] ?: error("image binding '$name' is not registered"))
        image(name, imageBinding)
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
