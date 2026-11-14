import org.openrndr.utils.collections.LRUCache
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TestLRUCache {
    @Test
    fun `setting a present key replaces its value and does not grow the cache`() {
        val cache = LRUCache<String, Int>(4)
        repeat(100) { cache.getOrSet("a", forceSet = true) { it } }
        assertEquals(1, cache.size)
        assertEquals(99, cache.get("a"))
    }

    @Test
    fun `the least recently used values are evicted`() {
        val cache = LRUCache<Int, Int>(8)
        for (i in 0 until 8) cache.set(i, i)
        // 0 is read, it is used more recently than the others
        cache.get(0)
        cache.set(8, 8)
        assertEquals(0, cache.get(0))
        assertEquals(8, cache.get(8))
        // the oldest quarter is evicted: 1 and 2
        assertNull(cache.get(1))
        assertNull(cache.get(2))
        assertEquals(3, cache.get(3))
        assertEquals(7, cache.size)
    }

    @Test
    fun `values are made once while they are cached`() {
        val cache = LRUCache<Int, String>(2)
        var made = 0
        repeat(10) { cache.getOrSet(1) { made++; "one" } }
        assertEquals(1, made)
    }
}
