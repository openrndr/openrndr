package org.openrndr.internal

import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment

/**
 * A list of [size] elements that are all true, without storage
 */
internal class AllTrue(override val size: Int) : AbstractList<Boolean>() {
    override fun get(index: Int): Boolean {
        if (index !in 0 until size) throw IndexOutOfBoundsException("index $index, size $size")
        return true
    }
}

/**
 * For every list in [lists] an [AllTrue] list of its size, the corners of lines whose points are all corners
 */
internal class AllTrueLists(private val lists: List<List<*>>) : AbstractList<List<Boolean>>() {
    override val size: Int get() = lists.size
    override fun get(index: Int): List<Boolean> = AllTrue(lists[index].size)
}

/**
 * The start and end points of [segments], the start of every segment followed by its end
 */
internal class SegmentPoints(private val segments: List<LineSegment>) : AbstractList<Vector2>() {
    override val size: Int get() = segments.size * 2
    override fun get(index: Int): Vector2 {
        if (index !in 0 until size) throw IndexOutOfBoundsException("index $index, size $size")
        val segment = segments[index / 2]
        return if (index % 2 == 0) segment.start else segment.end
    }
}
