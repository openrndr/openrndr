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


class SlugMap2(val curves: ColorBuffer, val bands: ColorBuffer) {
    private var totalSegments = 0
    private var shapes = 0

    val bandCounts = mutableListOf<Int>()
    val bounds = mutableListOf<Rectangle>()
    val bandIndices = mutableListOf<IntVector2>()

    fun writeCoordinates(buffer: MPPBuffer, point: Int) {
        val x = (totalSegments * 3 + point).mod(curves.width)
        val y = (totalSegments * 3 + point) / curves.width
        require(y < curves.height)
        curves.write(buffer, x = x, y = y, width = 1, height = 1)
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    private var bandIndex = 0
    fun writeBand(buffer: MPPBuffer): IntVector2 {
        val x = (bandIndex).mod(bands.width)
        val y = (bandIndex) / bands.width
        require(y < bands.height)
        bands.write(buffer, x = x, y = y, width = 1, height = 1)
        bandIndex++
        return IntVector2(x, y)
    }

    fun writeBandHeader(curveCount: Int, offset :Int, band: Int): IntVector2 {
        bandBuffer.put(curveCount.toShort())
        bandBuffer.put(offset.toShort())
        bandBuffer.put(band.toShort())
        bandBuffer.put(0.toShort())
        bandBuffer.rewind()
        return writeBand(bandBuffer)
    }

    fun writeBandCurveIndex(ascending: IntVector2, descending: IntVector2): IntVector2 {
        bandBuffer.put(ascending.x.toShort())
        bandBuffer.put(ascending.y.toShort())
        bandBuffer.put(descending.x.toShort())
        bandBuffer.put(descending.y.toShort())
        bandBuffer.rewind()
        return writeBand(bandBuffer)
    }

    val buffer = MPPBuffer.allocate(2 * 4)
    val bandBuffer = MPPBuffer.allocate(2 * 4)

    fun writeCurve(segment: Segment2D): IntVector2 {

        val x = (totalSegments * 3).mod(curves.width)
        val y = (totalSegments * 3) / curves.width
        buffer.putFloat(segment.start.x.toFloat())
        buffer.putFloat(segment.start.y.toFloat())
        buffer.rewind()
        writeCoordinates(buffer, 0)

        buffer.putFloat(segment.control[0].x.toFloat())
        buffer.putFloat(segment.control[0].y.toFloat())
        buffer.rewind()
        writeCoordinates(buffer, 1)

        buffer.putFloat(segment.end.x.toFloat())
        buffer.putFloat(segment.end.y.toFloat())
        buffer.rewind()
        writeCoordinates(buffer, 2)
        totalSegments++

        return IntVector2(x, y)

    }

    fun bandIndex(): IntVector2 {
        return IntVector2(bandIndex.mod(bands.width), bandIndex / bands.width)
    }

    fun addShape(shape: Shape, quadraticTolerance: Double = 1.0): Int {
        var segments = shape.contours.flatMap { it.segments.flatMap { it.toQuadratics(quadraticTolerance) } }

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
                bounds.y + (bounds.height * y) / bandCount,
                bounds.width + 2.0,
                bounds.height / bandCount
            ).offsetEdges(0.0, 1.0)
            val bandCurves = (segments.indices).filter { segments[it].bounds.intersects(band) }

            val asc = bandCurves.sortedBy { segments[it].bounds.x }
            val desc = bandCurves.sortedByDescending { segments[it].bounds.x + segments[it].bounds.width }
            writeBandHeader(bandCurves.size, curveOffset, y)
            println("hband: ${y}, curves: ${bandCurves.size}, offset: ${curveOffset}")
            curveOffset += bandCurves.size
            asc.map { segmentIndices[it] } to desc.map { segmentIndices[it] }
        }

        val vbands = (0 until bandCount).map { x ->
            val band = Rectangle(
                bounds.x + (bounds.width * x) / bandCount,
                bounds.y - 1.0,
                bounds.width / bandCount,
                bounds.height + 2.0
            ).offsetEdges(1.0, 0.0)
            val bandSegments = (segments.indices).filter { segments[it].bounds.intersects(band) }

            val asc = bandSegments.sortedBy { segments[it].bounds.y }
            val desc = bandSegments.sortedByDescending { segments[it].bounds.x + segments[it].bounds.height }
            println("vband: ${x}, curves: ${bandSegments.size}, offset: ${curveOffset}")
            writeBandHeader(bandSegments.size, curveOffset, x)
            curveOffset += bandSegments.size
            asc.map { segmentIndices[it] } to desc.map { segmentIndices[it] }
        }

        for ((bindex, band) in hbands.withIndex()) {
            for ((cindex, i) in band.first.indices.withIndex()) {
                writeBandCurveIndex(band.first[i], IntVector2(bindex, cindex))
            }
        }

        for ((bindex, band) in vbands.withIndex()) {
            for ((cindex, i) in band.first.indices.withIndex()) {
                writeBandCurveIndex(band.first[i], IntVector2(bindex, cindex))
            }
        }

        shapes++
        return shapes - 1
    }
}

class SlugGlyphMap2(val slugMap: SlugMap2, val glyphs: MutableMap<Int, Int> = mutableMapOf()) {

    private fun hash(face: Face, index: Int): Int {
        return face.hashCode() * 31 + index.hashCode()
    }

    fun getSlugForGlyphIndex(face: Face, index: Int): Int {
        return glyphs.getOrPut(hash(face, index)) {
            val glyph = face.glyphForIndex(index)
            slugMap.addShape(glyph.shape())
        }
    }

    fun getGlyph(face: Face, char: Char): Int {
        val glyph = face.glyphForCharacter(char)
        return glyphs.getOrPut(hash(face, glyph.index)) {
            slugMap.addShape(glyph.shape())
        }
    }
}

