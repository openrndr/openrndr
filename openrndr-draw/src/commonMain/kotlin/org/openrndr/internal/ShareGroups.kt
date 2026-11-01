package org.openrndr.internal

/**
 * Implemented by drivers whose contexts do not all share resources, such as the WebGL driver, which has a context per
 * canvas
 */
interface ShareGroups {
    /**
     * The share group of the current context
     */
    val shareGroupID: Long
}

/**
 * Identifies the group of contexts that share resources: resources created in a context can be used in every context
 * of its share group. The contexts of a JVM application share their resources, every WebGL context, one per canvas,
 * is its own share group. Caches of resources are kept per share group.
 */
val Driver.shareGroupID: Long
    get() = (this as? ShareGroups)?.shareGroupID ?: 0L
