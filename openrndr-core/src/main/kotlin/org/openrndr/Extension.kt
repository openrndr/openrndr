package org.openrndr

import org.openrndr.draw.Drawer

/**
 * Defines a Program extension. This is the interface for developers of OPENRNDR extensions.
 */
interface Extension {
    fun setup(program:Program) {}
    fun beforeDraw(drawer: Drawer, program:Program) {}
    fun afterDraw(drawer:Drawer, program:Program) {}
}
