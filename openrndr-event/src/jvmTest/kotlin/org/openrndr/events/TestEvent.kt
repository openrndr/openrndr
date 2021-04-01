//package org.openrndr.events
//
//import org.amshove.kluent.`should be equal to`
//import org.spekframework.spek2.Spek
//import org.spekframework.spek2.style.specification.describe
//
//
//object TestSegment : Spek({
//
//    describe("an event") {
//        val event = Event<Int>()
//
//        val listener = { _: Int ->
//
//        }
//
//        fun listener2(e: Int) {
//            e
//        }
//
//        val ref = event.listen(listener)
//        event.listeners.size `should be equal to` 1
//
//        event.cancel(ref)
//        event.listeners.size `should be equal to` 0
//
//
//        event.listen {
//
//        }
//        event.listen(::listener2)
//
//    }
//
//})
