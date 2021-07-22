package utils

class DoubleAccumulator {
    private var values = DoubleArray(2)
    private var index = 0
    private fun expand() {
        val values = DoubleArray(values.size shl 1)
        //System.arraycopy(this.values, 0, values, 0, this.values.size)
        this.values.copyInto(values, 0)

        this.values = values
    }

    fun add(ns: DoubleArray): DoubleAccumulator {
        while (index > values.size - ns.size) {
            expand()
        }
        //java.lang.System.arraycopy(ns, 0, values, index, ns.size)
        ns.copyInto(values, index, 0, ns.size)

        index += ns.size
        return this
    }

    fun add(n: Double): DoubleAccumulator {
        if (index > values.size - 1) {
            expand()
        }
        values[index++] = n
        return this
    }

    fun add(a: Double, b: Double): DoubleAccumulator {
        if (index > values.size - 2) {
            expand()
        }
        values[index++] = a
        values[index++] = b
        return this
    }

    fun pop(num: Int) {
        index -= num
    }

    fun clear() {
        index = 0
    }

    fun size(): Int {
        return index
    }

    operator fun get(index: Int): Double {
        return values[index]
    }

    fun last(): Double {
        return values[index - 1]
    }

    operator fun set(index: Int, n: Double) {
        values[index] = n
    }

    fun toArray(): DoubleArray {
        val result = DoubleArray(size())
        values.copyInto(result, 0, 0, size())
        return result
    }

    override fun toString(): String {
        val b = StringBuilder()
        b.append("[")
        for (i in 0 until index) {
            if (i != 0) {
                b.append(", ")
            }
            b.append(values[i])
        }
        b.append("]")
        return b.toString()
    }
}
