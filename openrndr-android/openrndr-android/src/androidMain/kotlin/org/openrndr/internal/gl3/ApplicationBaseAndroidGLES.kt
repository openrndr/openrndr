package org.openrndr.internal.gl3

import org.openrndr.Application
import org.openrndr.ApplicationBase
import org.openrndr.Configuration
import org.openrndr.Display
import org.openrndr.Program
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.fontdriver.android.FontDriverAndroid
import org.openrndr.internal.AppContextHolder

class ApplicationBaseAndroidGLES : ApplicationBase() {

    private val context = AppContextHolder.context

    init {
//        ImageDriver.driver = ImageDriverStbImage()
        FontDriver.driver = FontDriverAndroid(context)
    }

    override val displays: List<Display>
        get() = listOf()

    override fun build(
        program: Program,
        configuration: Configuration
    ): Application {
        return ApplicationAndroidGLES(program, configuration)
    }
}