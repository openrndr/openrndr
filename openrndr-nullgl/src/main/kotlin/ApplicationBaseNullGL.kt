package org.openrndr.internal.nullgl

import org.openrndr.*

class ApplicationBaseNullGL : ApplicationBase() {
    override val displays: List<Display> = emptyList()

    override fun build(program: Program, configuration: Configuration): Application {
        return ApplicationNullGL(program, configuration)
    }
}