package org.openrndr.shape

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.math.roundToInt

fun sampleEquidistant(segments: List<Vector2>, count: Int): List<Vector2> {
    val result = mutableListOf<Vector2>()

    if (segments.isEmpty()) {
        return result
    }

    var totalLength = 0.0
    for (i in 0 until segments.size - 1) {
        totalLength += segments[i].minus(segments[i + 1]).length
    }

    val spacing = totalLength / (count - 1)

    var runLength = 0.0
    var cursor: Vector2
    if (count > 0) {
        result.add(Vector2(segments[0].x, segments[0].y))
    }

    for (i in 0 until segments.size - 1) {
        val direction = segments[i + 1] - segments[i]
        val segmentLength = direction.length

        if (segmentLength + runLength < spacing) {
            runLength += segmentLength
        } else {
            val skip = (spacing - runLength).coerceAtLeast(0.0)
            val newSegmentCount = ((segmentLength - skip) / spacing).roundToInt()
            val skipT = skip / segmentLength
            val spaceT = spacing / segmentLength
            val start = segments[i]

            var t = skipT

            for (n in 0 until 1 + newSegmentCount) {
                cursor = start + direction * t
                t += spaceT
                result.add(cursor)
            }

            runLength = segmentLength - skip - newSegmentCount * spacing
        }
    }
    if (count >= 2) {
        if (result.size == count) {
            result[result.lastIndex] = segments.last()
        } else {
            result.add(segments.last())
        }
    }
    return result
}

@JvmName("sampleEquidistant3D")
fun sampleEquidistant(pieces: List<Vector3>, count: Int): List<Vector3> {
    val result = mutableListOf<Vector3>()

    if (pieces.isEmpty()) {
        return result
    }

    var totalLength = 0.0
    for (i in 0 until pieces.size - 1) {
        totalLength += (pieces[i] - pieces[i + 1]).length
    }

    val spacing = totalLength / count

    var runLength = 0.0
    var cursor: Vector3
    result.add(pieces[0])

    for (i in 0 until pieces.size - 1) {
        val piece = pieces[i] - pieces[i + 1]
        val pieceLength = piece.length

        if (pieceLength + runLength < spacing) {
            runLength += pieceLength
        } else {
            val skip = (spacing - runLength).coerceAtLeast(0.0)
            if (skip < 0) {
                throw RuntimeException("skip < 0 - $skip")
            }
            val newPieces = ((pieceLength - skip) / spacing).toInt()
            val skipT = skip / pieceLength
            val spaceT = spacing / pieceLength

            val direction = pieces[i + 1].minus(pieces[i])
            val start = pieces[i]

            var t = skipT

            for (n in 0 until 1 + newPieces) {
                cursor = start.plus(direction * t)
                t += spaceT
                result.add(cursor)
            }

            runLength = pieceLength - skip - newPieces * spacing
        }
    }
    result.add(pieces[pieces.size - 1])

    return result
}