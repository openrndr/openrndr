//package org.openrndr
//
//import org.openrndr.math.Vector2
//import kotlin.jvm.JvmRecord
//
//@JvmRecord
//data class Pointer(val position: Vector2, val primary: Boolean, val timestamp: Long)
//
//class Pointers(private val application: ()->Application) {
//    val pointers: List<Pointer>
//        get() = application().pointers
//}