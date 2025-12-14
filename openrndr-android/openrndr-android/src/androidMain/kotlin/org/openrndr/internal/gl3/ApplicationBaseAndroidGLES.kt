package org.openrndr.internal.gl3

import org.openrndr.Application
import org.openrndr.ApplicationBase
import org.openrndr.Configuration
import org.openrndr.Display
import org.openrndr.Program

class ApplicationBaseAndroidGLES : ApplicationBase() {
    init {
//        ImageDriver.driver = ImageDriverStbImage()
//        FontDriver.driver = FontDriverStbTt()
    }

    override val displays: List<Display> = emptyList()

    override fun build(program: Program, configuration: Configuration): Application {
        return ApplicationAndroidGLES(program, configuration)
    }
}