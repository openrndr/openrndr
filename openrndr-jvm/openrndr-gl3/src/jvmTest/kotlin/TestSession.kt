import org.openrndr.draw.Session
import org.openrndr.draw.colorBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class TestSession : AbstractApplicationTestFixture() {


    @Test
    fun testColorBufferSession() {
        assertEquals(Session.root, Session.active)
        val start = Session.active.colorBuffers.size
        val image = colorBuffer(256, 256)
        val end = Session.active.colorBuffers.size
        assertEquals(start + 1, end)
        image.destroy()
        assertEquals(start, Session.active.colorBuffers.size)

        val forked = Session.active.fork()
        assertEquals(forked, Session.active)
        assertEquals(0, forked.colorBuffers.size)
        colorBuffer(256, 256)
        assertEquals(1, forked.colorBuffers.size)
        forked.end()
    }

    @Test
    fun testPushPopSession() {
        val old = Session.active
        val forked = Session.active.fork()
        forked.pop()
        assertEquals(old, Session.active)
        forked.push()
        assertEquals(forked, Session.active)
        forked.pop()
        forked.end()
    }


}