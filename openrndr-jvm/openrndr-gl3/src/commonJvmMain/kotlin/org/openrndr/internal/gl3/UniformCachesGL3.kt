package org.openrndr.internal.gl3

import org.openrndr.math.Matrix33
import org.openrndr.math.Matrix44
import org.openrndr.math.Matrix55

/**
 * The values last set at the uniform locations of a shader, so that values that are set again are not uploaded.
 * Numbers and booleans are kept in primitive arrays, they are not boxed. Values of immutable types are kept as
 * objects and compared with `equals`.
 */
class UniformValueCache {
    private var kinds = ByteArray(16)
    private var bits = LongArray(16)
    private var objects = arrayOfNulls<Any>(16)

    /** values at locations past the arrays, which drivers rarely use */
    private val far = HashMap<Int, Any>()

    private fun fits(index: Int): Boolean {
        if (index < kinds.size) return true
        if (index >= MAX_DENSE) return false
        var size = kinds.size
        while (size <= index) size *= 2
        kinds = kinds.copyOf(size)
        bits = bits.copyOf(size)
        objects = objects.copyOf(size)
        return true
    }

    private inline fun changedBits(index: Int, kind: Byte, value: Long, boxed: () -> Any): Boolean {
        if (!fits(index)) {
            val box = boxed()
            if (far[index] == box) return false
            far[index] = box
            return true
        }
        if (kinds[index] == kind && bits[index] == value) return false
        kinds[index] = kind
        bits[index] = value
        objects[index] = null
        return true
    }

    /** is [value] different from the value last set at [index]? It is recorded when it is. */
    fun changed(index: Int, value: Double): Boolean = changedBits(index, DOUBLE, value.toRawBits()) { value }

    /** @see changed */
    fun changed(index: Int, value: Float): Boolean = changedBits(index, FLOAT, value.toRawBits().toLong()) { value }

    /** @see changed */
    fun changed(index: Int, value: Int): Boolean = changedBits(index, INT, value.toLong()) { value }

    /** @see changed */
    fun changed(index: Int, value: Boolean): Boolean = changedBits(index, BOOLEAN, if (value) 1L else 0L) { value }

    /** @see changed, [value] is of an immutable type */
    fun changed(index: Int, value: Any): Boolean {
        if (!fits(index)) {
            if (far[index] == value) return false
            far[index] = value
            return true
        }
        if (kinds[index] == OBJECT && objects[index] == value) return false
        kinds[index] = OBJECT
        objects[index] = value
        return true
    }

    /** forgets the value at [index], the next value set there is uploaded */
    fun clear(index: Int) {
        if (index < kinds.size) {
            kinds[index] = NONE
            objects[index] = null
        } else {
            far.remove(index)
        }
    }

    private companion object {
        const val NONE: Byte = 0
        const val DOUBLE: Byte = 1
        const val FLOAT: Byte = 2
        const val INT: Byte = 3
        const val BOOLEAN: Byte = 4
        const val OBJECT: Byte = 5
        const val MAX_DENSE = 1 shl 14
    }
}

/**
 * Arrays to convert uniform values into before they are uploaded, one set per thread. Uploads copy the values, so
 * the arrays can be reused right after.
 */
internal class UniformScratch {
    private var floats = arrayOfNulls<FloatArray>(32)
    private var ints = arrayOfNulls<IntArray>(32)

    /** a float array of exactly [size] elements, GL uploads all elements of an array */
    fun floats(size: Int): FloatArray {
        if (size >= floats.size) floats = floats.copyOf(maxOf(size + 1, floats.size * 2))
        return floats[size] ?: FloatArray(size).also { floats[size] = it }
    }

    /** an int array of exactly [size] elements */
    fun ints(size: Int): IntArray {
        if (size >= ints.size) ints = ints.copyOf(maxOf(size + 1, ints.size * 2))
        return ints[size] ?: IntArray(size).also { ints[size] = it }
    }

    companion object {
        private val scratch = ThreadLocal.withInitial { UniformScratch() }

        /** the scratch arrays of the current thread */
        val current: UniformScratch get() = scratch.get()
    }
}

/** writes the elements of this matrix in column-major order into [target] at [offset] */
internal fun Matrix44.copyInto(target: FloatArray, offset: Int = 0) {
    target[offset] = c0r0.toFloat(); target[offset + 1] = c0r1.toFloat(); target[offset + 2] = c0r2.toFloat(); target[offset + 3] = c0r3.toFloat()
    target[offset + 4] = c1r0.toFloat(); target[offset + 5] = c1r1.toFloat(); target[offset + 6] = c1r2.toFloat(); target[offset + 7] = c1r3.toFloat()
    target[offset + 8] = c2r0.toFloat(); target[offset + 9] = c2r1.toFloat(); target[offset + 10] = c2r2.toFloat(); target[offset + 11] = c2r3.toFloat()
    target[offset + 12] = c3r0.toFloat(); target[offset + 13] = c3r1.toFloat(); target[offset + 14] = c3r2.toFloat(); target[offset + 15] = c3r3.toFloat()
}

/** writes the elements of this matrix in column-major order into [target] at [offset] */
internal fun Matrix33.copyInto(target: FloatArray, offset: Int = 0) {
    target[offset] = c0r0.toFloat(); target[offset + 1] = c0r1.toFloat(); target[offset + 2] = c0r2.toFloat()
    target[offset + 3] = c1r0.toFloat(); target[offset + 4] = c1r1.toFloat(); target[offset + 5] = c1r2.toFloat()
    target[offset + 6] = c2r0.toFloat(); target[offset + 7] = c2r1.toFloat(); target[offset + 8] = c2r2.toFloat()
}

/** writes the elements of this matrix into [target] in the order of [Matrix55.floatArray] */
internal fun Matrix55.copyInto(target: FloatArray) {
    target[0] = c0r0.toFloat(); target[1] = c0r1.toFloat(); target[2] = c0r2.toFloat(); target[3] = c0r3.toFloat(); target[4] = c0r4.toFloat()
    target[5] = c1r0.toFloat(); target[6] = c1r1.toFloat(); target[7] = c1r2.toFloat(); target[8] = c1r3.toFloat(); target[9] = c1r4.toFloat()
    target[10] = c2r0.toFloat(); target[11] = c2r1.toFloat(); target[12] = c2r2.toFloat(); target[13] = c2r3.toFloat(); target[14] = c2r4.toFloat()
    target[15] = c3r0.toFloat(); target[16] = c3r1.toFloat(); target[17] = c3r2.toFloat(); target[18] = c3r3.toFloat(); target[19] = c3r4.toFloat()
    target[20] = c4r0.toFloat(); target[21] = c4r1.toFloat(); target[22] = c4r2.toFloat(); target[23] = c4r3.toFloat(); target[24] = c4r4.toFloat()
}
