package org.openrndr.android.application

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import org.openrndr.internal.gl3.ApplicationAndroidGLES
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

internal class ORRenderer(
    private val app: ApplicationAndroidGLES
) : GLSurfaceView.Renderer {

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        app.onSurfaceCreated()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        app.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        app.onDrawFrame()
    }
}