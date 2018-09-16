package org.openrndr.shape

class CompoundBuilder {

    inner class OpBuilder {
        val operands = mutableListOf<List<Shape>>()
        fun shape(shape: Shape) {
            operands.add(listOf(shape))
        }
        fun shape(contour: ShapeContour) {
            operands.add(listOf(Shape(listOf(contour))))
        }

        fun difference(f:CompoundBuilder.OpBuilder.() -> Unit) {
            operands.add(this@CompoundBuilder.difference(f))
        }

        fun union(f:CompoundBuilder.OpBuilder.() -> Unit) {
            operands.add(this@CompoundBuilder.union(f))
        }

        fun intersection(f:CompoundBuilder.OpBuilder.() -> Unit) {
            operands.add(this@CompoundBuilder.intersection(f))
        }

        internal val difference:List<Shape> get() = difference(operands[0], operands[1][0])
        internal val union:List<Shape> get() = union(operands[0], operands[1][0])
        internal val intersection:List<Shape> get() = intersection(operands[0], operands[1][0])
    }

    fun difference(f:CompoundBuilder.OpBuilder.() -> Unit) : List<Shape> {
        val ob = OpBuilder()
        ob.f()
        return ob.difference
    }

    fun union(f:CompoundBuilder.OpBuilder.() -> Unit) : List<Shape> {
        val ob = OpBuilder()
        ob.f()
        return ob.union
    }

    fun intersection(f:CompoundBuilder.OpBuilder.() -> Unit) : List<Shape> {
        val ob = OpBuilder()
        ob.f()
        return ob.intersection
    }
}

fun compound(f: CompoundBuilder.() -> List<Shape>): List<Shape> {
    val cb = CompoundBuilder()
    return cb.f()
}