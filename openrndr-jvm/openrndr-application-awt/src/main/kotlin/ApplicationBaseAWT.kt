package org.openrndr.awt

import org.openrndr.Application
import org.openrndr.ApplicationBase
import org.openrndr.Configuration
import org.openrndr.Display
import org.openrndr.Program

/**
 * The AWT application back-end, selected with `-Dorg.openrndr.application=AWT` or as the only back-end on the class
 * path. An application shows its program in a frame, see [ProgramPanel] to embed programs in other user interfaces.
 */
class ApplicationBaseAWT : ApplicationBase() {
    init {
        AWTBackend.initialize()
    }

    override val displays: List<Display> = emptyList()

    override fun build(program: Program, configuration: Configuration): Application {
        return ApplicationAWT(program, configuration, standalone = true)
    }
}
