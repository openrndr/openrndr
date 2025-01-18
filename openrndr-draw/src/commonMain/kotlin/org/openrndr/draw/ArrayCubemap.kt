package org.openrndr.draw

/**
 * Array of [Cubemap], all with the same [ColorType], [ColorFormat] and other
 * properties. Typically, used for efficient reflection probes, lighting and shadowing
 * systems. This is a common interface to be implemented in various
 * target languages.
 */
expect interface ArrayCubemap : AutoCloseable {
    val session: Session?

    val width: Int
    val layers: Int
    val format: ColorFormat
    val type: ColorType
    val levels: Int
    fun destroy()

    fun bind(unit: Int)

    /**
     * Copy contents in layer to target ColorBuffer
     * @param layer the layer to copy contents from
     * @param target the target to copy contents to
     */
    fun copyTo(layer: Int, target: Cubemap, fromLevel: Int = 0, toLevel: Int = 0)

    /**
     * Copy contents in layer to target ColorBuffer
     * @param layer the layer to copy contents from
     * @param target the layer array texture to copy contents to
     * @param targetLayer the layer of the target array texture to copy contents to
     */
    fun copyTo(layer: Int, target: ArrayCubemap, targetLayer: Int, fromLevel: Int = 0, toLevel: Int = 0)

    /** generates mipmaps from the top-level mipmap */
    fun generateMipmaps()

    /** the filter to use when displaying at sizes smaller than the original */
    var filterMin: MinifyingFilter

    /** the filter to use when displaying at sizes larger than the original */
    var filterMag: MagnifyingFilter

    var flipV: Boolean

}