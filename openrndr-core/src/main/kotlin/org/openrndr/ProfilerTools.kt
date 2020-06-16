package org.openrndr


val measuredTime = mutableMapOf<String, Long>()
val measuredHits = mutableMapOf<String, Long>()

inline fun <T> measure(name:String, crossinline f:()->T) : T {
    val start = System.nanoTime()
    val result = f()
    val end = System.nanoTime()

    measuredTime[name] = measuredTime.getOrDefault(name, 0L) + (end-start)
    measuredHits[name] = measuredHits.getOrDefault(name, 0L) + 1
    return result
}


fun report() {
    for (name in measuredTime.keys) {
        val average = (measuredTime[name]!!.toDouble() / measuredHits[name]!!.toDouble())/1000000.0

        println("$name\t\t\t${measuredTime[name]}\t${measuredHits[name]}\t${average}ms")
    }
}