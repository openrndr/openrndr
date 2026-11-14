import org.openrndr.draw.ColorBufferPool
import org.openrndr.draw.ColorType
import org.openrndr.draw.borrow
import org.openrndr.draw.colorBufferPool
import kotlin.test.*

/**
 * Color buffer pools hand out free buffers of the requested properties and destroy unused ones
 */
class TestColorBufferPoolGL3 : AbstractApplicationTestFixture() {
    @Test
    fun `released buffers are handed out again for the same properties`() {
        val pool = ColorBufferPool()
        val first = pool.acquire(16, 16)
        pool.release(first)
        assertSame(first, pool.acquire(16, 16))
        val other = pool.acquire(16, 16)
        assertNotSame(first, other)
        val float = pool.acquire(16, 16, type = ColorType.FLOAT16)
        assertEquals(ColorType.FLOAT16, float.type)
        val mipmapped = pool.acquire(16, 16, levels = 5)
        assertEquals(5, mipmapped.levels)
        assertEquals(4, pool.size)
        pool.destroy()
        assertEquals(0, pool.size)
    }

    @Test
    fun `buffers are released once and only to their pool`() {
        val pool = ColorBufferPool()
        val buffer = pool.acquire(8, 8)
        pool.release(buffer)
        assertFailsWith<IllegalArgumentException> { pool.release(buffer) }
        val foreign = org.openrndr.draw.colorBuffer(8, 8)
        assertFailsWith<IllegalArgumentException> { pool.release(foreign) }
        foreign.destroy()
        pool.destroy()
    }

    @Test
    fun `trim destroys free buffers that were not used recently and keeps acquired ones`() {
        val pool = ColorBufferPool()
        val kept = pool.acquire(8, 8)
        val old = pool.acquire(8, 8)
        pool.release(old)
        pool.trim(2)
        pool.trim(2)
        assertEquals(2, pool.size, "used two frames ago")
        pool.trim(2)
        assertEquals(1, pool.size, "not used for three frames")
        pool.release(kept)
        pool.trim(2)
        assertEquals(1, pool.size, "released in the previous frame")
        pool.trim(0)
        assertEquals(0, pool.size)
    }

    @Test
    fun `borrowed buffers are released also when the block throws`() {
        val pool = ColorBufferPool()
        val borrowed = pool.borrow(8, 8) { it }
        assertSame(borrowed, pool.acquire(8, 8))
        pool.release(borrowed)
        assertFailsWith<IllegalStateException> { pool.borrow(8, 8) { error("failed") } }
        assertSame(borrowed, pool.acquire(8, 8))
        pool.destroy()
    }

    @Test
    fun `the shared pool is the same for the share group`() {
        assertSame(colorBufferPool(), colorBufferPool())
    }
}
