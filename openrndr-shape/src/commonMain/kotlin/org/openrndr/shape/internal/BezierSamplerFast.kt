package org.openrndr.shape.internal

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Segment2D
import org.openrndr.shape.Segment3D
import org.openrndr.shape.SegmentType

/**
 * Scale applied to the user facing distance tolerance. The flattener meets its tolerance tightly, the
 * scale keeps the point density (and with that the accuracy of length based operations) in line with
 * the recursive sampler it replaced.
 */
private const val TOLERANCE_SCALE = 0.3

/**
 * Appends to [out] the parameters at which [segment] should be sampled such that the resulting polyline deviates at
 * most [tolerance] * [TOLERANCE_SCALE] from the segment. The first and last parameters are exactly 0.0 and 1.0.
 */
internal fun flattenSegmentTs(segment: Segment2D, tolerance: Double, out: DoubleList) {
    when (segment.type) {
        SegmentType.LINEAR -> { out.add(0.0); out.add(1.0) }
        SegmentType.QUADRATIC -> flattenQuadraticTs(segment, tolerance * TOLERANCE_SCALE, out)
        SegmentType.CUBIC -> flattenCubicTs(segment, tolerance * TOLERANCE_SCALE, out)
    }
}

internal fun flattenSegmentTs(segment: Segment3D, tolerance: Double, out: DoubleList) {
    when (segment.type) {
        SegmentType.LINEAR -> { out.add(0.0); out.add(1.0) }
        SegmentType.QUADRATIC -> flattenQuadraticTs(segment, tolerance * TOLERANCE_SCALE, out)
        SegmentType.CUBIC -> flattenCubicTs(segment, tolerance * TOLERANCE_SCALE, out)
    }
}

/** the parameters of [flattenSegmentTs] as a list */
internal fun flattenSegmentTs(segment: Segment2D, tolerance: Double): List<Double> =
    DoubleList().also { flattenSegmentTs(segment, tolerance, it) }.toList()

internal fun flattenSegmentTs(segment: Segment3D, tolerance: Double): List<Double> =
    DoubleList().also { flattenSegmentTs(segment, tolerance, it) }.toList()

internal fun flattenSegment(segment: Segment2D, tolerance: Double): List<Vector2> {
    val ts = DoubleList()
    flattenSegmentTs(segment, tolerance, ts)
    return List(ts.size) { segment.position(ts[it]) }
}

internal fun flattenSegmentWithT(segment: Segment2D, tolerance: Double): List<Pair<Vector2, Double>> {
    val ts = DoubleList()
    flattenSegmentTs(segment, tolerance, ts)
    return List(ts.size) { segment.position(ts[it]) to ts[it] }
}

internal fun flattenSegment(segment: Segment3D, tolerance: Double): List<Vector3> {
    val ts = DoubleList()
    flattenSegmentTs(segment, tolerance, ts)
    return List(ts.size) { segment.position(ts[it]) }
}

internal fun flattenSegmentWithT(segment: Segment3D, tolerance: Double): List<Pair<Vector3, Double>> {
    val ts = DoubleList()
    flattenSegmentTs(segment, tolerance, ts)
    return List(ts.size) { segment.position(ts[it]) to ts[it] }
}

/**
 * Appends the flattened positions of [segments] to [positions], skipping positions equal to the one before them, like
 * [org.openrndr.shape.Path.adaptivePositions]. When [corners] is given, a corner flag is appended for every position:
 * the corner of the segment at its first and last sample, false in between.
 */
internal fun flattenSegments(
    segments: List<Segment2D>,
    tolerance: Double,
    positions: MutableList<Vector2>,
    corners: MutableList<Boolean>? = null
) {
    val ts = DoubleList()
    for (segment in segments) {
        ts.clear()
        flattenSegmentTs(segment, tolerance, ts)
        val last = ts.size - 1
        for (i in 0 until ts.size) {
            val position = segment.position(ts[i])
            if (positions.isEmpty() || positions[positions.size - 1].squaredDistanceTo(position) > 0.0) {
                positions.add(position)
                corners?.add(if (i == 0 || i == last) segment.corner else false)
            }
        }
    }
}

internal fun flattenSegments(segments: List<Segment3D>, tolerance: Double, positions: MutableList<Vector3>) {
    val ts = DoubleList()
    for (segment in segments) {
        ts.clear()
        flattenSegmentTs(segment, tolerance, ts)
        for (i in 0 until ts.size) {
            val position = segment.position(ts[i])
            if (positions.isEmpty() || positions[positions.size - 1].squaredDistanceTo(position) > 0.0) {
                positions.add(position)
            }
        }
    }
}
