
import io.kotest.matchers.ints.shouldBeExactly
import java.util.*
import kotlin.Comparator
import org.openrndr.kartifex.graphs.PriorityQueue as KPriorityQueue
import kotlin.test.Test

class TestPriorityQueue {
    @Test
    fun testBasic() {
        val kpr = KPriorityQueue<Int>(Comparator { x,y -> x.compareTo(y) })
        kpr.add(10)
        kpr.add(0)
        val result = kpr.poll()
        result!!.shouldBeExactly(0)

        val pr = PriorityQueue<Int>(Comparator { x,y -> x.compareTo(y) })
        pr.add(10)
        pr.add(0)
        pr.poll()!!.shouldBeExactly(0)
    }
}