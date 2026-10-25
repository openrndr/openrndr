package org.openrndr.internal.gl3

import org.openrndr.*
import org.openrndr.draw.font.internal.CachingFontDriver
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.fontdriver.stb.FontDriverStbTt
import org.openrndr.internal.ImageDriver

class ApplicationBaseEGLGL3 : ApplicationBase() {
    init {
        ImageDriver.driver = ImageDriverStbImage()
        FontDriver.driver = CachingFontDriver(FontDriverStbTt())
    }

    override val displays: List<Display> = emptyList()

    override fun build(program: Program, configuration: Configuration): Application {
        return ApplicationEGLGL3(program, configuration)
    }
}