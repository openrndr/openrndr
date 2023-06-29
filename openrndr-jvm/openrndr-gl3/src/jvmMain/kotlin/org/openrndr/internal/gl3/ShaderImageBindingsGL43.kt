package org.openrndr.internal.gl3

import org.lwjgl.opengl.GL43C
import org.openrndr.draw.*
import org.openrndr.internal.Driver

interface ShaderImageBindingsGL43 : ShaderImageBindings, ShaderUniformsGL3{

    override fun image(name: String, image: Int, imageBinding: ImageBinding) {
        (Driver.instance as DriverGL3).version.require(DriverVersionGL.VERSION_4_3)

        when (imageBinding) {
            is BufferTextureImageBinding -> {
                val bufferTexture = imageBinding.bufferTexture as BufferTextureGL3
                require(bufferTexture.format.componentCount != 3) {
                    "color buffer has unsupported format (${imageBinding.bufferTexture.format}), only formats with 1, 2 or 4 components are supported"
                }
                GL43C.glBindImageTexture(
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
                GL43C.glBindImageTexture(
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
                GL43C.glBindImageTexture(
                    image,
                    arrayTexture.texture,
                    imageBinding.level,
                    false,
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
                GL43C.glBindImageTexture(
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
                GL43C.glBindImageTexture(
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
                GL43C.glBindImageTexture(
                    image,
                    volumeTexture.texture,
                    imageBinding.level,
                    false,
                    0,
                    imageBinding.access.gl(),
                    volumeTexture.glFormat()
                )
            }

            else -> error("unsupported binding")
        }
        checkGLErrors()
        val index = uniformIndex(name)
        GL43C.glProgramUniform1i(programObject, index, image)
        checkGLErrors()
    }

}