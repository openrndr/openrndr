package org.openrndr

import org.openrndr.draw.Drawer

@DslMarker
annotation class ExtensionDslMarker

/**
 * Indicates the stage in which the extension is
 */
enum class ExtensionStage {
    SETUP,
    BEFORE_DRAW,
    AFTER_DRAW
}

/**
 * Defines a Program extension. This is the interface for developers of OPENRNDR extensions.
 */
@ExtensionDslMarker
interface Extension {
    var enabled: Boolean
    fun setup(program: Program) {}
    fun beforeDraw(drawer: Drawer, program: Program) {}
    fun afterDraw(drawer: Drawer, program: Program) {}

    /**
     * Shutdown is called when the host application is quit
     */
    fun shutdown(program: Program) {}
}
