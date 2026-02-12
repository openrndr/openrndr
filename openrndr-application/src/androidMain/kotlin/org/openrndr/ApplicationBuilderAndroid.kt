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
    val builder = ApplicationBuilderAndroid(context)
    builder.apply {
        build()
        result = applicationBase.build(this.program, this.configuration)
        result.run()
    }

    val surfaceViewListener = result as GLSurfaceViewListener
    val renderer = ORSurfaceViewRenderer(surfaceViewListener)

    val surfaceView = GLSurfaceView(context).apply {
        setEGLContextClientVersion(3)
        setRenderer(renderer)
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        setOnTouchListener { view, event -> surfaceViewListener.onTouch(view, event) }
    }

    val lifecycle = (context as LifecycleOwner).lifecycle
    AndroidAppLifecycleHandler(lifecycle, object : AndroidAppLifecycleListener {
        override fun onResume() {
            builder.onResume()
            surfaceView.onResume()
        }

        override fun onPause() {
            builder.onPause()
            surfaceView.onPause()
        }
    })

    return surfaceView
}

@Suppress("DeprecatedCallableAddReplaceWith")
class ApplicationBuilderAndroid(context: Context) : ApplicationBuilder() {
    override val configuration = Configuration()
    override var program: Program = ProgramImplementation()
    override val applicationBase: ApplicationBase = ApplicationBase.initialize()
    override val displays by lazy { applicationBase.displays }

    override fun configure(init: Configuration.() -> Unit) {
        configuration.init()
    }

    private val sensorHandler = SensorHandler(context)

    override fun program(init: suspend Program.() -> Unit): Program {
        program = object : ProgramImplementation() {
            override suspend fun setup() {
                init()
            }

            override fun gyroscope(sensorRate: SensorRate): Gyroscope {
                return sensorHandler.provideGyroscope(sensorRate)
            }

            override fun accelerometer(sensorRate: SensorRate): Accelerometer {
                return sensorHandler.provideAccelerometer(sensorRate)
            }

            override fun compass(sensorRate: SensorRate): Compass {
                return sensorHandler.provideCompass(sensorRate)
            }

            override fun deviceRotation(sensorRate: SensorRate): DeviceRotation {
                return sensorHandler.provideDeviceRotation(sensorRate)
            }

            override fun proximity(sensorRate: SensorRate): Proximity {
                return sensorHandler.provideProximity(sensorRate)
            }

            override fun light(sensorRate: SensorRate): Light {
                return sensorHandler.provideLight(sensorRate)
            }
        }
        return program
    }

    fun onResume() {
        sensorHandler.onResume()
    }

    fun onPause() {
        sensorHandler.onPause()
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