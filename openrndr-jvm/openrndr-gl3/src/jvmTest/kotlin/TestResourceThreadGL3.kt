import org.openrndr.internal.Driver
import kotlin.test.Test

class TestResourceThreadGL3 : AbstractApplicationTestFixture() {

    @Test
    fun testResourceThread() {
        val rt = Driver.instance.createResourceThread {  }
        Thread.sleep(1000L)
    }
}
