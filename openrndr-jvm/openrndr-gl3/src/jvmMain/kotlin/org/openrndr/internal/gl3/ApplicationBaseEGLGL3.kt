package org.openrndr.internal.gl3

import org.openrndr.*
import org.openrndr.draw.font.FontDriverStbTt
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.internal.ImageDriver

class ApplicationBaseEGLGL3 : ApplicationBase() {
    init {
        ImageDriver.driver = ImageDriverStbImage()
        FontDriver.driver = FontDriverStbTt()
    }

    override val displays: List<Display> = emptyList()

    override fun build(program: Program, configuration: Configuration): Application {
        return ApplicationEGLGL3(program, configuration)
    }
}