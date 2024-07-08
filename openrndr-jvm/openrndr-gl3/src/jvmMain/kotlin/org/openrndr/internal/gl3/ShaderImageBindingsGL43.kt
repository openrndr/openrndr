package org.openrndr.internal.gl3

import org.lwjgl.opengl.GL11C.GL_INVALID_ENUM
import org.lwjgl.opengl.GL11C.GL_INVALID_VALUE
import org.openrndr.draw.*
import org.openrndr.internal.Driver

interface ShaderImageBindingsGL43 : ShaderImageBindings, ShaderUniformsGL3 {

    override fun image(name: String, image: Int, imageBinding: ImageBinding) {
        (Driver.instance as DriverGL3).version.require(DriverVersionGL.GL_VERSION_4_3)

        when (imageBinding) {
            is BufferTextureImageBinding -> {
                val bufferTexture = imageBinding.bufferTexture as BufferTextureGL3
                require(bufferTexture.format.componentCount != 3) {
                    "color buffer has unsupported format (${imageBinding.bufferTexture.format}), only formats with 1, 2 or 4 components are supported"
                }
                glBindImageTexture(
                    image,
                    bufferTexture.texture,
                    imageBinding.level,
                    false,
                    0,
                    imageBinding.access.gl(),
                    bufferTexture.glFormat()
                )
            }

            is ColorBufferImageBinding -> {
                val colorBuffer = imageBinding.colorBuffer as ColorBufferGL3
                require(colorBuffer.format.componentCount != 3) {
                    "color buffer has unsupported format (${imageBinding.colorBuffer.format}), only formats with 1, 2 or 4 components are supported"
                }
                glBindImageTexture(
                    image,
                    colorBuffer.texture,
                    imageBinding.level,
                    false,
                    0,
                    imageBinding.access.gl(),
                    colorBuffer.glFormat()
                )
            }

            is ArrayTextureImageBinding -> {
                val arrayTexture = imageBinding.arrayTexture as ArrayTextureGL3
                require(arrayTexture.format.componentCount != 3) {
                    "color buffer has unsupported format (${imageBinding.arrayTexture.format}), only formats with 1, 2 or 4 components are supported"
                }
                glBindImageTexture(
                    image,
                    arrayTexture.texture,
                    imageBinding.level,
                    true,
                    0,
                    imageBinding.access.gl(),
                    arrayTexture.glFormat()
                )
            }

            is CubemapImageBinding -> {
                val cubemap = imageBinding.cubemap as CubemapGL3
                require(cubemap.format.componentCount != 3) {
                    "color buffer has unsupported format (${imageBinding.cubemap.format}), only formats with 1, 2 or 4 components are supported"
                }
                glBindImageTexture(
                    image,
                    cubemap.texture,
                    imageBinding.level,
                    false,
                    0,
                    imageBinding.access.gl(),
                    cubemap.glFormat()
                )
            }

            is ArrayCubemapImageBinding -> {
                val arrayCubemap = imageBinding.arrayCubemap as ArrayCubemapGL4
                require(arrayCubemap.format.componentCount != 3) {
                    "color buffer has unsupported format (${imageBinding.arrayCubemap.format}), only formats with 1, 2 or 4 components are supported"
                }
                glBindImageTexture(
                    image,
                    arrayCubemap.texture,
                    imageBinding.level,
                    false,
                    0,
                    imageBinding.access.gl(),
                    arrayCubemap.glFormat()
                )
            }

            is VolumeTextureImageBinding -> {
                val volumeTexture = imageBinding.volumeTexture as VolumeTextureGL3
                require(volumeTexture.format.componentCount != 3) {
                    "color buffer has unsupported format (${imageBinding.volumeTexture.format}), only formats with 1, 2 or 4 components are supported"
                }
                glBindImageTexture(
                    image,
                    volumeTexture.texture,
                    imageBinding.level,
                    true,
                    0,
                    imageBinding.access.gl(),
                    volumeTexture.glFormat()
                )
                debugGLErrors {
                    when (it) {
                        GL_INVALID_VALUE -> "unit ${image} greater than or equal to the value of GL_MAX_IMAGE_UNITS. OR texture (${volumeTexture.texture} is not the name of an existing texture object. OR level (${imageBinding.level} or layer (0) is less than zero."
                        GL_INVALID_ENUM -> "access or format is not one of the supported tokens."
                        else -> null
                    }
                }
            }

            else -> error("unsupported binding")
        }
        debugGLErrors()
        val index = uniformIndex(name)
        glProgramUniform1i(programObject, index, image)
        debugGLErrors()
    }

    override fun image(name: String, image: Int, imageBinding: Array<out ImageBinding>) {
        for (i in imageBinding.indices) {
            image("${name}[$i]", image + i, imageBinding[i])
        }
    }
}