package org.openrndr.extensions

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Drawer

class FunctionDrawer(private val drawFunction: () -> Unit) : Extension {
    override var enabled: Boolean = true
    override fun beforeDraw(drawer: Drawer, program: Program) {
        drawFunction()
    }
}

fun draw(drawFunction: () -> Unit): FunctionDrawer {
    return FunctionDrawer(drawFunction)
}
