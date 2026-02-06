package org.openrndr.internal.gl3

import org.openrndr.Application
import org.openrndr.ApplicationBase
import org.openrndr.Configuration
import org.openrndr.Display
import org.openrndr.Program

class ApplicationBaseAndroidGLES : ApplicationBase() {

    override val displays: List<Display>
        get() = listOf()

    override fun build(
        program: Program,
        configuration: Configuration
    ): Application {
        return ApplicationAndroidGLES(program, configuration)
    }
}