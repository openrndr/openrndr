@file:Suppress("FunctionName", "ConstantConditionIf", "NAME_SHADOWING", "UNCHECKED_CAST",
    "MemberVisibilityCanBePrivate", "RemoveEmptySecondaryConstructorBody", "ClassName"
)

package org.openrndr.ktessellation


internal object Render {
    private const val USE_OPTIMIZED_CODE_PATH = false
    private val renderFan = RenderFan()
    private val renderStrip = RenderStrip()
    private val renderTriangle = RenderTriangle()

    /************************ Strips and Fans decomposition  */ /* __gl_renderMesh( tess, mesh ) takes a mesh and breaks it into triangle
 * fans, strips, and separate triangles.  A substantial effort is made
 * to use as few rendering primitives as possible (ie. to make the fans
 * and strips as large as possible).
 *
 * The rendering output is provided as callbacks (see the api).
 */
    fun __gl_renderMesh(
        tess: GLUtessellatorImpl,
        mesh: GLUmesh
    ) {
        var f: GLUface

        /* Make a list of separate triangles so we can render them all at once */tess.lonelyTriList = null
        f = mesh.fHead.next!!
        while (f !== mesh.fHead) {
            f.marked = false
            f = f.next!!
        }
        f = mesh.fHead.next!!
        while (f !== mesh.fHead) {


            /* We examine all faces in an arbitrary order.  Whenever we find
             * an unprocessed face F, we output a group of faces including F
             * whose size is maximum.
             */
            if (f.inside && !f.marked) {
                RenderMaximumFaceGroup(tess, f)
                require(f.marked)
            }
            f = f.next!!
        }
        if (tess.lonelyTriList != null) {
            RenderLonelyTriangles(tess, tess.lonelyTriList)
            tess.lonelyTriList = null
        }
    }

    fun RenderMaximumFaceGroup(
        tess: GLUtessellatorImpl,
        fOrig: GLUface
    ) {
        /* We want to find the largest triangle fan or strip of unmarked faces
         * which includes the given face fOrig.  There are 3 possible fans
         * passing through fOrig (one centered at each vertex), and 3 possible
         * strips (one for each CCW permutation of the vertices).  Our strategy
         * is to try all of these, and take the primitive which uses the most
         * triangles (a greedy approach).
         */
        val e: GLUhalfEdge = fOrig.anEdge!!
        var max = FaceCount()
        var newFace: FaceCount
        max.size = 1
        max.eStart = e
        max.render = renderTriangle
        if (!tess.flagBoundary) {
            newFace = MaximumFan(e)
            if (newFace.size > max.size) {
                max = newFace
            }
            newFace = MaximumFan(e.Lnext!!)
            if (newFace.size > max.size) {
                max = newFace
            }
            newFace = MaximumFan(e.Onext!!.Sym!!)
            if (newFace.size > max.size) {
                max = newFace
            }
            newFace = MaximumStrip(e)
            if (newFace.size > max.size) {
                max = newFace
            }
            newFace = MaximumStrip(e.Lnext!!)
            if (newFace.size > max.size) {
                max = newFace
            }
            newFace = MaximumStrip(e.Onext!!.Sym!!)
            if (newFace.size > max.size) {
                max = newFace
            }
        }
        max.render!!.render(tess, max.eStart!!, max.size)
    }

    /* Macros which keep track of faces we have marked temporarily, and allow
 * us to backtrack when necessary.  With triangle fans, this is not
 * really necessary, since the only awkward case is a loop of triangles
 * around a single origin vertex.  However with strips the situation is
 * more complicated, and we need a general tracking method like the
 * one here.
 */
    private fun Marked(f: GLUface): Boolean {
        return !f.inside || f.marked
    }

    private fun AddToTrail(
        f: GLUface,
        t: GLUface?
    ): GLUface {
        f.trail = t
        f.marked = true
        return f
    }

    private fun FreeTrail(t: GLUface?) {
        var t: GLUface? = t
        if (true) {
            while (t != null) {
                t.marked = false
                t = t.trail
            }
        } else {
            /* absorb trailing semicolon */
        }
    }

    private fun MaximumFan(eOrig: GLUhalfEdge): FaceCount {
        /* eOrig.Lface is the face we want to render.  We want to find the size
         * of a maximal fan around eOrig.Org.  To do this we just walk around
         * the origin vertex as far as possible in both directions.
         */
        val newFace = FaceCount(0, null, renderFan)
        var trail: GLUface? = null
        var e: GLUhalfEdge
        e = eOrig
        while (!Marked(e.Lface!!)) {
            trail = AddToTrail(e.Lface!!, trail)
            ++newFace.size
            e = e.Onext!!
        }
        e = eOrig
        while (!Marked(e.Sym!!.Lface!!)) {
            trail = AddToTrail(e.Sym!!.Lface!!, trail)
            ++newFace.size
            e = e.Sym!!.Lnext!!
        }
        newFace.eStart = e
        FreeTrail(trail)
        return newFace
    }

    private fun IsEven(n: Long): Boolean {
        return n and 0x1L == 0L
    }

    private fun MaximumStrip(eOrig: GLUhalfEdge): FaceCount {
        /* Here we are looking for a maximal strip that contains the vertices
         * eOrig.Org, eOrig.Dst, eOrig.Lnext.Dst (in that order or the
         * reverse, such that all triangles are oriented CCW).
         *
         * Again we walk forward and backward as far as possible.  However for
         * strips there is a twist: to get CCW orientations, there must be
         * an *even* number of triangles in the strip on one side of eOrig.
         * We walk the strip starting on a side with an even number of triangles;
         * if both side have an odd number, we are forced to shorten one side.
         */
        val newFace = FaceCount(0, null, renderStrip)
        var headSize: Long = 0
        var tailSize: Long = 0
        var trail: GLUface? = null
        var e: GLUhalfEdge
        e = eOrig
        while (!Marked(e.Lface!!)) {
            trail = AddToTrail(e.Lface!!, trail)
            ++tailSize
            e = e.Lnext!!.Sym!!
            if (Marked(e.Lface!!)) break
            trail = AddToTrail(e.Lface!!, trail)
            ++tailSize
            e = e.Onext!!
        }
        val eTail: GLUhalfEdge = e
        e = eOrig
        while (!Marked(e.Sym!!.Lface!!)) {
            trail = AddToTrail(e.Sym!!.Lface!!, trail)
            ++headSize
            e = e.Sym!!.Lnext!!
            if (Marked(e.Sym!!.Lface!!)) break
            trail = AddToTrail(e.Sym!!.Lface!!, trail)
            ++headSize
            e = e.Sym!!.Onext!!.Sym!!
        }
        val eHead: GLUhalfEdge = e
        newFace.size = tailSize + headSize
        if (IsEven(tailSize)) {
            newFace.eStart = eTail.Sym
        } else if (IsEven(headSize)) {
            newFace.eStart = eHead
        } else {
            /* Both sides have odd length, we must shorten one of them.  In fact,
             * we must start from eHead to guarantee inclusion of eOrig.Lface.
             */
            --newFace.size
            newFace.eStart = eHead.Onext
        }
        /*LINTED*/FreeTrail(trail)
        return newFace
    }

    fun RenderLonelyTriangles(
        tess: GLUtessellatorImpl,
        f: GLUface?
    ) {
        /* Now we render all the separate triangles which could not be
         * grouped into a triangle fan or strip.
         */
        var f: GLUface? = f
        var e: GLUhalfEdge
        var newState: Int
        var edgeState = -1 /* force edge state output for first vertex */
        tess.callBeginOrBeginData(GLConstants.GL_TRIANGLES)
        while (f != null) {

            /* Loop once for each edge (there will always be 3 edges) */
            e = f.anEdge!!
            do {
                if (tess.flagBoundary) {
                    /* Set the "edge state" to true just before we output the
                     * first vertex of each edge on the polygon boundary.
                     */
                    newState = if (!e.Sym!!.Lface!!.inside) 1 else 0
                    if (edgeState != newState) {
                        edgeState = newState
                        tess.callEdgeFlagOrEdgeFlagData(edgeState != 0)
                    }
                }
                tess.callVertexOrVertexData(e.Org!!.data)
                e = e.Lnext!!
            } while (e !== f.anEdge)
            f = f.trail
        }
        tess.callEndOrEndData()
    }

    /************************ Boundary contour decomposition  */ /* __gl_renderBoundary( tess, mesh ) takes a mesh, and outputs one
 * contour for each face marked "inside".  The rendering output is
 * provided as callbacks (see the api).
 */
    fun __gl_renderBoundary(
        tess: GLUtessellatorImpl,
        mesh: GLUmesh
    ) {
        var f: GLUface
        var e: GLUhalfEdge
        f = mesh.fHead.next!!
        while (f !== mesh.fHead) {
            if (f.inside) {
                tess.callBeginOrBeginData(GLConstants.GL_LINE_LOOP)
                e = f.anEdge!!
                do {
                    tess.callVertexOrVertexData(e.Org!!.data)
                    e = e.Lnext!!
                } while (e !== f.anEdge)
                tess.callEndOrEndData()
            }
            f = f.next!!
        }
    }

    /************************ Quick-and-dirty decomposition  */
    private const val SIGN_INCONSISTENT = 2
    fun ComputeNormal(tess: GLUtessellatorImpl, norm: DoubleArray, check: Boolean): Int /*
 * If check==false, we compute the polygon normal and place it in norm[].
 * If check==true, we check that each triangle in the fan from v0 has a
 * consistent orientation with respect to norm[].  If triangles are
 * consistently oriented CCW, return 1; if CW, return -1; if all triangles
 * are degenerate return 0; otherwise (no consistent orientation) return
 * SIGN_INCONSISTENT.
 */ {
        val v: Array<CachedVertex> = tess.cache as Array<CachedVertex>
        //            CachedVertex vn = v0 + tess.cacheCount;
        val vn: Int = tess.cacheCount
        //            CachedVertex vc;
        var dot: Double
        var xc: Double
        var yc: Double
        var zc: Double
        var xp: Double
        var yp: Double
        var zp: Double
        val n = DoubleArray(3)
        var sign = 0

        /* Find the polygon normal.  It is important to get a reasonable
         * normal even when the polygon is self-intersecting (eg. a bowtie).
         * Otherwise, the computed normal could be very tiny, but perpendicular
         * to the true plane of the polygon due to numerical noise.  Then all
         * the triangles would appear to be degenerate and we would incorrectly
         * decompose the polygon as a fan (or simply not render it at all).
         *
         * We use a sum-of-triangles normal algorithm rather than the more
         * efficient sum-of-trapezoids method (used in CheckOrientation()
         * in normal.c).  This lets us explicitly reverse the signed area
         * of some triangles to get a reasonable normal in the self-intersecting
         * case.
         */if (!check) {
            norm[2] = 0.0
            norm[1] = norm[2]
            norm[0] = norm[1]
        }
        var vc = 1
        xc = v[vc].coords[0] - v[0].coords[0]
        yc = v[vc].coords[1] - v[0].coords[1]
        zc = v[vc].coords[2] - v[0].coords[2]
        while (++vc < vn) {
            xp = xc
            yp = yc
            zp = zc
            xc = v[vc].coords[0] - v[0].coords[0]
            yc = v[vc].coords[1] - v[0].coords[1]
            zc = v[vc].coords[2] - v[0].coords[2]

            /* Compute (vp - v0) cross (vc - v0) */n[0] = yp * zc - zp * yc
            n[1] = zp * xc - xp * zc
            n[2] = xp * yc - yp * xc
            dot = n[0] * norm[0] + n[1] * norm[1] + n[2] * norm[2]
            if (!check) {
                /* Reverse the contribution of back-facing triangles to get
                 * a reasonable normal for self-intersecting polygons (see above)
                 */
                if (dot >= 0) {
                    norm[0] += n[0]
                    norm[1] += n[1]
                    norm[2] += n[2]
                } else {
                    norm[0] -= n[0]
                    norm[1] -= n[1]
                    norm[2] -= n[2]
                }
            } else if (dot != 0.0) {
                /* Check the new orientation for consistency with previous triangles */
                sign = if (dot > 0) {
                    if (sign < 0) return SIGN_INCONSISTENT
                    1
                } else {
                    if (sign > 0) return SIGN_INCONSISTENT
                    -1
                }
            }
        }
        return sign
    }

    /* __gl_renderCache( tess ) takes a single contour and tries to render it
 * as a triangle fan.  This handles convex polygons, as well as some
 * non-convex polygons if we get lucky.
 *
 * Returns true if the polygon was successfully rendered.  The rendering
 * output is provided as callbacks (see the api).
 */
    fun __gl_renderCache(tess: GLUtessellatorImpl): Boolean {
        val v: Array<CachedVertex> = tess.cache as Array<CachedVertex>
        //            CachedVertex vn = v0 + tess.cacheCount;
        val vn: Int = tess.cacheCount
        //            CachedVertex vc;
        var vc: Int
        val norm = DoubleArray(3)
        if (tess.cacheCount < 3) {
            /* Degenerate contour -- no output */
            return true
        }
        norm[0] = tess.normal[0]
        norm[1] = tess.normal[1]
        norm[2] = tess.normal[2]
        if (norm[0] == 0.0 && norm[1] == 0.0 && norm[2] == 0.0) {
            ComputeNormal(tess, norm, false)
        }
        val sign: Int = ComputeNormal(tess, norm, true)
        if (sign == SIGN_INCONSISTENT) {
            /* Fan triangles did not have a consistent orientation */
            return false
        }
        if (sign == 0) {
            /* All triangles were degenerate */
            return true
        }
        return if (!USE_OPTIMIZED_CODE_PATH) {
            false
        } else {
            /* Make sure we do the right thing for each winding rule */
            when (tess.windingRule) {
                GLU.GLU_TESS_WINDING_ODD, GLU.GLU_TESS_WINDING_NONZERO -> {}
                GLU.GLU_TESS_WINDING_POSITIVE -> if (sign < 0) return true
                GLU.GLU_TESS_WINDING_NEGATIVE -> if (sign > 0) return true
                GLU.GLU_TESS_WINDING_ABS_GEQ_TWO -> return true
            }
            tess.callBeginOrBeginData(if (tess.boundaryOnly) GLConstants.GL_LINE_LOOP else if (tess.cacheCount > 3) GLConstants.GL_TRIANGLE_FAN else GLConstants.GL_TRIANGLES)
            tess.callVertexOrVertexData(v[0].data)
            if (sign > 0) {
                vc = 1
                while (vc < vn) {
                    tess.callVertexOrVertexData(v[vc].data)
                    ++vc
                }
            } else {
                vc = vn - 1
                while (vc > 0) {
                    tess.callVertexOrVertexData(v[vc].data)
                    --vc
                }
            }
            tess.callEndOrEndData()
            true
        }
    }

    /* This structure remembers the information we need about a primitive
 * to be able to render it later, once we have determined which
 * primitive is able to use the most triangles.
 */
    private class FaceCount {
        constructor() {}
        constructor(size: Long, eStart: GLUhalfEdge?, render: renderCallBack) {
            this.size = size
            this.eStart = eStart
            this.render = render
        }

        var size /* number of triangles used */: Long = 0
        var eStart /* edge where this primitive starts */: GLUhalfEdge? = null
        var render: renderCallBack? = null
    }

    private interface renderCallBack {
        fun render(
            tess: GLUtessellatorImpl,
            e: GLUhalfEdge,
            size: Long
        )
    }

    private class RenderTriangle : renderCallBack {
        override fun render(
            tess: GLUtessellatorImpl,
            e: GLUhalfEdge,
            size: Long
        ) {
            /* Just add the triangle to a triangle list, so we can render all
             * the separate triangles at once.
             */
            require(size == 1L)
            tess.lonelyTriList = AddToTrail(e.Lface!!, tess.lonelyTriList)
        }
    }

    private class RenderFan : renderCallBack {
        override fun render(
            tess: GLUtessellatorImpl,
            e: GLUhalfEdge,
            size: Long
        ) {
            /* Render as many CCW triangles as possible in a fan starting from
             * edge "e".  The fan *should* contain exactly "size" triangles
             * (otherwise we've goofed up somewhere).
             */
            var e: GLUhalfEdge = e
            var size = size
            tess.callBeginOrBeginData(GLConstants.GL_TRIANGLE_FAN)
            tess.callVertexOrVertexData(e.Org!!.data)
            tess.callVertexOrVertexData(e.Sym!!.Org!!.data)
            while (!Marked(e.Lface!!)) {
                e.Lface!!.marked = true
                --size
                e = e.Onext!!
                tess.callVertexOrVertexData(e.Sym!!.Org!!.data)
            }
            require(size == 0L)
            tess.callEndOrEndData()
        }
    }

    private class RenderStrip : renderCallBack {
        override fun render(
            tess: GLUtessellatorImpl,
            e: GLUhalfEdge,
            size: Long
        ) {
            /* Render as many CCW triangles as possible in a strip starting from
             * edge "e".  The strip *should* contain exactly "size" triangles
             * (otherwise we've goofed up somewhere).
             */
            var e: GLUhalfEdge = e
            var size = size
            tess.callBeginOrBeginData(GLConstants.GL_TRIANGLE_STRIP)
            tess.callVertexOrVertexData(e.Org!!.data)
            tess.callVertexOrVertexData(e.Sym!!.Org!!.data)
            while (!Marked(e.Lface!!)) {
                e.Lface!!.marked = true
                --size
                e = e.Lnext!!.Sym!!
                tess.callVertexOrVertexData(e.Org!!.data)
                if (Marked(e.Lface!!)) break
                e.Lface!!.marked = true
                --size
                e = e.Onext!!
                tess.callVertexOrVertexData(e.Sym!!.Org!!.data)
            }
            require(size == 0L)
            tess.callEndOrEndData()
        }
    }
}
