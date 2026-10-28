package org.openrndr.draw

/**
 * Imports macOS IOSurfaces as color buffers that sample the IOSurface directly, without copying it.
 *
 * Importers are created by drivers that implement [IOSurfaceImporterFactory]. All functions must be called on
 * the thread that draws.
 * @since openrndr 0.5.1
 */
interface IOSurfaceImporter : AutoCloseable {
    /**
     * Returns a color buffer that samples [ioSurface], an `IOSurfaceRef`.
     *
     * The importer owns the color buffer and returns the same color buffer for the same IOSurface. The contents of
     * the color buffer are the current contents of the IOSurface.
     * @return the color buffer, or null when the IOSurface can not be imported
     */
    fun import(ioSurface: Long): ColorBuffer?

    /**
     * Runs [action] once the GPU has finished the commands issued so far, use it to release an IOSurface only
     * after the GPU is done sampling it
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
}

/**
 * Implemented by drivers that can import IOSurfaces
 * @since openrndr 0.5.1
 */
interface IOSurfaceImporterFactory {
    /**
     * Creates an [IOSurfaceImporter]
     * @return the importer, or null when IOSurfaces can not be imported without copying them
     */
    fun createIOSurfaceImporter(): IOSurfaceImporter?
}
