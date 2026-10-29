package org.openrndr.draw

/**
 * Imports Direct3D 11 textures of one `ID3D11Device` as color buffers that share the memory of the texture,
 * without copying it.
 *
 * Importers are created by drivers that implement [D3D11TextureImporterFactory]. All functions must be called on
 * the thread that draws.
 * @since openrndr 0.5.1
 */
interface D3D11TextureImporter : AutoCloseable {
    /**
     * Returns a color buffer that shares [texture], an `ID3D11Texture2D` of [width] x [height] pixels that was
     * created on the device of the importer with `D3D11_BIND_RENDER_TARGET` and `D3D11_BIND_SHADER_RESOURCE`.
     *
     * The color buffer reports [format] and [type]. It can only be sampled between [lock] and [unlock], Direct3D
     * can only write [texture] while it is unlocked.
     * @return the color buffer, or null when the texture can not be imported
     */
    fun import(texture: Long, width: Int, height: Int, format: ColorFormat, type: ColorType): ColorBuffer?

    /**
     * Makes [colorBuffer] available to draw with, waiting for the Direct3D commands that write it
     * @return true when the color buffer was locked
     */
    fun lock(colorBuffer: ColorBuffer): Boolean

    /**
     * Returns [colorBuffer] to Direct3D
     */
    fun unlock(colorBuffer: ColorBuffer)

    /**
     * Stops sharing the texture of [colorBuffer] and destroys [colorBuffer]
     */
    fun release(colorBuffer: ColorBuffer)

    /**
     * Releases all imported color buffers, the device can be destroyed afterwards
     */
    override fun close()
}

/**
 * Implemented by drivers that can import Direct3D 11 textures
 * @since openrndr 0.5.1
 */
interface D3D11TextureImporterFactory {
    /**
     * Can Direct3D 11 textures be imported without copying them?
     */
    val supportsD3D11TextureImport: Boolean

    /**
     * Creates a [D3D11TextureImporter] for [device], an `ID3D11Device`
     * @return the importer, or null when textures of [device] can not be imported
     */
    fun createD3D11TextureImporter(device: Long): D3D11TextureImporter?
}
