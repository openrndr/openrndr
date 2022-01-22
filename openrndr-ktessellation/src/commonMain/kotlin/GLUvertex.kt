package org.openrndr.ktessellation

internal class GLUvertex {
    var next /* next vertex (never NULL) */: GLUvertex? = null
    var prev /* previous vertex (never NULL) */: GLUvertex? = null
    var anEdge /* a half-edge with this origin */: GLUhalfEdge? = null
    var data /* client's data */: Any? = null

    /* Internal data (keep hidden) */
    var coords = DoubleArray(3) /* vertex location in 3D */
    var s = 0.0
    var t /* projection onto the sweep plane */ = 0.0
    var pqHandle /* to allow deletion from priority queue */ = 0
}
