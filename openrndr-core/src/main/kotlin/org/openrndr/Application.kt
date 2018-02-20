package org.openrndr

import org.openrndr.math.Vector2
import kotlin.concurrent.thread

abstract class Application {
    companion object {

        fun run(program: Program, configuration: Configuration) {
            val c = Application::class.java.classLoader.loadClass("org.openrndr.internal.gl3.ApplicationGL3")
            val application = c.declaredConstructors[0].newInstance(program, configuration) as Application
            application.setup()
            application.loop()
        }

        fun runAsync(program: Program, configuration: Configuration) {
            val c = Application::class.java.classLoader.loadClass("org.openrndr.internal.gl3.ApplicationGL3")
            val application = c.declaredConstructors[0].newInstance(program, configuration) as Application
            thread {
                application.setup()
                application.loop()
            }
        }

    }

    abstract fun exit()
    abstract fun setup()

    abstract fun loop()
    abstract var clipboardContents:String?
    abstract var windowTitle:String

    abstract var windowPosition: Vector2

    abstract val seconds:Double
}

fun application(program:Program, configuration:Configuration = Configuration()) {
    Application.run(program, configuration)
}