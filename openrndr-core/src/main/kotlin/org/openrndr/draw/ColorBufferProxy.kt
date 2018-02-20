//package org.openrndr.draw
//
//
//class ColorBufferProxy {
//
//
//    constructor(url:String, loader:ColorBufferLoader) {
//
//    }
//
//    constructor(colorBuffer: ColorBuffer) {
//        this.colorBuffer = colorBuffer
//    }
//
//    enum class State {
//        NOT_LOADED,
//        QUEUED,
//        LOADED
//    }
//    internal var realState = State.NOT_LOADED
//
//    var persistent = false
//    var colorBuffer:ColorBuffer? = null
//
//    private var lastTouched = 0L
//
//    fun queue() {
//        touch()
//        if (loader != null) {
//            if (state == State.NOT_LOADED) {
//                loader.queue(this)
//                state = State.QUEUED
//            }
//        }
//    }
//
//    fun touch() {
//        lastTouched = System.currentTimeMillis()
//    }
//
//
//
//}