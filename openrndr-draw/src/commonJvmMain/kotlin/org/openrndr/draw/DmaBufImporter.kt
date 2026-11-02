package org.openrndr.draw

/**
 * Imports Linux DMA-BUF planes as color buffers that sample the DMA-BUF directly, without copying it.
 *
 * Importers are created by drivers that implement [DmaBufImporterFactory]. All functions must be called on the
 * thread that draws.
 * @since openrndr 0.5.1
 */
interface DmaBufImporter : AutoCloseable {
    /**
     * Returns a color buffer that samples a single plane DMA-BUF image of [width] x [height] pixels in the DRM
     * format [fourcc], stored at [offset] in the DMA-BUF [fd] with rows of [pitch] bytes. [modifier] is the DRM
     * format modifier of the DMA-BUF, or [DRM_FORMAT_MOD_INVALID] when it is not known.
     *
     * Supported are the formats `R8`, and `GR88` and `RG88`, imported with [ColorFormat.R] and [ColorFormat.RG]
     * and [ColorType.UINT8], and `R16`, and `GR1616` and `RG1616`, imported with [ColorFormat.R] and
     * [ColorFormat.RG] and [ColorType.UINT16]. These are the formats of the luma and chroma planes of NV12 and P010
     * frames.
     *
     * The importer does not take ownership of [fd], it can be closed once this function returns. The contents of
     * the color buffer are the current contents of the DMA-BUF.
     * @return the color buffer, or null when the plane can not be imported
     */
    fun import(fd: Int, width: Int, height: Int, fourcc: Int, offset: Int, pitch: Int, modifier: Long): ColorBuffer?

    /**
     * Destroys [colorBuffer], a color buffer returned by [import]
     */
    fun release(colorBuffer: ColorBuffer)

    /**
     * Runs [action] once the GPU has finished the commands issued so far, use it to release a DMA-BUF only after
     * the GPU is done sampling it
     */
    fun runWhenGpuFinished(action: () -> Unit)

    /**
     * Runs the actions passed to [runWhenGpuFinished] whose commands have finished
     */
    fun update()

    /**
     * Waits for the GPU, runs all pending actions and destroys the imported color buffers
     */
    override fun close()

    companion object {
        /** the DRM format modifier of DMA-BUFs whose modifier is not known */
        const val DRM_FORMAT_MOD_INVALID = 0x00ffffffffffffffL
    }
}

/**
 * Implemented by drivers that can import DMA-BUFs
 * @since openrndr 0.5.1
 */
interface DmaBufImporterFactory {
    /**
     * Creates a [DmaBufImporter] for the current context
     * @return the importer, or null when DMA-BUFs can not be imported without copying them
     */
    fun createDmaBufImporter(): DmaBufImporter?
}
