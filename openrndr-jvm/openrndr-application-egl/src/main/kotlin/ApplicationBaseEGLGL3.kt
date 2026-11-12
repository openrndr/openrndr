package org.openrndr.internal.gl3

import org.openrndr.*
import org.openrndr.biditextdriver.BidiTextDriverUax9
import org.openrndr.draw.font.internal.BidiTextDriver
import org.openrndr.draw.font.internal.CachingFontDriver
import org.openrndr.draw.font.internal.CachingTextShapingDriver
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.fontdriver.freetype.FontDriverFreetype
import org.openrndr.internal.ImageDriver
import org.openrndr.textshapingdriver.harfbuzz.TextShapingDriverHarfBuzz

class ApplicationBaseEGLGL3 : ApplicationBase() {
    init {
        // the same drivers as the GLFW and SDL backends
        ImageDriver.driver = ImageDriverStbImage()
        FontDriver.driver = CachingFontDriver(FontDriverFreetype())
        TextShapingDriver.driver = CachingTextShapingDriver(TextShapingDriverHarfBuzz())
        BidiTextDriver.driver = BidiTextDriverUax9()
    }

    override val displays: List<Display> = emptyList()

    override fun build(program: Program, configuration: Configuration): Application {
        return ApplicationEGLGL3(program, configuration)
    }
}
