package org.openrndr.draw.boundary

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.VertexBuffer
import org.openrndr.math.Matrix44
import org.openrndr.shape.FillRule
import org.openrndr.shape.Shape
import kotlin.jvm.JvmRecord

/**
 * A command to draw a boundary from a [BoundaryMap]
 * @property boundaryIndex the index of the boundary in the [BoundaryMap], as returned by [BoundaryMap.addShape]
 * @property transform the transform of the boundary
 * @property fill the fill color, null for no fill
 * @property stroke the stroke color, null for no stroke
 * @property strokeWeight the weight of the stroke, in the space of the boundary
 * @property strokeMode 0 for a stroke centered on the boundary, 1 for inside, 2 for outside, 3 to erode the fill
 */
@JvmRecord
data class BoundaryCommand(
    val boundaryIndex: Int,
    val transform: Matrix44,
    val fill: ColorRGBa?,
    val stroke: ColorRGBa?,
    val strokeWeight: Double,
    val strokeMode: Int = 0
)

/**
 * Prepared boundary commands, drawn with [BoundaryDrawer.draw]
 * @property instances the instance buffer holding the prepared commands
 * @property count the number of prepared commands
 */
interface BoundaryDrawCommand {
    val instances: VertexBuffer
    val count: Int
}

/**
 * GPU storage for the boundaries of shapes, drawn by a [BoundaryDrawer]
 */
interface BoundaryMap {
    /**
     * Adds the boundary of [shape] and returns its index
     * @param fillRule the fill rule of the boundary
     * @param quadraticTolerance the tolerance with which cubic segments are approximated with quadratic segments
     */
    fun addShape(shape: Shape, fillRule: FillRule = FillRule.NON_ZERO, quadraticTolerance: Double = 1.0): Int

    /**
     * Starts a batch, in which added boundaries are uploaded to the GPU once [endBatch] is called
     */
    fun startBatch()

    fun endBatch()
}

/**
 * Draws boundaries from a [BoundaryMap] of type [M]
 */
interface BoundaryDrawer<M : BoundaryMap> : AutoCloseable {
    /**
     * How much the coverage of edges in dark colors is increased, 0.0 for uncorrected coverage
     */
    var contrast: Double

    /**
     * Boundaries that are at most this many pixels in width and height are drawn from approximate ink profiles
     * instead of from their curves, 0.0 to always draw from the curves
     */
    var minificationThreshold: Double

    /**
     * The instance buffer that commands are prepared in by default
     */
    val instances: VertexBuffer

    fun createInstanceBuffer(size: Int): VertexBuffer

    /**
     * Prepares [commands] in [instances]
     */
    fun prepare(map: M, commands: List<BoundaryCommand>, instances: VertexBuffer = this.instances): BoundaryDrawCommand

    /**
     * Draws the prepared [command] with the draw state of [drawer]
     */
    fun draw(drawer: Drawer, map: M, command: BoundaryDrawCommand)

    /**
     * Draws the commands last prepared in [instances]
     */
    fun draw(drawer: Drawer, map: M)
}
