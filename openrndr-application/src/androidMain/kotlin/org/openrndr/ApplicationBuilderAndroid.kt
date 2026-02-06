package org.openrndr

import android.content.Context
import android.opengl.GLSurfaceView
import androidx.lifecycle.LifecycleOwner

fun androidApplication(
    context: Context,
    build: ApplicationBuilderAndroid.() -> Unit
): GLSurfaceView {
    System.setProperty("org.openrndr.application", "ANDROID-GLES")

    val result: Application
    ApplicationBuilderAndroid().apply {
        build()
        result = applicationBase.build(this.program, this.configuration)
        result.run()
    }

    val listener = result as GLSurfaceViewListener
    val renderer = ORSurfaceViewRenderer(listener)

    val surfaceView = GLSurfaceView(context).apply {
        setEGLContextClientVersion(3)
        setRenderer(renderer)
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    val lifecycle = (context as LifecycleOwner).lifecycle
    AndroidAppLifecycleHandler(lifecycle, object : AndroidAppLifecycleListener {
        override fun onPause() {
            surfaceView.onPause()
        }

        override fun onResume() {
            surfaceView.onResume()
        }
    })

    return surfaceView
}

@Suppress("DeprecatedCallableAddReplaceWith")
class ApplicationBuilderAndroid : ApplicationBuilder() {
    override val configuration = Configuration()
    override var program: Program = ProgramImplementation()
    override val applicationBase: ApplicationBase = ApplicationBase.initialize()
    override val displays by lazy { applicationBase.displays }

    override fun configure(init: Configuration.() -> Unit) {
        configuration.init()
    }

    override fun program(init: suspend Program.() -> Unit): Program {
        program = object : ProgramImplementation() {
            override suspend fun setup() {
                init()
            }
        }
        return program
    }

    fun run(): Application {
        val result = applicationBase.build(this.program, this.configuration)
        result.run()
        return result
    }

    @Deprecated(
        "Cannot construct application in an application block.",
        level = DeprecationLevel.ERROR
    )
    override fun application(build: ApplicationBuilder.() -> Unit): Nothing =
        error("Cannot construct application in an application block.")

    @Deprecated("Cannot construct program in a program block.", level = DeprecationLevel.ERROR)
    override fun Program.program(init: Program.() -> Unit): Nothing =
        error("Cannot construct program in a program block.")
}