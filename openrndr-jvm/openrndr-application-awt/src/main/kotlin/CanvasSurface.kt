package org.openrndr.awt

import java.awt.Canvas
import java.awt.Component
import java.awt.Container
import java.awt.Window
import javax.swing.JRootPane

/**
 * A canvas that OPENRNDR draws in
 */
internal interface CanvasSurface {
    /** the AWT component of the surface */
    val canvas: Canvas

    /** the width of the surface in pixels */
    val pixelWidth: Int

    /** the height of the surface in pixels */
    val pixelHeight: Int

    /**
     * Makes the context of the surface current, runs [frame] and presents the frame. Must be called from the render
     * thread.
     * @return false when the surface could not be drawn, because it is not showing or has no size
     */
    fun render(frame: () -> Unit): Boolean
}

/**
 * The bounds of [canvas] in the coordinates of the layer of its window: y up, relative to the root pane, or to the
 * content of the window when the canvas is not in a root pane.
 *
 * The component tree is read without the AWT tree lock, the render thread must not take it, see lwjgl3-awt's
 * PlatformMacOSXGLCanvas.getLayerBounds.
 */
internal fun layerBounds(canvas: Canvas): IntArray {
    var x = 0
    var y = 0
    var child: Component = canvas
    var parent: Container? = canvas.parent
    while (parent != null) {
        x += child.x
        y += child.y
        if (parent is JRootPane) {
            return intArrayOf(x, parent.height - y - canvas.height, canvas.width, canvas.height)
        }
        if (parent is Window) {
            val insets = parent.insets
            val contentHeight = parent.height - insets.top - insets.bottom
            return intArrayOf(x - insets.left, contentHeight - (y - insets.top) - canvas.height, canvas.width, canvas.height)
        }
        child = parent
        parent = parent.parent
    }
    return intArrayOf(0, 0, canvas.width, canvas.height)
}
