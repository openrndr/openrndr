package debug

import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.Segment2D
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import java.io.File

/*
 * A bit-exact text format for shapes, meant for capturing inputs that make shape handling code
 * misbehave, so they can be reproduced elsewhere (e.g. in a test) without any loss of precision.
 *
 * Every double is stored as the 16 hex digits of its raw IEEE 754 bit pattern, so values such as
 * -0.0, subnormals and NaN payloads survive a round trip unchanged. The format is line based:
 *
 *     openrndr-shape-bit-exact 1
 *     shape <contour count>
 *     contour <closed: 0|1> <polarity> <segment count>
 *     segment <corner: 0|1> <control count> <start x> <start y> [<control x> <control y>]... <end x> <end y>
 *
 * Each `contour` line is followed by its `segment` lines.
 */

private const val HEADER = "openrndr-shape-bit-exact"
private const val VERSION = 1

private fun Double.toBitString(): String = toRawBits().toULong().toString(16).padStart(16, '0')

private fun String.toBitExactDouble(): Double = Double.fromBits(toULong(16).toLong())

/**
 * Writes this shape to [file] such that [Shape.Companion.loadBitExact] reproduces it exactly, down
 * to the bit pattern of every coordinate.
 */
fun Shape.persistBitExact(file: File) {
    file.bufferedWriter().use { w ->
        w.write("$HEADER $VERSION\n")
        w.write("shape ${contours.size}\n")
        for (contour in contours) {
            w.write("contour ${if (contour.closed) 1 else 0} ${contour.polarity.name} ${contour.segments.size}\n")
            for (segment in contour.segments) {
                w.write("segment ${if (segment.corner) 1 else 0} ${segment.control.size}")
                for (v in listOf(segment.start) + segment.control + segment.end) {
                    w.write(" ${v.x.toBitString()} ${v.y.toBitString()}")
                }
                w.write("\n")
            }
        }
    }
}

/**
 * Reads a shape written by [Shape.persistBitExact] from [file].
 */
fun Shape.Companion.loadBitExact(file: File): Shape {
    val lines = file.readLines().filter { it.isNotBlank() }.iterator()
    var lineNumber = 0

    fun next(keyword: String): List<String> {
        require(lines.hasNext()) { "${file.path}: unexpected end of file, expected '$keyword'" }
        lineNumber++
        val tokens = lines.next().trim().split(Regex("\\s+"))
        require(tokens[0] == keyword) {
            "${file.path}:$lineNumber: expected '$keyword', found '${tokens[0]}'"
        }
        return tokens.drop(1)
    }

    val header = next(HEADER)
    require(header.singleOrNull() == VERSION.toString()) {
        "${file.path}: unsupported $HEADER version '${header.joinToString(" ")}'"
    }

    val contourCount = next("shape").single().toInt()
    val contours = List(contourCount) {
        val (closed, polarity, segmentCount) = next("contour")
        val segments = List(segmentCount.toInt()) {
            val tokens = next("segment")
            val corner = tokens[0] == "1"
            val pointCount = tokens[1].toInt() + 2
            require(tokens.size == 2 + pointCount * 2) {
                "${file.path}:$lineNumber: expected ${pointCount * 2} coordinates, found ${tokens.size - 2}"
            }
            val points = List(pointCount) { i ->
                Vector2(tokens[2 + i * 2].toBitExactDouble(), tokens[3 + i * 2].toBitExactDouble())
            }
            Segment2D(points.first(), points.subList(1, pointCount - 1).toList(), points.last(), corner)
        }
        ShapeContour(segments, closed == "1", YPolarity.valueOf(polarity))
    }
    require(!lines.hasNext()) { "${file.path}: unexpected content after the last contour" }
    return Shape(contours)
}
