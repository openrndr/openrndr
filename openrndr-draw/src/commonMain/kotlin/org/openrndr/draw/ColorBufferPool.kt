package org.openrndr.draw

import org.openrndr.internal.Driver
import org.openrndr.internal.shareGroupID

/**
 * A pool of color buffers, for intermediates that are needed for a short time, such as the passes of a multi-pass
 * filter. [acquire] hands out a free buffer of the requested size and format or creates one, [release] gives it back
 * to be handed out again. A pool is not thread safe, it is used from the thread of its share group.
 *
 * The buffers of a pool are not tracked by a [Session], they are destroyed by [trim] or [destroy].
 *
 * ```
 * val pool = colorBufferPool()
 * pool.borrow(width, height, type = ColorType.FLOAT16) { intermediate ->
 *     horizontal.apply(source, intermediate)
 *     vertical.apply(intermediate, target)
 * }
 * pool.trim(60) // once per frame, destroys buffers that were not acquired in the last 60 frames
 * ```
 * @see colorBufferPool
 */
interface ColorBufferPool {
    /**
     * A color buffer with these properties, free in the pool or new. Its contents are undefined, it holds what it held
     * when it was released.
     */
    fun acquire(
        width: Int,
        height: Int,
        contentScale: Double = 1.0,
        format: ColorFormat = ColorFormat.RGBa,
        type: ColorType = defaultColorType(format),
        multisample: BufferMultisample = BufferMultisample.Disabled,
        levels: Int = 1
    ): ColorBuffer

    /**
     * Gives [buffer] back to the pool, the next [acquire] of its properties may hand it out again
     * @throws IllegalArgumentException when [buffer] was not acquired from this pool or was released already
     */
    fun release(buffer: ColorBuffer)

    /**
     * Ends a frame of the pool and destroys the free buffers that were not acquired in the last [frames] frames, all
     * free buffers when [frames] is 0. A buffer counts as used in the frames it was acquired or released in, buffers
     * that are acquired are kept. Called once per frame it keeps the buffers
     * that are in use from frame to frame.
     */
    fun trim(frames: Int = 0)

    /** the number of buffers in the pool, acquired and free */
    val size: Int

    /**
     * Destroys the buffers of the pool, also the acquired ones
     */
    fun destroy()
}

/**
 * Acquires a color buffer from this pool for the length of [block] and releases it afterwards
 * @see ColorBufferPool.acquire
 */
inline fun <T> ColorBufferPool.borrow(
    width: Int,
    height: Int,
    contentScale: Double = 1.0,
    format: ColorFormat = ColorFormat.RGBa,
    type: ColorType = defaultColorType(format),
    multisample: BufferMultisample = BufferMultisample.Disabled,
    levels: Int = 1,
    block: (ColorBuffer) -> T
): T {
    val buffer = acquire(width, height, contentScale, format, type, multisample, levels)
    try {
        return block(buffer)
    } finally {
        release(buffer)
    }
}

/**
 * A new [ColorBufferPool], that keeps its own buffers
 * @see colorBufferPool
 */
fun ColorBufferPool(): ColorBufferPool = ColorBufferPoolImpl()

/** the shared pools, one per [share group][Driver.shareGroupID], dropped when the driver changes */
private val sharedPools = mutableMapOf<Long, ColorBufferPool>()
private var sharedPoolsDriver: Driver? = null

/**
 * The color buffer pool of the current [share group][Driver.shareGroupID], shared by the users of this function, for
 * example the filters that need intermediates
 */
fun colorBufferPool(): ColorBufferPool {
    if (sharedPoolsDriver !== Driver.instance) {
        // the buffers of the pools of an earlier driver were released with its contexts
        sharedPools.clear()
        sharedPoolsDriver = Driver.instance
    }
    return sharedPools.getOrPut(Driver.instance.shareGroupID) { ColorBufferPool() }
}

private data class PoolKey(
    val width: Int,
    val height: Int,
    val contentScale: Double,
    val format: ColorFormat,
    val type: ColorType,
    val multisample: BufferMultisample,
    val levels: Int
)

private class PoolEntry(val key: PoolKey, val buffer: ColorBuffer) {
    var acquired = false
    var lastFrame = 0L
}

private class ColorBufferPoolImpl : ColorBufferPool {
    private val free = mutableMapOf<PoolKey, ArrayDeque<PoolEntry>>()
    private val entries = mutableMapOf<ColorBuffer, PoolEntry>()
    private var frame = 0L

    override val size: Int get() = entries.size

    override fun acquire(
        width: Int,
        height: Int,
        contentScale: Double,
        format: ColorFormat,
        type: ColorType,
        multisample: BufferMultisample,
        levels: Int
    ): ColorBuffer {
        val key = PoolKey(width, height, contentScale, format, type, multisample, levels)
        val entry = free[key]?.removeLastOrNull() ?: run {
            val buffer = colorBuffer(width, height, contentScale, format, type, multisample, levels, session = null)
            PoolEntry(key, buffer).also { entries[buffer] = it }
        }
        entry.acquired = true
        entry.lastFrame = frame
        return entry.buffer
    }

    override fun release(buffer: ColorBuffer) {
        val entry = entries[buffer]
        require(entry != null) { "$buffer was not acquired from this pool" }
        require(entry.acquired) { "$buffer was released already" }
        entry.acquired = false
        entry.lastFrame = frame
        free.getOrPut(entry.key) { ArrayDeque() }.addLast(entry)
    }

    override fun trim(frames: Int) {
        require(frames >= 0) { "frames must be 0 or more, it is $frames" }
        frame++
        val iterator = free.values.iterator()
        while (iterator.hasNext()) {
            val queue = iterator.next()
            queue.removeAll { entry ->
                val unused = frame - entry.lastFrame > frames
                if (unused) {
                    entries.remove(entry.buffer)
                    entry.buffer.destroy()
                }
                unused
            }
            if (queue.isEmpty()) iterator.remove()
        }
    }

    override fun destroy() {
        for (entry in entries.values) {
            entry.buffer.destroy()
        }
        entries.clear()
        free.clear()
    }
}
