package org.openrndr.shape.internal

/**
 * A growable list of doubles without boxing, for the parameters that the flatteners produce
 */
internal class DoubleList(capacity: Int = 16) {
    private var values = DoubleArray(capacity)

    var size = 0
        private set

    operator fun get(index: Int): Double = values[index]

    fun add(value: Double) {
        if (size == values.size) {
            values = values.copyOf(values.size * 2)
        }
        values[size++] = value
    }

    fun clear() {
        size = 0
    }

    fun toList(): List<Double> = List(size) { values[it] }
}
