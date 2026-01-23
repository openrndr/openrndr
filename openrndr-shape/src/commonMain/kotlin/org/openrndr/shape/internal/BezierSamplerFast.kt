package org.openrndr.shape.internal

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Segment2D
import org.openrndr.shape.Segment3D
import org.openrndr.shape.SegmentType

fun flattenSegment(segment: Segment2D, toleranceC: Double, toleranceQ: Double): List<Vector2> {
    return when (segment.type) {
        SegmentType.LINEAR -> listOf(segment.start, segment.end)
        SegmentType.QUADRATIC -> flattenQuadratic(segment, segment, toleranceQ)
        SegmentType.CUBIC -> flattenCubic(segment, toleranceC * 0.1, toleranceQ * 0.1)
    }
}

fun flattenSegmentWithT(segment: Segment2D, toleranceC: Double, toleranceQ: Double): List<Pair<Vector2, Double>> {
    return when (segment.type) {
        SegmentType.LINEAR -> listOf(segment.start to 0.0, segment.end to 1.0)
        SegmentType.QUADRATIC -> flattenQuadraticWithT(segment, segment, toleranceQ)
        SegmentType.CUBIC -> flattenCubicWithT(segment, toleranceC * 0.1, toleranceQ * 0.1)
    }
}

fun flattenSegment(segment: Segment3D, toleranceC: Double, toleranceQ: Double): List<Vector3> {
    return when (segment.type) {
        SegmentType.LINEAR -> listOf(segment.start, segment.end)
        SegmentType.QUADRATIC -> flattenQuadratic(segment, segment, toleranceQ)
        SegmentType.CUBIC -> flattenCubic(segment, toleranceC * 0.1, toleranceQ * 0.1)
    }
}

fun flattenSegmentWithT(segment: Segment3D, toleranceC: Double, toleranceQ: Double): List<Pair<Vector3, Double>> {
    return when (segment.type) {
        SegmentType.LINEAR -> listOf(segment.start to 0.0, segment.end to 1.0)
        SegmentType.QUADRATIC -> flattenQuadraticWithT(segment, segment, toleranceQ)
        SegmentType.CUBIC -> flattenCubicWithT(segment, toleranceC * 0.1, toleranceQ * 0.1)
    }
}