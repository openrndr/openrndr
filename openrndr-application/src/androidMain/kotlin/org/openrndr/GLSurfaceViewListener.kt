package org.openrndr

import android.view.MotionEvent
import android.view.View

interface GLSurfaceViewListener {
    fun onSurfaceCreated()
    fun onSurfaceChanged(width: Int, height: Int)
    fun onDrawFrame()
    fun onTouch(view: View?, event: MotionEvent): Boolean
}