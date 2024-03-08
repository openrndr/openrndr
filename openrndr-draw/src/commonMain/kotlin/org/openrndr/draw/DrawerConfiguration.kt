package org.openrndr.draw

import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType

/**
 * Drawer configuration
 */
object DrawerConfiguration {

    /**
     * Should vertex buffer optimizations be disabled.
     *
     * This can be configured by passing `-Dorg.openrndr.draw.vertex_buffer_optimizations=disable` to the JVM
     * @since openrndr 0.4.5
     */
    val disableVertexBufferOptimizations by lazy { Platform.property("org.openrndr.draw.vertex_buffer_optimizations") == "disable" }


    /**
     * Should vertex buffer optimizations be forcefully enabled
     *
     * This can be configured by passing `-Dorg.openrndr.draw.vertex_buffer_optimizations=force` to the JVM
     * @since openrndr 0.4.5
     */
    val forceVertexBufferOptimizations by lazy { Platform.property("org.openrndr.draw.vertex_buffer_optimizations") == "force" }


    /**
     * A hint for the number of vertex buffers to create in order to reduce pipeline stall
     *
     * This can be configured by passing `-Dorg.openrndr.draw.vertex_buffer_multi_buffer_hint=<count>` to the JVM, the
     * default value is 100.
     * @since openrndr 0.4.5
     */
    val vertexBufferMultiBufferHint by lazy {
        Platform.property("org.openrndr.draw.vertex_buffer_multi_buffer_hint")?.toIntOrNull() ?: 100
    }

    /**
     * The effective number of vertex buffers to create in order to reduce pipeline stall
     *
     * @see disableVertexBufferOptimizations
     * @see forceVertexBufferOptimizations
     * @see vertexBufferMultiBufferHint
     * @since openrndr 0.4.5
     */
    val vertexBufferMultiBufferCount by lazy {
        if (disableVertexBufferOptimizations) {
            1
        } else {
            if (forceVertexBufferOptimizations || Platform.type == PlatformType.MAC) {
                vertexBufferMultiBufferHint
            } else {
                1
            }
        }
    }
}