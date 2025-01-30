package org.openrndr.ktessellation


/**
 * Internal representation of a face in the tessellation process.
 *
 * This class is used as part of the tessellation algorithm to manage and
 * maintain data about the left faces formed during tessellation. It contains
 * connections to other faces, associated half-edges, and additional flags that
 * aid in the management and traversal of its structure.
 *
 * @constructor Creates an instance of GLUface.
 */
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

