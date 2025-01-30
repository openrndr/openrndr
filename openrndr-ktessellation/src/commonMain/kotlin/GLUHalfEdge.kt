/*
* Portions Copyright (C) 2003-2006 Sun Microsystems, Inc.
* All rights reserved.
*/
/*
** License Applicability. Except to the extent portions of this file are
** made subject to an alternative license as permitted in the SGI Free
** Software License B, Version 1.1 (the "License"), the contents of this
** file are subject only to the provisions of the License. You may not use
** this file except in compliance with the License. You may obtain a copy
** of the License at Silicon Graphics, Inc., attn: Legal Services, 1600
** Amphitheatre Parkway, Mountain View, CA 94043-1351, or at:
**
** http://oss.sgi.com/projects/FreeB
**
** Note that, as provided in the License, the Software is distributed on an
** "AS IS" basis, with ALL EXPRESS AND IMPLIED WARRANTIES AND CONDITIONS
** DISCLAIMED, INCLUDING, WITHOUT LIMITATION, ANY IMPLIED WARRANTIES AND
** CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A
** PARTICULAR PURPOSE, AND NON-INFRINGEMENT.
**
** NOTE:  The Original Code (as defined below) has been licensed to Sun
** Microsystems, Inc. ("Sun") under the SGI Free Software License B
** (Version 1.1), shown above ("SGI License").   Pursuant to Section
** 3.2(3) of the SGI License, Sun is distributing the Covered Code to
** you under an alternative license ("Alternative License").  This
** Alternative License includes all of the provisions of the SGI License
** except that Section 2.2 and 11 are omitted.  Any differences between
** the Alternative License and the SGI License are offered solely by Sun
** and not by SGI.
**
** Original Code. The Original Code is: OpenGL Sample Implementation,
** Version 1.2.1, released January 26, 2000, developed by Silicon Graphics,
** Inc. The Original Code is Copyright (c) 1991-2000 Silicon Graphics, Inc.
** Copyright in any portions created by third parties is as indicated
** elsewhere herein. All Rights Reserved.
**
** Additional Notice Provisions: The application programming interfaces
** established by SGI in conjunction with the Original Code are The
** OpenGL(R) Graphics System: A Specification (Version 1.2.1), released
** April 1, 1999; The OpenGL(R) Graphics System Utility Library (Version
** 1.3), released November 4, 1998; and OpenGL(R) Graphics with the X
** Window System(R) (Version 1.3), released October 19, 1998. This software
** was created using the OpenGL(R) version 1.2.1 Sample Implementation
** published by SGI, but has not been independently verified as being
** compliant with the OpenGL(R) version 1.2.1 Specification.
**
** Author: Eric Veach, July 1994
** Java Port: Pepijn VanEeckhoudt, July 2003
** Java Port: Nathan Parker Burg, August 2003
** Kotlin Port: Edwin Jakobs, December 2021
*/
package org.openrndr.ktessellation

/**
 * Represents a half-edge in the tessellation algorithm.
 *
 * This class is a core part of the tessellation process, managing relationships
 * between vertices, faces, and neighboring edges. Each half-edge has an
 * associated symmetric counterpart and participates in various linked lists
 * that allow traversal and manipulation of the tessellation structure.
 *
 * @constructor Creates an instance of GLUhalfEdge, specifying whether this
 * half-edge is the first in its pair.
 *
 * Properties:
 * - `first`: Indicates whether this half-edge is the primary in a pair of symmetric edges.
 * - `next`: Points to the next half-edge in the doubly-linked list.
 * - `Sym`: Points to the symmetric half-edge, which is the same edge in the
 * opposite direction.
 * - `Onext`: Points to the next edge counter-clockwise around the origin vertex.
 * - `Lnext`: Points to the next edge counter-clockwise around the left face.
 * - `Org`: References the origin vertex of the half-edge.
 * - `Lface`: References the left face associated with the half-edge.
 * - `activeRegion`: References the active region that this edge acts as an
 * upper edge for, used during the sweep algorithm.
 * - `winding`: Indicates the change in winding number associated with this
 * edge during traversal.
 */
internal class GLUhalfEdge(var first: Boolean) {
    var next /* doubly-linked list (prev==Sym->next) */: GLUhalfEdge? = null
    var Sym /* same edge, opposite direction */: GLUhalfEdge? = null
    var Onext /* next edge CCW around origin */: GLUhalfEdge? = null
    var Lnext /* next edge CCW around left face */: GLUhalfEdge? = null
    var Org /* origin vertex (Overtex too long) */: GLUvertex? = null
    var Lface /* left face */: GLUface? = null

    /* Internal data (keep hidden) */
    var activeRegion /* a region with this upper edge (sweep.c) */: ActiveRegion? = null
    var winding /* change in winding number when crossing */ = 0
}