package org.openrndr.shape

import org.openrndr.math.Vector2

fun sampleEquidistant(pieces: List<Vector2>, count: Int): List<Vector2> {


    val result = mutableListOf<Vector2>()

    if (pieces.isEmpty()) {
        return result
    }

    var totalLength = 0.0
    for (i in 0 until pieces.size - 1) {
        totalLength += pieces[i].minus(pieces[i + 1]).length
    }

    val spacing = totalLength / count

    var runLength = 0.0
    var cursor: Vector2
    result.add(Vector2(pieces[0].x, pieces[0].y))


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

    result.add(pieces[pieces.size - 1])

    return result
}

