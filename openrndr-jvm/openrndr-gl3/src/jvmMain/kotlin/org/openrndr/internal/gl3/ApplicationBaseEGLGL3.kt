package org.openrndr.internal.gl3

import org.openrndr.*
import org.openrndr.internal.ImageDriver

class ApplicationBaseEGLGL3 : ApplicationBase() {
    init {
        ImageDriver.driver = ImageDriverStbImage()
    }

    override val displays: List<Display> = emptyList()

    override fun build(program: Program, configuration: Configuration): Application {
        return ApplicationEGLGL3(program, configuration)
    }
}