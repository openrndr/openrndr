package org.openrndr

import android.opengl.GLES20.glDisable
import android.opengl.GLES20.glViewport
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ORSurfaceViewRenderer(
    private val listener: GLSurfaceViewListener
) : GLSurfaceView.Renderer {

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        glDisable(GLES30.GL_DEPTH_TEST)
        listener.onSurfaceCreated()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)
        listener.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        listener.onDrawFrame()
    }
}