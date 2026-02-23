import org.openrndr.draw.ColorBufferProxy
import org.openrndr.internal.colorBufferLoader
import kotlin.test.Test
import kotlin.test.assertEquals

class TestColorBufferProxyGL3 : AbstractApplicationTestFixture() {

    /**
     * Tests the behavior of the `ColorBufferProxy` when loading a color buffer from a URL.
     *
     * This test verifies that a `ColorBufferProxy` transitions from the `QUEUED` state to the `LOADED` state
     * after being processed. It continuously checks the proxy's state and ensures it reaches the expected
     * `LOADED` state, pausing execution between checks to allow for the state transition.
     *
     * Assertions:
     * - The final state of the `ColorBufferProxy` must be `LOADED`.
     */
    @Test
    fun testColorBufferProxy() {
        val proxy = colorBufferLoader.loadFromUrl("https://avatars3.githubusercontent.com/u/31103334?s=200&v=4")
        while (true) {
            if (proxy.state != ColorBufferProxy.State.QUEUED) {
                assertEquals(ColorBufferProxy.State.LOADED, proxy.state)
                break
            }
            Thread.sleep(1000L)
        }
    }
}