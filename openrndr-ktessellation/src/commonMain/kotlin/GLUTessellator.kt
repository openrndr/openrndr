package org.openrndr.ktessellation

interface GLUtessellator {
    fun gluDeleteTess()
    fun gluTessProperty(which: Int, value: Double)
    fun gluTessProperty(which: Int, value: Int) {
        gluTessProperty(which, value.toDouble())
    }

    /* Returns tessellator property */
    fun gluGetTessProperty(
        which: Int, value: DoubleArray,
        value_offset: Int
    ) /* gluGetTessProperty() */

    fun gluTessNormal(x: Double, y: Double, z: Double)
    fun gluTessCallback(
        which: Int,
        aCallback: GLUtessellatorCallback?
    )

    fun gluTessVertex(
        coords: DoubleArray, coords_offset: Int,
        vertexData: Any?
    )

    fun gluTessBeginPolygon(data: Any?)
    fun gluTessBeginContour()
    fun gluTessEndContour()
    fun gluTessEndPolygon()

    /** */ /* Obsolete calls -- for backward compatibility */
    fun gluBeginPolygon()

    /*ARGSUSED*/
    fun gluNextContour(type: Int)
    fun gluEndPolygon()
}