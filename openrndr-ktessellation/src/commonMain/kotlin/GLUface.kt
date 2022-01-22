package org.openrndr.ktessellation


internal class GLUface {
    var next /* next face (never NULL) */: GLUface? = null
    var prev /* previous face (never NULL) */: GLUface? = null
    var anEdge /* a half edge with this left face */: GLUhalfEdge? = null
    var data /* room for client's data */: Any? = null

    /* Internal data (keep hidden) */
    var trail /* "stack" for conversion to strips */: GLUface? = null
    var marked /* flag for conversion to strips */ = false
    var inside /* this face is in the polygon interior */ = false
}

