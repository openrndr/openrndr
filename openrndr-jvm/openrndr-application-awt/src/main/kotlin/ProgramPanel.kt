package org.openrndr.awt

import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.ProgramImplementation
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * A Swing panel that shows an OPENRNDR program, to embed it in a Swing or AWT user interface, or in Compose with
 * `SwingPanel`.
 *
 * The program is set up when the panel is first shown, it is drawn and receives its events on the render thread of the
 * AWT back-end, not on AWT's event thread. The program runs until [dispose] is called or the program calls
 * `application.exit()`.
 *
 * On macOS with OpenGL ES the program is drawn through ANGLE in a Core Animation layer, elsewhere with OpenGL through
 * lwjgl3-awt.
 *
 * @param program the program to show
 * @param configuration the configuration of the program, its width and height are the preferred size of the panel
 */
class ProgramPanel(val program: Program, configuration: Configuration = Configuration()) : JPanel(BorderLayout()) {
    private val application = ApplicationAWT(program, configuration, standalone = false)

    init {
        add(application.surface.canvas, BorderLayout.CENTER)
    }

    override fun addNotify() {
        super.addNotify()
        application.start()
    }

    /**
     * Ends the program, its extensions are shut down on the render thread
     */
    fun dispose() {
        application.exit()
    }
}

/**
 * Creates a [ProgramPanel] for a program with the setup in [init], like `program { }` in an `application { }`
 */
fun programPanel(configuration: Configuration = Configuration(), init: suspend Program.() -> Unit): ProgramPanel {
    val program = object : ProgramImplementation() {
        override suspend fun setup() {
            init()
        }
    }
    return ProgramPanel(program, configuration)
}
