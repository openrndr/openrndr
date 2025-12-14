package org.openrndr.android.application

import android.content.Context
import android.opengl.GLSurfaceView
import org.openrndr.AndroidAppRegistry
import org.openrndr.Configuration
import org.openrndr.ProgramImplementation
import org.openrndr.internal.gl3.ApplicationAndroidGLES

fun createSurfaceView(context: Context): GLSurfaceView {

    // 1) Take the builder prepared by application { … }
    val (config: Configuration, program: ProgramImplementation) =
        AndroidAppRegistry.consume().build()

    // 2) Build the OPENRNDR Android runner
    val app = ApplicationAndroidGLES(program, config)

    // 3) GLSurfaceView + Renderer
    return GLSurfaceView(context).apply {
        setEGLContextClientVersion(3)
        setRenderer(ORRenderer(app))
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }
}