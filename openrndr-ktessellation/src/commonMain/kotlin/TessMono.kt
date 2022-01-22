package org.openrndr.ktessellation

internal object TessMono {
    /* __gl_meshTessellateMonoRegion( face ) tessellates a monotone region
 * (what else would it do??)  The region must consist of a single
 * loop of half-edges (see mesh.h) oriented CCW.  "Monotone" in this
 * case means that any vertical line intersects the interior of the
 * region in a single interval.
 *
 * Tessellation consists of adding interior edges (actually pairs of
 * half-edges), to split the region into non-overlapping triangles.
 *
 * The basic idea is explained in Preparata and Shamos (which I don''t
 * have handy right now), although their implementation is more
 * complicated than this one.  The are two edge chains, an upper chain
 * and a lower chain.  We process all vertices from both chains in order,
 * from right to left.
 *
 * The algorithm ensures that the following invariant holds after each
 * vertex is processed: the untessellated region consists of two
 * chains, where one chain (say the upper) is a single edge, and
 * the other chain is concave.  The left vertex of the single edge
 * is always to the left of all vertices in the concave chain.
 *
 * Each step consists of adding the rightmost unprocessed vertex to one
 * of the two chains, and forming a fan of triangles from the rightmost
 * of two chain endpoints.  Determining whether we can add each triangle
 * to the fan is a simple orientation test.  By making the fan as large
 * as possible, we restore the invariant (check it yourself).
 */
    fun __gl_meshTessellateMonoRegion(face: GLUface): Boolean {
        var up: GLUhalfEdge
        var lo: GLUhalfEdge

        /* All edges are oriented CCW around the boundary of the region.
         * First, find the half-edge whose origin vertex is rightmost.
         * Since the sweep goes from left to right, face->anEdge should
         * be close to the edge we want.
         */
        up = face.anEdge ?: error("face.anEdge == null")
        require(up.Lnext !== up && up.Lnext?.Lnext !== up)
        while (Geom.VertLeq(up.Sym?.Org ?: error("up.Sym.org == null"), up.Org ?: error("up.Sym.org == null"))) {
            up = up.Onext?.Sym ?: error("up.Onext.sym = null")
        }
        while (Geom.VertLeq(up.Org ?: error("up.Org == null"), up.Sym?.Org ?: error("up.Sym.Org == null"))) {
            up = up.Lnext ?: error("up.Lnext == null")
        }
        lo = up.Onext?.Sym ?: error("up.Onext.Sym == null")
        while (up.Lnext !== lo) {
            if (Geom.VertLeq(up.Sym?.Org ?: error("up.Sym.Org == null"), lo.Org ?: error("lo.Org == null"))) {
                /* up.Sym.Org is on the left.  It is safe to form triangles from lo.Org.
                 * The EdgeGoesLeft test guarantees progress even when some triangles
                 * are CW, given that the upper and lower chains are truly monotone.
                 */
                while (lo.Lnext !== up && (Geom.EdgeGoesLeft(lo.Lnext ?: error("lo.Lnext == null"))
                            || Geom.EdgeSign(lo.Org ?: error("lo.Org == null"),
                        lo.Sym?.Org ?: error("lo.Sym.Org = null"), lo.Lnext?.Sym?.Org ?: error("lo.Lnext.Sym.Org == null")) <= 0)
                ) {
                    val tempHalfEdge: GLUhalfEdge =
                        Mesh.__gl_meshConnect(lo.Lnext!!, lo) ?: return false
                    lo = tempHalfEdge.Sym ?: error("tempHalfEdge.Sym == null")
                }
                lo = lo.Onext?.Sym ?: error("lo.Onext.Sym == null")
            } else {
                /* lo.Org is on the left.  We can make CCW triangles from up.Sym.Org. */
                while (lo.Lnext !== up && (Geom.EdgeGoesRight(up.Onext?.Sym ?: error("up.Onext.Sym == null"))
                            || Geom.EdgeSign(up.Sym?.Org ?: error("up.Sym.Org == null"), up.Org ?: error("up.Org == null"),
                        up.Onext?.Sym?.Org ?: error("up.Onext.Sym.Org == null")) >= 0)
                ) {
                    val tempHalfEdge: GLUhalfEdge =
                        Mesh.__gl_meshConnect(up, up.Onext?.Sym ?: error("up.Onext.Sym == null"))
                            ?: return false
                    up = tempHalfEdge.Sym ?: error("tempHalfEdge.Sym == null")
                }
                up = up.Lnext ?: error("up.Lnext == null")
            }
        }
        require(lo.Lnext !== up)
        while ((lo.Lnext?.Lnext ?: error("lo.Lnext.Lnext == null")) !== up) {
            val tempHalfEdge: GLUhalfEdge = Mesh.__gl_meshConnect(lo.Lnext!!, lo)
                ?: return false
            lo = tempHalfEdge.Sym ?: error("tempHalfEdge.Sym == null")
        }
        return true
    }

    /* __gl_meshTessellateInterior( mesh ) tessellates each region of
 * the mesh which is marked "inside" the polygon.  Each such region
 * must be monotone.
 */
    fun __gl_meshTessellateInterior(mesh: GLUmesh): Boolean {
        var f: GLUface
        var next: GLUface

        f = mesh.fHead.next ?: error("mesh.fHead.next == null")
        while (f !== mesh.fHead) {

            /* Make sure we don''t try to tessellate the new triangles. */
            next = f.next ?: error("f.next == null")
            if (f.inside) {
                if (!__gl_meshTessellateMonoRegion(f)) return false
            }
            f = next
        }
        return true
    }

    /* __gl_meshDiscardExterior( mesh ) zaps (ie. sets to NULL) all faces
 * which are not marked "inside" the polygon.  Since further mesh operations
 * on NULL faces are not allowed, the main purpose is to clean up the
 * mesh so that exterior loops are not represented in the data structure.
 */
    fun __gl_meshDiscardExterior(mesh: GLUmesh) {
        var f: GLUface
        var next: GLUface

        /*LINTED*/
        f = mesh.fHead.next ?: error("mesh.fHead.next == null")
        while (f !== mesh.fHead) {

            /* Since f will be destroyed, save its next pointer. */next = f.next ?: error("f.next == null")
            if (!f.inside) {
                Mesh.__gl_meshZapFace(f)
            }
            f = next
        }
    }

    //    private static final int MARKED_FOR_DELETION = 0x7fffffff;
    /* __gl_meshSetWindingNumber( mesh, value, keepOnlyBoundary ) resets the
 * winding numbers on all edges so that regions marked "inside" the
 * polygon have a winding number of "value", and regions outside
 * have a winding number of 0.
 *
 * If keepOnlyBoundary is TRUE, it also deletes all edges which do not
 * separate an interior region from an exterior one.
 */
    fun __gl_meshSetWindingNumber(mesh: GLUmesh, value: Int, keepOnlyBoundary: Boolean): Boolean {
        var e: GLUhalfEdge
        var eNext: GLUhalfEdge
        e = mesh.eHead.next ?: error("mesh.eHead.next == null")
        while (e !== mesh.eHead) {
            eNext = e.next ?: error("e.next == null")
            if (e.Sym!!.Lface!!.inside != e.Lface!!.inside) {

                /* This is a boundary edge (one side is interior, one is exterior). */
                e.winding = if (e.Lface!!.inside) value else -value
            } else {

                /* Both regions are interior, or both are exterior. */
                if (!keepOnlyBoundary) {
                    e.winding = 0
                } else {
                    if (!Mesh.__gl_meshDelete(e)) return false
                }
            }
            e = eNext
        }
        return true
    }
}
