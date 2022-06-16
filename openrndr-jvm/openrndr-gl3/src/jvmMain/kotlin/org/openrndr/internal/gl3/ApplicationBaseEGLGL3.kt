package org.openrndr.internal.gl3

import org.openrndr.*

class ApplicationBaseEGLGL3 : ApplicationBase() {
    override val displays: List<Display> = emptyList()

    override fun build(program: Program, configuration: Configuration): Application {
        return ApplicationEGLGL3(program, configuration)
    }
}