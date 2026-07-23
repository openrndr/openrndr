package org.openrndr.draw.slug

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.Glyph
import org.openrndr.math.IntVector2
import org.openrndr.shape.Shape
import org.openrndr.shape.toQuadratics
import org.openrndr.utils.buffer.MPPBuffer
import kotlin.math.ceil

class SlugMap(val coordinates: ColorBuffer, val index: ColorBuffer) {
    private var totalSegments = 0
    private var shapes = 0

    fun writeCoordinates(buffer: MPPBuffer, point: Int) {
        val x = (totalSegments * 3 + point).mod(coordinates.width)
        val y = (totalSegments * 3 + point) / coordinates.width
        require(y < coordinates.height)
        coordinates.write(buffer, x = x, y = y, width = 1, height = 1)
    }

    fun writeIndex(buffer: MPPBuffer, point: Int) {
        val x = (shapes * 3 + point).mod(index.width)
        val y = (shapes * 3 + point) / index.width
        require(y < index.height)
        index.write(buffer, x = x, y = y, width = 1, height = 1)
    }

    fun addShape(shape: Shape, quadraticTolerance: Double = 1.0): Int {
        val segments = shape.contours.flatMap { it.reversed.segments.flatMap { it.toQuadratics(quadraticTolerance) } }

        val buffer = MPPBuffer.allocate(2 * 4)
        buffer.rewind()

        val startSegment = totalSegments
        for (i in 0 until segments.size) {
            val segment = segments[i]

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
        }

        val indexBuffer = MPPBuffer.allocate(4 * 2)
        indexBuffer.putFloat(startSegment.toFloat())
        indexBuffer.putFloat(segments.size.toFloat())
        indexBuffer.rewind()
        writeIndex(indexBuffer, 0)

        val bounds = shape.bounds
        indexBuffer.putFloat(bounds.position(0.0, 0.0).x.toFloat())
        indexBuffer.putFloat(bounds.position(0.0, 0.0).y.toFloat())
        indexBuffer.rewind()
        writeIndex(indexBuffer, 1)

        indexBuffer.putFloat(bounds.position(1.0, 1.0).x.toFloat())
        indexBuffer.putFloat(bounds.position(1.0, 1.0).y.toFloat())
        indexBuffer.rewind()
        writeIndex(indexBuffer, 2)

        shapes++
        return shapes - 1
    }
}

class SlugGlyphMap(val slugMap: SlugMap, val glyphs: MutableMap<Int, Int> = mutableMapOf()) {

    private fun hash(face: Face, index: Int): Int {
        return face.hashCode()*31 + index.hashCode()
    }

    fun getGlyphForIndex(face: Face, index: Int): Int {

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

