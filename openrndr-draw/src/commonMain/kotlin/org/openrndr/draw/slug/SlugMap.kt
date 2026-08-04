package org.openrndr.draw.slug

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.font.Face
import org.openrndr.math.IntVector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Segment2D
import org.openrndr.shape.Shape
import org.openrndr.shape.toQuadratics
import org.openrndr.utils.buffer.MPPBuffer
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.time.Clock


class SlugMap(val curves: ColorBuffer, val bands: ColorBuffer) {
    private var totalSegments = 0
    private var shapes = 0

    val bandCounts = mutableListOf<Int>()
    val bounds = mutableListOf<Rectangle>()
    val bandIndices = mutableListOf<IntVector2>()

    private var batching = false

    /**
     * Begins a new batching operation for the SlugMap.
     * 
     * Batching mode accumulates curve and band data in memory buffers instead of immediately
     * writing them to GPU textures after each shape is added. This significantly reduces the
     * number of texture write operations, which are expensive GPU operations, by deferring
     * all writes until [endBatch] is called.
     * 
     * This is particularly beneficial and advised when generating slugmaps dynamically at runtime
     * (online generation), such as when processing multiple glyphs or shapes in sequence. Instead
     * of performing one texture write per shape, batching allows all accumulated data to be written
     * in fewer, larger operations when the batch is complete.
     * 
     * Usage pattern:
     * ```
     * slugMap.startBatch()
     * // Add multiple shapes
     * slugMap.addShape(shape1)
     * slugMap.addShape(shape2)
     * slugMap.addShape(shape3)
     * slugMap.endBatch()  // All data written to GPU textures at once
     * ```
     * 
     * This method ensures that the batching process is not already active by 
     * requiring the `batching` property to be `false`. If the condition is met, 
     * it sets the `batching` property to `true`, marking the start of a new batch.
     *
     * @throws IllegalArgumentException if batching is already active (i.e., `batching` is `true`).
     * @see endBatch
     */
    fun startBatch() {
        require(!batching)
        batching = true
    }

    fun endBatch() {
        require(batching)
        batching = false
        flushCurves()
        flushBands()
    }

    private fun writeCoordinates(buffer: MPPBuffer, point: Int) {
        if (curveRowBuffer.position() == curves.width * 8) {
            flushCurves()
        }
        val x = (curveIndex + curveRowBuffer.position() / 8).mod(curves.width)
        val y = (curveIndex + curveRowBuffer.position() / 8) / curves.width
        require(y < curves.height)

        buffer.rewind()
        curveRowBuffer.put(buffer)
    }


    private var bandIndex = 0
    private var bandRowBuffer = MPPBuffer.allocate(bands.width * 8)

    private var curveIndex = 0
    private var curveRowBuffer = MPPBuffer.allocate(curves.width * 8)

    private fun flushCurves() {
        var x = (curveIndex).mod(curves.width)
        var y = (curveIndex) / curves.width
        var bufferedCount = (curveRowBuffer.position() / 8)
        if (bufferedCount > 0) {
            curveRowBuffer.flip()
            while (bufferedCount > 0) {
                val remainingInRow = curves.width - x
                val width = bufferedCount.coerceAtMost(remainingInRow)
                curves.write(curveRowBuffer, x = x, y = y, width = width, height = 1)
                curveIndex += width
                bufferedCount -= width

                x = (curveIndex).mod(curves.width)
                y = (curveIndex) / curves.width
                curveRowBuffer.position(curveRowBuffer.position() + width * 8)
            }
            curveRowBuffer.rewind()
            curveRowBuffer.limit(curves.width * 8)
        }
    }

    private fun flushBands() {
        var x = (bandIndex).mod(bands.width)
        var y = (bandIndex) / bands.width
        var bufferedCount = (bandRowBuffer.position() / 8)
        if (bufferedCount > 0) {
            bandRowBuffer.flip()
            while (bufferedCount > 0) {
                val remainingInRow = bands.width - x
                val width = bufferedCount.coerceAtMost(remainingInRow)
                bands.write(bandRowBuffer, x = x, y = y, width = width, height = 1)
                bandIndex += width
                bufferedCount -= width

                x = (bandIndex).mod(bands.width)
                y = (bandIndex) / bands.width
                bandRowBuffer.position(bandRowBuffer.position() + width * 8)
            }
            bandRowBuffer.rewind()
            bandRowBuffer.limit(bands.width * 8)
        }
    }

    private fun writeBand(buffer: MPPBuffer): IntVector2 {
        if (bandRowBuffer.position() == bands.width * 8) {
            flushBands()
        }

        val x = (bandIndex + bandRowBuffer.position() / 8).mod(bands.width)
        val y = (bandIndex + bandRowBuffer.position() / 8) / bands.width
        require(y < bands.height)

        buffer.rewind()
        bandRowBuffer.put(buffer)

        return IntVector2(x, y)
    }

    private fun writeBandHeader(curveCount: Int, offset: Int, band: Int): IntVector2 {
        bandBuffer.rewind()
        bandBuffer.put(curveCount.toShort())
        bandBuffer.put(offset.toShort())
        bandBuffer.put(0.toShort())
        bandBuffer.put(0.toShort())
        bandBuffer.rewind()
        return writeBand(bandBuffer)
    }

    private fun writeBandCurveIndex(ascending: IntVector2, descending: IntVector2): IntVector2 {
        bandBuffer.rewind()
        bandBuffer.put(ascending.x.toShort())
        bandBuffer.put(ascending.y.toShort())
        bandBuffer.put(descending.x.toShort())
        bandBuffer.put(descending.y.toShort())
        bandBuffer.rewind()
        return writeBand(bandBuffer)
    }

    val buffer = MPPBuffer.allocate(2 * 4)
    val bandBuffer = MPPBuffer.allocate(2 * 4)

    private fun writeCurve(segment: Segment2D): IntVector2 {

        val x = (totalSegments * 3).mod(curves.width)
        val y = (totalSegments * 3) / curves.width
        buffer.rewind()
        buffer.putFloat(segment.start.x.toFloat())
        buffer.putFloat(segment.start.y.toFloat())
        buffer.rewind()
        writeCoordinates(buffer, 0)

        buffer.rewind()
        buffer.putFloat(segment.control[0].x.toFloat())
        buffer.putFloat(segment.control[0].y.toFloat())
        buffer.rewind()
        writeCoordinates(buffer, 1)

        buffer.rewind()
        buffer.putFloat(segment.end.x.toFloat())
        buffer.putFloat(segment.end.y.toFloat())
        buffer.rewind()
        writeCoordinates(buffer, 2)
        totalSegments++

        return IntVector2(x, y)

    }

    private fun bandIndex(): IntVector2 {
        val totalIndex = bandIndex + bandRowBuffer.position() / 8
        return IntVector2(totalIndex.mod(bands.width), totalIndex / bands.width)
    }

    fun addShape(shape: Shape, quadraticTolerance: Double = 1.0): Int {
        val segments = shape.contours.flatMap { it.segments.flatMap { it.toQuadratics(quadraticTolerance) } }

        val bandCount = ceil(sqrt(segments.size.toDouble())).toInt().coerceIn(4..16)

        val bounds = shape.bounds
        this.bounds.add(bounds)
        bandCounts.add(bandCount)
        bandIndices.add(bandIndex())

        val segmentIndices = segments.map { writeCurve(it) }

        var curveOffset = 0
        val hbands = (0 until bandCount).map { y ->
            val band = Rectangle(
                bounds.x - 1.0,
                bounds.y + (bounds.height * y) / bandCount - 1.0,
                bounds.width + 2.0,
                bounds.height / bandCount + 2.0
            )
            val bandCurves = (segments.indices).filter { segments[it].bounds.intersects(band) }

            val asc = bandCurves.sortedBy { segments[it].bounds.x }
            val desc = bandCurves.sortedByDescending { segments[it].bounds.x + segments[it].bounds.width }
            writeBandHeader(bandCurves.size, curveOffset, y)
            curveOffset += bandCurves.size
            asc.map { segmentIndices[it] } to desc.map { segmentIndices[it] }
        }

        val vbands = (0 until bandCount).map { x ->
            val band = Rectangle(
                bounds.x + (bounds.width * x) / bandCount - 1.0,
                bounds.y - 1.0,
                bounds.width / bandCount + 2.0,
                bounds.height + 2.0
            )
            val bandSegments = (segments.indices).filter { segments[it].bounds.intersects(band) }

            val asc = bandSegments.sortedBy { segments[it].bounds.y }
            val desc = bandSegments.sortedByDescending { segments[it].bounds.x + segments[it].bounds.height }
            writeBandHeader(bandSegments.size, curveOffset, x)
            curveOffset += bandSegments.size
            asc.map { segmentIndices[it] } to desc.map { segmentIndices[it] }
        }

        for ((bindex, band) in hbands.withIndex()) {
            for ((cindex, i) in band.first.indices.withIndex()) {
                writeBandCurveIndex(band.first[i], band.second[i])
            }
        }

        for ((bindex, band) in vbands.withIndex()) {
            for ((cindex, i) in band.first.indices.withIndex()) {
                writeBandCurveIndex(band.first[i], band.second[i])
            }
        }

        if (!batching) {
            flushCurves()
            flushBands()
        }
        shapes++
        return shapes - 1
    }
}
