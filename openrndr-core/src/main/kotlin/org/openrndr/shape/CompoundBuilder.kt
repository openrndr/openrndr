package org.openrndr.shape

class CompoundBuilder {

    inner class OpBuilder {
        val operands = mutableListOf<List<Shape>>()

        /** specify a shape operand of [Shape] */
        fun shape(shape: Shape) {
            operands.add(listOf(shape))
        }

        /** specify a shape operand of [ShapeContour] */
        fun shape(contour: ShapeContour) {
            operands.add(listOf(Shape(listOf(contour))))
        }

        /** describe a difference */
        fun difference(f:CompoundBuilder.OpBuilder.() -> Unit) {
            operands.add(this@CompoundBuilder.difference(f))
        }

        /** describe a union */
        fun union(f:CompoundBuilder.OpBuilder.() -> Unit) {
            operands.add(this@CompoundBuilder.union(f))
        }

        /** describe an intersection */
        fun intersection(f:CompoundBuilder.OpBuilder.() -> Unit) {
            operands.add(this@CompoundBuilder.intersection(f))
        }

        internal val difference:List<Shape> get() = difference(operands[0], operands.drop(1))
        internal val union:List<Shape> get() = union(operands[0], operands.drop(1))
        internal val intersection:List<Shape> get() = intersection(operands[0], operands.drop(1))
    }

    /** describe a difference */
    fun difference(f:CompoundBuilder.OpBuilder.() -> Unit) : List<Shape> {
        val ob = OpBuilder()
        ob.f()
        return ob.difference
    }

    /** describe a union */
    fun union(f:CompoundBuilder.OpBuilder.() -> Unit) : List<Shape> {
        val ob = OpBuilder()
        ob.f()
        return ob.union
    }

    /** describe an intersection */
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