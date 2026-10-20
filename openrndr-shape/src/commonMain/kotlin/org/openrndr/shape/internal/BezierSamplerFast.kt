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
 * Parameters at which [segment] should be sampled such that the resulting polyline deviates at most
 * [tolerance] * [TOLERANCE_SCALE] from the segment. The first and last parameters are exactly 0.0 and 1.0.
 */
internal fun flattenSegmentTs(segment: Segment2D, tolerance: Double): List<Double> =
    when (segment.type) {
        SegmentType.LINEAR -> listOf(0.0, 1.0)
        SegmentType.QUADRATIC -> flattenQuadraticTs(segment, tolerance * TOLERANCE_SCALE)
        SegmentType.CUBIC -> flattenCubicTs(segment, tolerance * TOLERANCE_SCALE)
    }

internal fun flattenSegmentTs(segment: Segment3D, tolerance: Double): List<Double> =
    when (segment.type) {
        SegmentType.LINEAR -> listOf(0.0, 1.0)
        SegmentType.QUADRATIC -> flattenQuadraticTs(segment, tolerance * TOLERANCE_SCALE)
        SegmentType.CUBIC -> flattenCubicTs(segment, tolerance * TOLERANCE_SCALE)
    }

internal fun flattenSegment(segment: Segment2D, tolerance: Double): List<Vector2> =
    flattenSegmentTs(segment, tolerance).map { segment.position(it) }

internal fun flattenSegmentWithT(segment: Segment2D, tolerance: Double): List<Pair<Vector2, Double>> =
    flattenSegmentTs(segment, tolerance).map { segment.position(it) to it }

internal fun flattenSegment(segment: Segment3D, tolerance: Double): List<Vector3> =
    flattenSegmentTs(segment, tolerance).map { segment.position(it) }

internal fun flattenSegmentWithT(segment: Segment3D, tolerance: Double): List<Pair<Vector3, Double>> =
    flattenSegmentTs(segment, tolerance).map { segment.position(it) to it }
