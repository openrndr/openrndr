package org.openrndr.draw

interface StyleImageBindings {
    val imageValues: MutableMap<String, Array<out ImageBinding>>
    val imageTypes: MutableMap<String, String>
    val imageAccess: MutableMap<String, ImageAccess>
    val imageFlags: MutableMap<String, Set<ImageFlag>>
    val imageArrayLength: MutableMap<String, Int>
    val imageBindings: MutableMap<String, Int>

    @Deprecated("renamed to image", ReplaceWith("image"))
    fun parameter(name: String, value: ImageBinding) {
        image(name, value)
    }

    /**
     * @since 0.4.4
     */
    fun StyleImageBindings.registerImageBinding(
        name: String,
        access: ImageAccess = ImageAccess.READ_WRITE,
        flags: Set<ImageFlag>,
        arrayLength: Int = -1
    ) {
        imageAccess[name] = access
        imageFlags[name] = flags
        imageArrayLength[name] = arrayLength
        imageBindings.getOrPut(name) { imageBindings.size }
    }

    /**
     * @since 0.4.4
     */
    fun image(name: String, value: ImageBinding) {
        if (imageAccess[name] != null) {
            require(imageAccess[name] == value.access) {
                "expected image access ${imageAccess[name]}, got ${value.access}"
            }
        } else {
            imageAccess[name] = value.access
        }
        imageValues[name] = arrayOf(value)
        imageTypes[name] = imageBindingType(value)
        imageArrayLength[name] = -1
        imageBindings.getOrPut(name) { imageBindings.size }
    }

    /**
     * @since 0.4.4
     */
    fun image(name: String, values: Array<out ImageBinding>) {
        require(values.isNotEmpty())
        require(values.all { it.access == values[0].access })

        if (imageAccess[name] != null) {
            require(imageAccess[name] == values[0].access) {
                "expected image access ${imageAccess[name]}, got ${values[0].access}"
            }
        } else {
            imageAccess[name] = values[0].access
        }
        imageValues[name] = values
        imageTypes[name] = imageBindingType(values[0])
        imageArrayLength[name] = values.size
        imageBindings.getOrPut(name) { imageBindings.size }
    }

    /**
     * @since 0.4.4
     */
    fun image(name: String, arrayTexture: ArrayTexture, level: Int = 0) {
        require(imageAccess[name] != null) {
            "image binding '$name' is not registered"
        }
        val imageBinding =
            arrayTexture.imageBinding(level, imageAccess[name] ?: error("image binding '$name' is not registered"))
        image(name, imageBinding)
    }

    /**
     * @since 0.4.4
     */
    fun image(
        name: String,
        arrayTextures: Array<ArrayTexture>,
        levels: Array<Int> = Array(arrayTextures.size) { 0 }
    ) {
        require(arrayTextures.isNotEmpty())
        require(imageAccess[name] != null) {
            "image binding '$name' is not registered"
        }
        val imageBindings = arrayTextures.mapIndexed { index, it ->
            it.imageBinding(levels[index], imageAccess[name]!!)
        }.toTypedArray()
        image(name, imageBindings)
    }

    /**
     * @since 0.4.4
     */
    fun image(name: String, colorBuffer: ColorBuffer, level: Int = 0) {
        require(imageAccess[name] != null) {
            "image binding '$name' is not registered"
        }
        val imageBinding =
            colorBuffer.imageBinding(level, imageAccess[name] ?: error("image binding '$name' is not registered"))
        image(name, imageBinding)
    }

    /**
     * @since 0.4.4
     */
    fun image(
        name: String,
        colorBuffers: Array<ColorBuffer>,
        levels: Array<Int> = Array(colorBuffers.size) { 0 }
    ) {
        require(colorBuffers.isNotEmpty())
        require(imageAccess[name] != null) {
            "image binding '$name' is not registered"
        }
        val imageBindings = colorBuffers.mapIndexed { index, it ->
            it.imageBinding(levels[index], imageAccess[name]!!)
        }.toTypedArray()
        image(name, imageBindings)
    }

    /**
     * @since 0.4.4
     */
    fun image(name: String, cubemap: Cubemap, level: Int = 0) {
        require(imageAccess[name] != null) {
            "image binding '$name' is not registered"
        }
        val imageBinding =
            cubemap.imageBinding(level, imageAccess[name] ?: error("image binding '$name' is not registered"))
        image(name, imageBinding)
    }

    /**
     * @since 0.4.4
     */
    fun image(
        name: String,
        cubemaps: Array<Cubemap>,
        levels: Array<Int> = Array(cubemaps.size) { 0 }
    ) {
        require(cubemaps.isNotEmpty())
        require(imageAccess[name] != null) {
            "image binding '$name' is not registered"
        }
        val imageBindings = cubemaps.mapIndexed { index, it ->
            it.imageBinding(levels[index], imageAccess[name]!!)
        }.toTypedArray()
        image(name, imageBindings)
    }

    /**
     * @since 0.4.4
     */
    fun image(name: String, volumeTexture: VolumeTexture, level: Int = 0) {
        require(imageAccess[name] != null) {
            "image binding '$name' is not registered"
        }
        val imageBinding = volumeTexture.imageBinding(level, imageAccess[name]!!)
        image(name, imageBinding)
    }

    /**
     * @since 0.4.4
     */
    fun image(
        name: String,
        volumeTextures: Array<VolumeTexture>,
        levels: Array<Int> = Array(volumeTextures.size) { 0 }
    ) {
        require(volumeTextures.isNotEmpty())
        require(imageAccess[name] != null) {
            "image binding '$name' is not registered"
        }
        val imageBindings = volumeTextures.mapIndexed { index, it ->
            it.imageBinding(levels[index], imageAccess[name]!!)
        }.toTypedArray()
        image(name, imageBindings)
    }

    /**
     * @since 0.4.4
     */
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
