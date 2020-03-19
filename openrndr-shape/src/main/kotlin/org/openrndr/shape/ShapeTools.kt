package org.openrndr.shape

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

fun sampleEquidistant(pieces: List<Vector2>, count: Int): List<Vector2> {
    val result = mutableListOf<Vector2>()

    if (pieces.isEmpty()) {
        return result
    }

    var totalLength = 0.0
    for (i in 0 until pieces.size - 1) {
        totalLength += pieces[i].minus(pieces[i + 1]).length
    }

    val spacing = totalLength / (count - 1.0)

    var runLength = 0.0
    var cursor: Vector2
    if (count > 0) {
        result.add(Vector2(pieces[0].x, pieces[0].y))
    }

    for (i in 0 until pieces.size - 1) {
        val piece = pieces[i].minus(pieces[i + 1])
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