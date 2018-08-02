package org.openrndr

import org.openrndr.math.Vector2
import kotlin.concurrent.thread

enum class PresentationMode {
    AUTOMATIC,
    MANUAL,
}

abstract class Application {
    companion object {

        fun run(program: Program, configuration: Configuration) {

            val c = if (!configuration.headless)
                Application::class.java.classLoader.loadClass("org.openrndr.internal.gl3.ApplicationGLFWGL3")
            else
                Application::class.java.classLoader.loadClass("org.openrndr.internal.gl3.ApplicationEGLGL3")

            val application = c.declaredConstructors[0].newInstance(program, configuration) as Application
            println(application)
            application.setup()
            application.loop()
        }

        fun runAsync(program: Program, configuration: Configuration) {
            val c = Application::class.java.classLoader.loadClass("org.openrndr.internal.gl3.ApplicationGLFWGL3")
            val application = c.declaredConstructors[0].newInstance(program, configuration) as Application
            thread {
                application.setup()
                application.loop()
            }
        }
    }

    abstract fun requestDraw()

    abstract fun exit()
    abstract fun setup()

    abstract fun loop()
    abstract var clipboardContents: String?
    abstract var windowTitle: String

    abstract var windowPosition: Vector2

    abstract val seconds: Double

    abstract var presentationMode: PresentationMode
}

fun application(program: Program, configuration: Configuration = Configuration()) {
    Application.run(program, configuration)
}

fun resourceUrl(name: String, `class`: Class<*> = Application::class.java): String {
    val resource = `class`.getResource(name)

    if (resource == null) {
        throw RuntimeException("resource $name not found")
    } else {
        return `class`.getResource(name).toExternalForm()
    }
}
