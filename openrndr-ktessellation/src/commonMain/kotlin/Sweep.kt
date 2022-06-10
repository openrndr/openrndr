@file:Suppress(
    "FunctionName", "UNUSED_PARAMETER", "MemberVisibilityCanBePrivate", "MemberVisibilityCanBePrivate",
    "SENSELESS_COMPARISON", "NAME_SHADOWING", "SpellCheckingInspection", "RedundantNullableReturnType",
    "UNNECESSARY_NOT_NULL_ASSERTION", "unused", "FoldInitializerAndIfToElvis"
)

package org.openrndr.ktessellation

import kotlin.math.max
import kotlin.math.min

internal object Sweep {
    //    #ifdef FOR_TRITE_TEST_PROGRAM
    //    extern void DebugEvent( GLUtessellator *tess );
    //    #else
    private fun DebugEvent(tess: GLUtessellatorImpl) {}

    //    #endif
    /*
 * Invariants for the Edge Dictionary.
 * - each pair of adjacent edges e2=Succ(e1) satisfies EdgeLeq(e1,e2)
 *   at any valid location of the sweep event
 * - if EdgeLeq(e2,e1) as well (at any valid sweep event), then e1 and e2
 *   share a common endpoint
 * - for each e, e.Dst has been processed, but not e.Org
 * - each edge e satisfies VertLeq(e.Dst,event) && VertLeq(event,e.Org)
 *   where "event" is the current sweep line event.
 * - no edge e has zero length
 *
 * Invariants for the Mesh (the processed portion).
 * - the portion of the mesh left of the sweep line is a planar graph,
 *   ie. there is *some* way to embed it in the plane
 * - no processed edge has zero length
 * - no two processed vertices have identical coordinates
 * - each "inside" region is monotone, ie. can be broken into two chains
 *   of monotonically increasing vertices according to VertLeq(v1,v2)
 *   - a non-invariant: these chains may intersect (very slightly)
 *
 * Invariants for the Sweep.
 * - if none of the edges incident to the event vertex have an activeRegion
 *   (ie. none of these edges are in the edge dictionary), then the vertex
 *   has only right-going edges.
 * - if an edge is marked "fixUpperEdge" (it is a temporary edge introduced
 *   by ConnectRightVertex), then it is the only right-going edge from
 *   its associated vertex.  (This says that these edges exist only
 *   when it is necessary.)
 */
    /* When we merge two edges into one, we need to compute the combined
 * winding of the new edge.
 */
    private fun AddWinding(
        eDst: GLUhalfEdge,
        eSrc: GLUhalfEdge
    ) {
        eDst.winding += eSrc.winding
        eDst.Sym!!.winding += eSrc.Sym!!.winding
    }

    private fun RegionBelow(r: ActiveRegion): ActiveRegion? {
        return Dict.dictKey(Dict.dictPred(r.nodeUp!!)!!) as ActiveRegion?
    }

    private fun RegionAbove(r: ActiveRegion): ActiveRegion? {
        return Dict.dictKey(Dict.dictSucc(r.nodeUp!!)!!) as ActiveRegion?
    }

    fun EdgeLeq(
        tess: GLUtessellatorImpl,
        reg1: ActiveRegion,
        reg2: ActiveRegion
    ): Boolean /*
 * Both edges must be directed from right to left (this is the canonical
 * direction for the upper edge of each region).
 *
 * The strategy is to evaluate a "t" value for each edge at the
 * current sweep line position, given by tess.event.  The calculations
 * are designed to be very stable, but of course they are not perfect.
 *
 * Special case: if both edge destinations are at the sweep event,
 * we sort the edges by slope (they would otherwise compare equally).
 */ {
        val event: GLUvertex = tess.event!!
        val t1: Double
        val t2: Double
        val e1: GLUhalfEdge = reg1.eUp!!
        val e2: GLUhalfEdge = reg2.eUp!!
        if (e1.Sym!!.Org === event) {
            return if (e2.Sym!!.Org === event) {
                /* Two edges right of the sweep line which meet at the sweep event.
                         * Sort them by slope.
                         */
                if (Geom.VertLeq(e1.Org!!, e2.Org!!)) {
                    Geom.EdgeSign(e2.Sym!!.Org!!, e1.Org!!, e2.Org!!) <= 0
                } else Geom.EdgeSign(e1.Sym!!.Org!!, e2.Org!!, e1.Org!!) >= 0
            } else Geom.EdgeSign(e2.Sym!!.Org!!, event, e2.Org!!) <= 0
        }
        if (e2.Sym!!.Org === event) {
            return Geom.EdgeSign(e1.Sym!!.Org!!, event, e1.Org!!) >= 0
        }

        /* General case - compute signed distance *from* e1, e2 to event */t1 =
            Geom.EdgeEval(e1.Sym!!.Org!!, event, e1.Org!!)
        t2 = Geom.EdgeEval(e2.Sym!!.Org!!, event, e2.Org!!)
        return t1 >= t2
    }

    fun DeleteRegion(
        tess: GLUtessellatorImpl,
        reg: ActiveRegion?
    ) {
        if (reg!!.fixUpperEdge) {
            /* It was created with zero winding number, so it better be
             * deleted with zero winding number (ie. it better not get merged
             * with a real edge).
             */
            require(reg.eUp!!.winding == 0)
        }
        reg.eUp!!.activeRegion = null
        Dict.dictDelete(tess.dict, reg.nodeUp!!) /* __gl_dictListDelete */
    }

    fun FixUpperEdge(
        reg: ActiveRegion,
        newEdge: GLUhalfEdge?
    ): Boolean /*
 * Replace an upper edge which needs fixing (see ConnectRightVertex).
 */ {
        require(reg.fixUpperEdge)
        if (!Mesh.__gl_meshDelete(reg.eUp!!)) return false
        reg.fixUpperEdge = false
        reg.eUp = newEdge
        newEdge!!.activeRegion = reg
        return true
    }

    fun TopLeftRegion(reg: ActiveRegion): ActiveRegion? {
        var reg: ActiveRegion = reg
        val org: GLUvertex = reg.eUp!!.Org!!
        val e: GLUhalfEdge

        /* Find the region above the uppermost edge with the same origin */do {
            reg = RegionAbove(reg)!!
        } while (reg.eUp!!.Org === org)

        /* If the edge above was a temporary edge introduced by ConnectRightVertex,
         * now is the time to fix it.
         */if (reg.fixUpperEdge) {
            e = Mesh.__gl_meshConnect(RegionBelow(reg)!!.eUp!!.Sym!!, reg.eUp!!.Lnext!!)
            if (e == null) return null
            if (!FixUpperEdge(reg, e)) return null
            reg = RegionAbove(reg)!!
        }
        return reg
    }

    fun TopRightRegion(reg: ActiveRegion): ActiveRegion {
        var reg: ActiveRegion = reg
        val dst: GLUvertex = reg.eUp!!.Sym!!.Org!!

        /* Find the region above the uppermost edge with the same destination */do {
            reg = RegionAbove(reg)!!
        } while (reg.eUp!!.Sym!!.Org!! === dst)
        return reg
    }

    fun AddRegionBelow(
        tess: GLUtessellatorImpl,
        regAbove: ActiveRegion?,
        eNewUp: GLUhalfEdge?
    ): ActiveRegion? /*
 * Add a new active region to the sweep line, *somewhere* below "regAbove"
 * (according to where the new edge belongs in the sweep-line dictionary).
 * The upper edge of the new region will be "eNewUp".
 * Winding number and "inside" flag are not updated.
 */ {
        val regNew = ActiveRegion()
        regNew.eUp = eNewUp
        /* __gl_dictListInsertBefore */regNew.nodeUp =
            Dict.dictInsertBefore(tess.dict!!, regAbove!!.nodeUp!!, regNew)
        if (regNew.nodeUp == null) throw RuntimeException()
        regNew.fixUpperEdge = false
        regNew.sentinel = false
        regNew.dirty = false
        eNewUp!!.activeRegion = regNew
        return regNew
    }

    fun IsWindingInside(tess: GLUtessellatorImpl, n: Int): Boolean {
        return when (tess.windingRule) {
            GLU.GLU_TESS_WINDING_ODD -> n and 1 != 0
            GLU.GLU_TESS_WINDING_NONZERO -> n != 0
            GLU.GLU_TESS_WINDING_POSITIVE -> n > 0
            GLU.GLU_TESS_WINDING_NEGATIVE -> n < 0
            GLU.GLU_TESS_WINDING_ABS_GEQ_TWO -> n >= 2 || n <= -2
            else -> error("no such winding rule")
        }

    }

    fun ComputeWinding(
        tess: GLUtessellatorImpl,
        reg: ActiveRegion
    ) {
        reg.windingNumber = RegionAbove(reg)!!.windingNumber + reg.eUp!!.winding
        reg.inside = IsWindingInside(tess, reg.windingNumber)
    }

    fun FinishRegion(
        tess: GLUtessellatorImpl,
        reg: ActiveRegion
    ) /*
 * Delete a region from the sweep line.  This happens when the upper
 * and lower chains of a region meet (at a vertex on the sweep line).
 * The "inside" flag is copied to the appropriate mesh face (we could
 * not do this before -- since the structure of the mesh is always
 * changing, this face may not have even existed until now).
 */ {
        val e: GLUhalfEdge = reg.eUp!!
        val f: GLUface = e.Lface!!
        f.inside = reg.inside
        f.anEdge = e /* optimization for __gl_meshTessellateMonoRegion() */
        DeleteRegion(tess, reg)
    }

    fun FinishLeftRegions(
        tess: GLUtessellatorImpl,
        regFirst: ActiveRegion, regLast: ActiveRegion?
    ): GLUhalfEdge /*
 * We are given a vertex with one or more left-going edges.  All affected
 * edges should be in the edge dictionary.  Starting at regFirst.eUp,
 * we walk down deleting all regions where both edges have the same
 * origin vOrg.  At the same time we copy the "inside" flag from the
 * active region to the face, since at this point each face will belong
 * to at most one region (this was not necessarily true until this point
 * in the sweep).  The walk stops at the region above regLast; if regLast
 * is null we walk as far as possible.  At the same time we relink the
 * mesh if necessary, so that the ordering of edges around vOrg is the
 * same as in the dictionary.
 */ {
        var reg: ActiveRegion
        var regPrev: ActiveRegion?
        var e: GLUhalfEdge
        var ePrev: GLUhalfEdge
        regPrev = regFirst
        ePrev = regFirst.eUp!!
        while (regPrev !== regLast) {
            regPrev!!.fixUpperEdge = false /* placement was OK */
            reg = RegionBelow(regPrev)!!
            e = reg.eUp!!
            if (e.Org !== ePrev.Org) {
                if (!reg.fixUpperEdge) {
                    /* Remove the last left-going edge.  Even though there are no further
                     * edges in the dictionary with this origin, there may be further
                     * such edges in the mesh (if we are adding left edges to a vertex
                     * that has already been processed).  Thus it is important to call
                     * FinishRegion rather than just DeleteRegion.
                     */
                    FinishRegion(tess, regPrev)
                    break
                }
                /* If the edge below was a temporary edge introduced by
                 * ConnectRightVertex, now is the time to fix it.
                 */
                e = Mesh.__gl_meshConnect(ePrev.Onext!!.Sym!!, e.Sym!!)
                if (e == null) throw RuntimeException()
                if (!FixUpperEdge(reg, e)) throw RuntimeException()
            }

            /* Relink edges so that ePrev.Onext == e */
            if (ePrev.Onext !== e) {
                if (!Mesh.__gl_meshSplice(e.Sym!!.Lnext!!, e)) throw RuntimeException()
                if (!Mesh.__gl_meshSplice(ePrev, e)) throw RuntimeException()
            }
            FinishRegion(tess, regPrev) /* may change reg.eUp */
            ePrev = reg.eUp!!
            regPrev = reg
        }
        return ePrev
    }

    fun AddRightEdges(
        tess: GLUtessellatorImpl,
        regUp: ActiveRegion?,
        eFirst: GLUhalfEdge,
        eLast: GLUhalfEdge,
        eTopLeft: GLUhalfEdge?,
        cleanUp: Boolean
    ) /*
 * Purpose: insert right-going edges into the edge dictionary, and update
 * winding numbers and mesh connectivity appropriately.  All right-going
 * edges share a common origin vOrg.  Edges are inserted CCW starting at
 * eFirst; the last edge inserted is eLast.Sym.Lnext.  If vOrg has any
 * left-going edges already processed, then eTopLeft must be the edge
 * such that an imaginary upward vertical segment from vOrg would be
 * contained between eTopLeft.Sym.Lnext and eTopLeft; otherwise eTopLeft
 * should be null.
 */ {
        var eTopLeft: GLUhalfEdge? = eTopLeft
        var reg: ActiveRegion
        var regPrev: ActiveRegion?
        var e: GLUhalfEdge
        var ePrev: GLUhalfEdge?
        var firstTime = true

        /* Insert the new right-going edges in the dictionary */e = eFirst
        do {
            require(Geom.VertLeq(e.Org!!, e.Sym!!.Org!!))
            AddRegionBelow(tess, regUp, e.Sym)
            e = e.Onext!!
        } while (e !== eLast)

        /* Walk *all* right-going edges from e.Org, in the dictionary order,
         * updating the winding numbers of each region, and re-linking the mesh
         * edges to match the dictionary ordering (if necessary).
         */if (eTopLeft == null) {
            eTopLeft = RegionBelow(regUp!!)!!.eUp!!.Sym!!.Onext
        }
        regPrev = regUp
        ePrev = eTopLeft
        while (true) {
            reg = RegionBelow(regPrev!!)!!
            e = reg.eUp!!.Sym!!
            if (e.Org !== ePrev!!.Org) break
            if (e.Onext !== ePrev) {
                /* Unlink e from its current position, and relink below ePrev */
                if (!Mesh.__gl_meshSplice(e.Sym!!.Lnext!!, e)) throw RuntimeException()
                if (!Mesh.__gl_meshSplice(ePrev!!.Sym!!.Lnext!!, e)) throw RuntimeException()
            }
            /* Compute the winding number and "inside" flag for the new regions */reg.windingNumber =
                regPrev.windingNumber - e.winding
            reg.inside = IsWindingInside(tess, reg.windingNumber)

            /* Check for two outgoing edges with same slope -- process these
             * before any intersection tests (see example in __gl_computeInterior).
             */regPrev.dirty = true
            if (!firstTime && CheckForRightSplice(tess, regPrev)) {
                AddWinding(e, ePrev!!)
                DeleteRegion(tess, regPrev)
                if (!Mesh.__gl_meshDelete(ePrev)) throw RuntimeException()
            }
            firstTime = false
            regPrev = reg
            ePrev = e
        }
        regPrev!!.dirty = true
        require(regPrev.windingNumber - e.winding == reg.windingNumber)
        if (cleanUp) {
            /* Check for intersections between newly adjacent edges. */
            WalkDirtyRegions(tess, regPrev)
        }
    }

    fun CallCombine(
        tess: GLUtessellatorImpl, isect: GLUvertex,
        data: Array<Any?>?, weights: FloatArray?, needed: Boolean
    ) {
        val coords = DoubleArray(3)

        /* Copy coord data in case the callback changes it. */coords[0] = isect.coords[0]
        coords[1] = isect.coords[1]
        coords[2] = isect.coords[2]
        val outData = arrayOfNulls<Any>(1)
        tess.callCombineOrCombineData(coords, data, weights, outData)
        isect.data = outData[0]
        if (isect.data == null) {
            if (!needed) {
                isect.data = data!![0]
            } else if (!tess.fatalError) {
                /* The only way fatal error is when two edges are found to intersect,
                 * but the user has not provided the callback necessary to handle
                 * generated intersection points.
                 */
                tess.callErrorOrErrorData(GLU.GLU_TESS_NEED_COMBINE_CALLBACK)
                tess.fatalError = true
            }
        }
    }

    fun SpliceMergeVertices(
        tess: GLUtessellatorImpl, e1: GLUhalfEdge,
        e2: GLUhalfEdge
    ) /*
 * Two vertices with idential coordinates are combined into one.
 * e1.Org is kept, while e2.Org is discarded.
 */ {
        val data = arrayOfNulls<Any>(4)
        val weights = floatArrayOf(0.5f, 0.5f, 0.0f, 0.0f)
        data[0] = e1.Org!!.data
        data[1] = e2.Org!!.data
        CallCombine(tess, e1.Org!!, data, weights, false)
        if (!Mesh.__gl_meshSplice(e1, e2)) throw RuntimeException()
    }

    fun VertexWeights(
        isect: GLUvertex,
        org: GLUvertex,
        dst: GLUvertex,
        weights: FloatArray
    ) /*
 * Find some weights which describe how the intersection vertex is
 * a linear combination of "org" and "dest".  Each of the two edges
 * which generated "isect" is allocated 50% of the weight; each edge
 * splits the weight between its org and dst according to the
 * relative distance to "isect".
 */ {
        val t1: Double = Geom.VertL1dist(org, isect)
        val t2: Double = Geom.VertL1dist(dst, isect)
        weights[0] = (0.5 * t2 / (t1 + t2)).toFloat()
        weights[1] = (0.5 * t1 / (t1 + t2)).toFloat()
        isect.coords[0] += weights[0] * org.coords[0] + weights[1] * dst.coords[0]
        isect.coords[1] += weights[0] * org.coords[1] + weights[1] * dst.coords[1]
        isect.coords[2] += weights[0] * org.coords[2] + weights[1] * dst.coords[2]
    }

    fun GetIntersectData(
        tess: GLUtessellatorImpl, isect: GLUvertex,
        orgUp: GLUvertex, dstUp: GLUvertex,
        orgLo: GLUvertex, dstLo: GLUvertex
    ) /*
 * We've computed a new intersection point, now we need a "data" pointer
 * from the user so that we can refer to this new vertex in the
 * rendering callbacks.
 */ {
        val data = arrayOfNulls<Any>(4)
        val weights = FloatArray(4)
        val weights1 = FloatArray(2)
        val weights2 = FloatArray(2)
        data[0] = orgUp.data
        data[1] = dstUp.data
        data[2] = orgLo.data
        data[3] = dstLo.data
        isect.coords[2] = 0.0
        isect.coords[1] = isect.coords[2]
        isect.coords[0] = isect.coords[1]
        VertexWeights(isect, orgUp, dstUp, weights1)
        VertexWeights(isect, orgLo, dstLo, weights2)
        arraycopy(weights1, 0, weights, 0, 2)
        arraycopy(weights2, 0, weights, 2, 2)
        CallCombine(tess, isect, data, weights, true)
    }

    fun CheckForRightSplice(
        tess: GLUtessellatorImpl,
        regUp: ActiveRegion
    ): Boolean /*
 * Check the upper and lower edge of "regUp", to make sure that the
 * eUp.Org is above eLo, or eLo.Org is below eUp (depending on which
 * origin is leftmost).
 *
 * The main purpose is to splice right-going edges with the same
 * dest vertex and nearly identical slopes (ie. we can't distinguish
 * the slopes numerically).  However the splicing can also help us
 * to recover from numerical errors.  For example, suppose at one
 * point we checked eUp and eLo, and decided that eUp.Org is barely
 * above eLo.  Then later, we split eLo into two edges (eg. from
 * a splice operation like this one).  This can change the result of
 * our test so that now eUp.Org is incident to eLo, or barely below it.
 * We must correct this condition to maintain the dictionary invariants.
 *
 * One possibility is to check these edges for intersection again
 * (ie. CheckForIntersect).  This is what we do if possible.  However
 * CheckForIntersect requires that tess.event lies between eUp and eLo,
 * so that it has something to fall back on when the intersection
 * calculation gives us an unusable answer.  So, for those cases where
 * we can't check for intersection, this routine fixes the problem
 * by just splicing the offending vertex into the other edge.
 * This is a guaranteed solution, no matter how degenerate things get.
 * Basically this is a combinatorial solution to a numerical problem.
 */ {
        val regLo: ActiveRegion = RegionBelow(regUp)!!
        val eUp: GLUhalfEdge = regUp.eUp!!
        val eLo: GLUhalfEdge = regLo.eUp!!
        if (Geom.VertLeq(eUp.Org!!, eLo.Org!!)) {
            if (Geom.EdgeSign(eLo.Sym!!.Org!!, eUp.Org!!, eLo.Org!!) > 0) return false

            /* eUp.Org appears to be below eLo */if (!Geom.VertEq(eUp.Org!!, eLo.Org!!)) {
                /* Splice eUp.Org into eLo */
                if (Mesh.__gl_meshSplitEdge(eLo.Sym!!) == null) throw RuntimeException()
                if (!Mesh.__gl_meshSplice(eUp, eLo.Sym!!.Lnext!!)) throw RuntimeException()
                regLo.dirty = true
                regUp.dirty = regLo.dirty
            } else if (eUp.Org !== eLo.Org) {
                /* merge the two vertices, discarding eUp.Org */
                tess.pq!!.pqDelete(eUp.Org!!.pqHandle) /* __gl_pqSortDelete */
                SpliceMergeVertices(tess, eLo.Sym!!.Lnext!!, eUp)
            }
        } else {
            if (Geom.EdgeSign(eUp.Sym!!.Org!!, eLo.Org!!, eUp.Org!!) < 0) return false

            /* eLo.Org appears to be above eUp, so splice eLo.Org into eUp */regUp.dirty = true
            RegionAbove(regUp)!!.dirty = regUp.dirty
            if (Mesh.__gl_meshSplitEdge(eUp.Sym!!) == null) throw RuntimeException()
            if (!Mesh.__gl_meshSplice(eLo.Sym!!.Lnext!!, eUp)) throw RuntimeException()
        }
        return true
    }

    fun CheckForLeftSplice(
        tess: GLUtessellatorImpl?,
        regUp: ActiveRegion?
    ): Boolean /*
 * Check the upper and lower edge of "regUp", to make sure that the
 * eUp.Sym.Org is above eLo, or eLo.Sym.Org is below eUp (depending on which
 * destination is rightmost).
 *
 * Theoretically, this should always be true.  However, splitting an edge
 * into two pieces can change the results of previous tests.  For example,
 * suppose at one point we checked eUp and eLo, and decided that eUp.Sym.Org
 * is barely above eLo.  Then later, we split eLo into two edges (eg. from
 * a splice operation like this one).  This can change the result of
 * the test so that now eUp.Sym.Org is incident to eLo, or barely below it.
 * We must correct this condition to maintain the dictionary invariants
 * (otherwise new edges might get inserted in the wrong place in the
 * dictionary, and bad stuff will happen).
 *
 * We fix the problem by just splicing the offending vertex into the
 * other edge.
 */ {
        val regLo: ActiveRegion = RegionBelow(regUp!!)!!
        val eUp: GLUhalfEdge = regUp.eUp!!
        val eLo: GLUhalfEdge = regLo.eUp!!
        val e: GLUhalfEdge
        require(!Geom.VertEq(eUp.Sym!!.Org!!, eLo.Sym!!.Org!!))
        if (Geom.VertLeq(eUp.Sym!!.Org!!, eLo.Sym!!.Org!!)) {
            if (Geom.EdgeSign(eUp.Sym!!.Org!!, eLo.Sym!!.Org!!, eUp.Org!!) < 0) return false

            /* eLo.Sym.Org is above eUp, so splice eLo.Sym.Org into eUp */regUp.dirty = true
            RegionAbove(regUp!!)!!.dirty = regUp!!.dirty
            e = Mesh.__gl_meshSplitEdge(eUp)
            if (e == null) throw RuntimeException()
            if (!Mesh.__gl_meshSplice(eLo.Sym!!, e)) throw RuntimeException()
            e.Lface!!.inside = regUp.inside
        } else {
            if (Geom.EdgeSign(eLo.Sym!!.Org!!, eUp.Sym!!.Org!!, eLo.Org!!) > 0) return false

            /* eUp.Sym.Org is below eLo, so splice eUp.Sym.Org into eLo */regLo.dirty = true
            regUp.dirty = regLo.dirty
            e = Mesh.__gl_meshSplitEdge(eLo)
            if (e == null) throw RuntimeException()
            if (!Mesh.__gl_meshSplice(eUp.Lnext!!, eLo.Sym!!)) throw RuntimeException()
            e.Sym!!.Lface!!.inside = regUp.inside
        }
        return true
    }

    fun CheckForIntersect(
        tess: GLUtessellatorImpl,
        regUp: ActiveRegion
    ): Boolean /*
 * Check the upper and lower edges of the given region to see if
 * they intersect.  If so, create the intersection and add it
 * to the data structures.
 *
 * Returns true if adding the new intersection resulted in a recursive
 * call to AddRightEdges(); in this case all "dirty" regions have been
 * checked for intersections, and possibly regUp has been deleted.
 */ {
        var regUp: ActiveRegion = regUp
        var regLo: ActiveRegion = RegionBelow(regUp!!)!!
        var eUp: GLUhalfEdge = regUp.eUp!!
        var eLo: GLUhalfEdge = regLo.eUp!!
        val orgUp: GLUvertex = eUp.Org!!
        val orgLo: GLUvertex = eLo.Org!!
        val dstUp: GLUvertex = eUp.Sym!!.Org!!
        val dstLo: GLUvertex = eLo.Sym!!.Org!!
        val isect = GLUvertex()
        val e: GLUhalfEdge
        require(!Geom.VertEq(dstLo, dstUp))
        require(Geom.EdgeSign(dstUp, tess.event!!, orgUp) <= 0)
        require(Geom.EdgeSign(dstLo, tess.event!!, orgLo) >= 0)
        require(orgUp !== tess.event && orgLo !== tess.event)
        require(!regUp.fixUpperEdge && !regLo.fixUpperEdge)
        if (orgUp === orgLo) return false /* right endpoints are the same */
        val tMinUp: Double = min(orgUp.t, dstUp.t)
        val tMaxLo: Double = max(orgLo.t, dstLo.t)
        if (tMinUp > tMaxLo) return false /* t ranges do not overlap */
        if (Geom.VertLeq(orgUp, orgLo)) {
            if (Geom.EdgeSign(dstLo, orgUp, orgLo) > 0) return false
        } else {
            if (Geom.EdgeSign(dstUp, orgLo, orgUp) < 0) return false
        }

        /* At this point the edges intersect, at least marginally */DebugEvent(tess)
        Geom.EdgeIntersect(dstUp, orgUp, dstLo, orgLo, isect)
        require(min(orgUp.t, dstUp.t) <= isect.t)
        require(isect.t <= max(orgLo.t, dstLo.t))
        require(min(dstLo.s, dstUp.s) <= isect.s)
        require(isect.s <= max(orgLo.s, orgUp.s))
        if (Geom.VertLeq(isect, tess.event!!)) {
            /* The intersection point lies slightly to the left of the sweep line,
             * so move it until it''s slightly to the right of the sweep line.
             * (If we had perfect numerical precision, this would never happen
             * in the first place).  The easiest and safest thing to do is
             * replace the intersection by tess.event.
             */
            isect.s = tess.event!!.s
            isect.t = tess.event!!.t
        }
        /* Similarly, if the computed intersection lies to the right of the
         * rightmost origin (which should rarely happen), it can cause
         * unbelievable inefficiency on sufficiently degenerate inputs.
         * (If you have the test program, try running test54.d with the
         * "X zoom" option turned on).
         */
        val orgMin: GLUvertex = if (Geom.VertLeq(orgUp, orgLo)) orgUp else orgLo
        if (Geom.VertLeq(orgMin, isect)) {
            isect.s = orgMin.s
            isect.t = orgMin.t
        }
        if (Geom.VertEq(isect, orgUp) || Geom.VertEq(
                isect,
                orgLo
            )
        ) {
            /* Easy case -- intersection at one of the right endpoints */
            CheckForRightSplice(tess, regUp)
            return false
        }
        if ((!Geom.VertEq(dstUp, tess.event!!)
                    && Geom.EdgeSign(dstUp, tess.event!!, isect) >= 0)
            || (!Geom.VertEq(dstLo, tess.event!!)
                    && Geom.EdgeSign(dstLo, tess.event!!, isect) <= 0)
        ) {
            /* Very unusual -- the new upper or lower edge would pass on the
             * wrong side of the sweep event, or through it.  This can happen
             * due to very small numerical errors in the intersection calculation.
             */
            if (dstLo === tess.event) {
                /* Splice dstLo into eUp, and process the new region(s) */
                if (Mesh.__gl_meshSplitEdge(eUp.Sym!!) == null) throw RuntimeException()
                if (!Mesh.__gl_meshSplice(eLo.Sym!!, eUp)) throw RuntimeException()
                regUp = TopLeftRegion(regUp)!!
                if (regUp == null) throw RuntimeException()
                eUp = RegionBelow(regUp)!!.eUp!!
                FinishLeftRegions(tess, RegionBelow(regUp)!!, regLo)
                AddRightEdges(tess, regUp, eUp.Sym!!.Lnext!!, eUp, eUp, true)
                return true
            }
            if (dstUp === tess.event) {
                /* Splice dstUp into eLo, and process the new region(s) */
                if (Mesh.__gl_meshSplitEdge(eLo.Sym!!) == null) throw RuntimeException()
                if (!Mesh.__gl_meshSplice(
                        eUp.Lnext!!,
                        eLo.Sym!!.Lnext!!
                    )
                ) throw RuntimeException()
                regLo = regUp
                regUp = TopRightRegion(regUp)
                e = RegionBelow(regUp)!!.eUp!!.Sym!!.Onext!!
                regLo.eUp = eLo.Sym!!.Lnext!!
                eLo = FinishLeftRegions(tess, regLo, null)
                AddRightEdges(tess, regUp, eLo.Onext!!, eUp.Sym!!.Onext!!, e, true)
                return true
            }
            /* Special case: called from ConnectRightVertex.  If either
             * edge passes on the wrong side of tess.event, split it
             * (and wait for ConnectRightVertex to splice it appropriately).
             */if (Geom.EdgeSign(dstUp, tess.event!!, isect) >= 0) {
                regUp.dirty = true
                RegionAbove(regUp)!!.dirty = regUp.dirty
                if (Mesh.__gl_meshSplitEdge(eUp.Sym!!) == null) throw RuntimeException()
                eUp.Org!!.s = tess.event!!.s
                eUp.Org!!.t = tess.event!!.t
            }
            if (Geom.EdgeSign(dstLo, tess.event!!, isect) <= 0) {
                regLo.dirty = true
                regUp.dirty = regLo.dirty
                if (Mesh.__gl_meshSplitEdge(eLo.Sym!!) == null) throw RuntimeException()
                eLo.Org!!.s = tess.event!!.s
                eLo.Org!!.t = tess.event!!.t
            }
            /* leave the rest for ConnectRightVertex */return false
        }

        /* General case -- split both edges, splice into new vertex.
         * When we do the splice operation, the order of the arguments is
         * arbitrary as far as correctness goes.  However, when the operation
         * creates a new face, the work done is proportional to the size of
         * the new face.  We expect the faces in the processed part of
         * the mesh (ie. eUp.Lface) to be smaller than the faces in the
         * unprocessed original contours (which will be eLo.Sym.Lnext.Lface).
         */if (Mesh.__gl_meshSplitEdge(eUp.Sym!!) == null) throw RuntimeException()
        if (Mesh.__gl_meshSplitEdge(eLo.Sym!!) == null) throw RuntimeException()
        if (!Mesh.__gl_meshSplice(eLo.Sym!!.Lnext!!, eUp)) throw RuntimeException()
        eUp.Org!!.s = isect.s
        eUp.Org!!.t = isect.t
        eUp.Org!!.pqHandle = tess.pq!!.pqInsert(eUp.Org) /* __gl_pqSortInsert */
        if (eUp.Org!!.pqHandle.toLong() == Long.MAX_VALUE) {
            tess.pq!!.pqDeletePriorityQ() /* __gl_pqSortDeletePriorityQ */
            tess.pq = null
            throw RuntimeException()
        }
        GetIntersectData(tess, eUp.Org!!, orgUp, dstUp, orgLo, dstLo)
        regLo.dirty = true
        regUp.dirty = regLo.dirty
        RegionAbove(regUp)!!.dirty = regUp.dirty
        return false
    }

    fun WalkDirtyRegions(
        tess: GLUtessellatorImpl,
        regUp: ActiveRegion
    ) /*
 * When the upper or lower edge of any region changes, the region is
 * marked "dirty".  This routine walks through all the dirty regions
 * and makes sure that the dictionary invariants are satisfied
 * (see the comments at the beginning of this file).  Of course
 * new dirty regions can be created as we make changes to restore
 * the invariants.
 */ {
        var regUp: ActiveRegion? = regUp
        var regLo: ActiveRegion? = RegionBelow(regUp ?: error("regUp == null"))
        var eUp: GLUhalfEdge
        var eLo: GLUhalfEdge
        while (true) {

            /* Find the lowest dirty region (we walk from the bottom up). */
            while (regLo!!.dirty) {
                regUp = regLo
                regLo = RegionBelow(regLo)!!
            }
            if (!regUp!!.dirty) {
                regLo = regUp
                regUp = RegionAbove(regUp)
                if (regUp == null || !regUp.dirty) {
                    /* We've walked all the dirty regions */
                    return
                }
            }
            regUp.dirty = false
            eUp = regUp.eUp!!
            eLo = regLo.eUp!!
            if (eUp.Sym!!.Org !== eLo.Sym!!.Org) {
                /* Check that the edge ordering is obeyed at the Dst vertices. */
                if (CheckForLeftSplice(tess, regUp)) {

                    /* If the upper or lower edge was marked fixUpperEdge, then
                     * we no longer need it (since these edges are needed only for
                     * vertices which otherwise have no right-going edges).
                     */
                    if (regLo.fixUpperEdge) {
                        DeleteRegion(tess, regLo)
                        if (!Mesh.__gl_meshDelete(eLo)) throw RuntimeException()
                        regLo = RegionBelow(regUp)!!
                        eLo = regLo.eUp!!
                    } else if (regUp.fixUpperEdge) {
                        DeleteRegion(tess, regUp)
                        if (!Mesh.__gl_meshDelete(eUp)) throw RuntimeException()
                        regUp = RegionAbove(regLo)!!
                        eUp = regUp.eUp!!
                    }
                }
            }
            if (eUp.Org !== eLo.Org) {
                if (eUp.Sym!!.Org !== eLo.Sym!!.Org && !regUp.fixUpperEdge && !regLo.fixUpperEdge
                    && (eUp.Sym!!.Org === tess.event || eLo.Sym!!.Org === tess.event)
                ) {
                    /* When all else fails in CheckForIntersect(), it uses tess.event
                     * as the intersection location.  To make this possible, it requires
                     * that tess.event lie between the upper and lower edges, and also
                     * that neither of these is marked fixUpperEdge (since in the worst
                     * case it might splice one of these edges into tess.event, and
                     * violate the invariant that fixable edges are the only right-going
                     * edge from their associated vertex).
                         */
                    if (CheckForIntersect(tess, regUp)) {
                        /* WalkDirtyRegions() was called recursively; we're done */
                        return
                    }
                } else {
                    /* Even though we can't use CheckForIntersect(), the Org vertices
                     * may violate the dictionary edge ordering.  Check and correct this.
                     */
                    CheckForRightSplice(tess, regUp)
                }
            }
            if (eUp.Org === eLo.Org && eUp.Sym!!.Org === eLo.Sym!!.Org) {
                /* A degenerate loop consisting of only two edges -- delete it. */
                AddWinding(eLo, eUp)
                DeleteRegion(tess, regUp)
                if (!Mesh.__gl_meshDelete(eUp)) throw RuntimeException()
                regUp = RegionAbove(regLo)!!
            }
        }
    }

    fun ConnectRightVertex(
        tess: GLUtessellatorImpl, regUp: ActiveRegion,
        eBottomLeft: GLUhalfEdge
    ) /*
 * Purpose: connect a "right" vertex vEvent (one where all edges go left)
 * to the unprocessed portion of the mesh.  Since there are no right-going
 * edges, two regions (one above vEvent and one below) are being merged
 * into one.  "regUp" is the upper of these two regions.
 *
 * There are two reasons for doing this (adding a right-going edge):
 *  - if the two regions being merged are "inside", we must add an edge
 *    to keep them separated (the combined region would not be monotone).
 *  - in any case, we must leave some record of vEvent in the dictionary,
 *    so that we can merge vEvent with features that we have not seen yet.
 *    For example, maybe there is a vertical edge which passes just to
 *    the right of vEvent; we would like to splice vEvent into this edge.
 *
 * However, we don't want to connect vEvent to just any vertex.  We don''t
 * want the new edge to cross any other edges; otherwise we will create
 * intersection vertices even when the input data had no self-intersections.
 * (This is a bad thing; if the user's input data has no intersections,
 * we don't want to generate any false intersections ourselves.)
 *
 * Our eventual goal is to connect vEvent to the leftmost unprocessed
 * vertex of the combined region (the union of regUp and regLo).
 * But because of unseen vertices with all right-going edges, and also
 * new vertices which may be created by edge intersections, we don''t
 * know where that leftmost unprocessed vertex is.  In the meantime, we
 * connect vEvent to the closest vertex of either chain, and mark the region
 * as "fixUpperEdge".  This flag says to delete and reconnect this edge
 * to the next processed vertex on the boundary of the combined region.
 * Quite possibly the vertex we connected to will turn out to be the
 * closest one, in which case we won''t need to make any changes.
 */ {
        var regUp: ActiveRegion = regUp
        var eBottomLeft: GLUhalfEdge = eBottomLeft
        var eNew: GLUhalfEdge
        var eTopLeft: GLUhalfEdge = eBottomLeft.Onext!!
        val regLo: ActiveRegion = RegionBelow(regUp)!!
        val eUp: GLUhalfEdge = regUp.eUp!!
        val eLo: GLUhalfEdge = regLo.eUp!!
        var degenerate = false
        if (eUp.Sym!!.Org !== eLo.Sym!!.Org) {
            CheckForIntersect(tess, regUp)
        }

        /* Possible new degeneracies: upper or lower edge of regUp may pass
         * through vEvent, or may coincide with new intersection vertex
         */if (Geom.VertEq(eUp.Org!!, tess.event!!)) {
            if (!Mesh.__gl_meshSplice(eTopLeft.Sym!!.Lnext!!, eUp)) throw RuntimeException()
            regUp = TopLeftRegion(regUp)!!
            if (regUp == null) throw RuntimeException()
            eTopLeft = RegionBelow(regUp)!!.eUp!!
            FinishLeftRegions(tess, RegionBelow(regUp)!!, regLo)
            degenerate = true
        }
        if (Geom.VertEq(eLo.Org!!, tess.event!!)) {
            if (!Mesh.__gl_meshSplice(
                    eBottomLeft,
                    eLo.Sym!!.Lnext!!
                )
            ) throw RuntimeException()
            eBottomLeft = FinishLeftRegions(tess, regLo, null)
            degenerate = true
        }
        if (degenerate) {
            AddRightEdges(tess, regUp, eBottomLeft.Onext!!, eTopLeft, eTopLeft, true)
            return
        }

        /* Non-degenerate situation -- need to add a temporary, fixable edge.
         * Connect to the closer of eLo.Org, eUp.Org.
         */
        eNew = if (Geom.VertLeq(eLo.Org!!, eUp.Org!!)) {
            eLo.Sym!!.Lnext!!
        } else {
            eUp
        }
        eNew = Mesh.__gl_meshConnect(eBottomLeft.Onext!!.Sym!!, eNew)
        //if (eNew == null) throw RuntimeException()

        /* Prevent cleanup, otherwise eNew might disappear before we've even
         * had a chance to mark it as a temporary edge.
         */
        AddRightEdges(tess, regUp, eNew, eNew.Onext!!, eNew.Onext, false)
        eNew.Sym!!.activeRegion!!.fixUpperEdge = true
        WalkDirtyRegions(tess, regUp)
    }

    /* Because vertices at exactly the same location are merged together
 * before we process the sweep event, some degenerate cases can't occur.
 * However if someone eventually makes the modifications required to
 * merge features which are close together, the cases below marked
 * TOLERANCE_NONZERO will be useful.  They were debugged before the
 * code to merge identical vertices in the main loop was added.
 */
    private const val TOLERANCE_NONZERO = false
    fun ConnectLeftDegenerate(
        tess: GLUtessellatorImpl,
        regUp: ActiveRegion, vEvent: GLUvertex
    ) /*
 * The event vertex lies exacty on an already-processed edge or vertex.
 * Adding the new vertex involves splicing it into the already-processed
 * part of the mesh.
 */ {
        var regUp: ActiveRegion = regUp
        var eTopLeft: GLUhalfEdge?
        var eTopRight: GLUhalfEdge
        val e: GLUhalfEdge = regUp.eUp!!
        if (Geom.VertEq(e.Org!!, vEvent)) {
            /* e.Org is an unprocessed vertex - just combine them, and wait
             * for e.Org to be pulled from the queue
             */
            //require(TOLERANCE_NONZERO)
            SpliceMergeVertices(tess, e, vEvent.anEdge!!)
            return
        }
        if (!Geom.VertEq(e.Sym!!.Org!!, vEvent)) {
            /* General case -- splice vEvent into edge e which passes through it */
            if (Mesh.__gl_meshSplitEdge(e.Sym!!) == null) throw RuntimeException()
            if (regUp.fixUpperEdge) {
                /* This edge was fixable -- delete unused portion of original edge */
                if (!Mesh.__gl_meshDelete(e.Onext!!)) throw RuntimeException()
                regUp.fixUpperEdge = false
            }
            if (!Mesh.__gl_meshSplice(vEvent.anEdge!!, e)) throw RuntimeException()
            SweepEvent(tess, vEvent) /* recurse */
            return
        }
        //assert(TOLERANCE_NONZERO)
        regUp = TopRightRegion(regUp)
        val reg: ActiveRegion = RegionBelow(regUp)!!
        eTopRight = reg.eUp!!.Sym!!
        val eLast: GLUhalfEdge = eTopRight.Onext!!
        eTopLeft = eLast
        if (reg.fixUpperEdge) {
            /* Here e.Sym.Org has only a single fixable edge going right.
             * We can delete it since now we have some real right-going edges.
             */
            require(eTopLeft !== eTopRight /* there are some left edges too */)
            DeleteRegion(tess, reg)
            if (!Mesh.__gl_meshDelete(eTopRight)) throw RuntimeException()
            eTopRight = eTopLeft.Sym!!.Lnext!!
        }
        if (!Mesh.__gl_meshSplice(vEvent.anEdge!!, eTopRight)) throw RuntimeException()
        if (!Geom.EdgeGoesLeft(eTopLeft)) {
            /* e.Sym.Org had no left-going edges -- indicate this to AddRightEdges() */
            eTopLeft = null
        }
        AddRightEdges(tess, regUp, eTopRight.Onext!!, eLast, eTopLeft, true)
    }

    fun ConnectLeftVertex(
        tess: GLUtessellatorImpl,
        vEvent: GLUvertex
    ) /*
 * Purpose: connect a "left" vertex (one where both edges go right)
 * to the processed portion of the mesh.  Let R be the active region
 * containing vEvent, and let U and L be the upper and lower edge
 * chains of R.  There are two possibilities:
 *
 * - the normal case: split R into two regions, by connecting vEvent to
 *   the rightmost vertex of U or L lying to the left of the sweep line
 *
 * - the degenerate case: if vEvent is close enough to U or L, we
 *   merge vEvent into that edge chain.  The subcases are:
 *	- merging with the rightmost vertex of U or L
 *	- merging with the active edge of U or L
 *	- merging with an already-processed portion of U or L
 */ {
        val regUp: ActiveRegion
        val reg: ActiveRegion
        val eLo: GLUhalfEdge
        val eNew: GLUhalfEdge
        val tmp = ActiveRegion()

        /* assert ( vEvent.anEdge.Onext.Onext == vEvent.anEdge ); */

        /* Get a pointer to the active region containing vEvent */
        tmp.eUp = vEvent.anEdge!!.Sym
        /* __GL_DICTLISTKEY */ /* __gl_dictListSearch */
        regUp = Dict.dictKey(
            Dict.dictSearch(
                tess.dict!!,
                tmp
            )!!
        ) as ActiveRegion
        val regLo: ActiveRegion = RegionBelow(regUp)!!
        val eUp: GLUhalfEdge = regUp.eUp!!
        eLo = regLo.eUp!!

        /* Try merging with U or L first */
        if (Geom.EdgeSign(
                eUp.Sym!!.Org!!,
                vEvent,
                eUp.Org!!
            ) == 0.0
        ) {
            ConnectLeftDegenerate(tess, regUp, vEvent)
            return
        }

        /* Connect vEvent to rightmost processed vertex of either chain.
         * e.Sym.Org is the vertex that we will connect to vEvent.
         */reg = if (Geom.VertLeq(eLo.Sym!!.Org!!, eUp.Sym!!.Org!!)) regUp else regLo
        if (regUp.inside || reg.fixUpperEdge) {
            if (reg === regUp) {
                eNew = Mesh.__gl_meshConnect(vEvent.anEdge!!.Sym!!, eUp.Lnext!!)
                if (eNew == null) throw RuntimeException()
            } else {
                val tempHalfEdge: GLUhalfEdge =
                    Mesh.__gl_meshConnect(eLo.Sym!!.Onext!!.Sym!!, vEvent.anEdge!!)

                eNew = tempHalfEdge.Sym!!
            }
            if (reg.fixUpperEdge) {
                if (!FixUpperEdge(reg, eNew)) throw RuntimeException()
            } else {
                ComputeWinding(tess, AddRegionBelow(tess, regUp, eNew)!!)
            }
            SweepEvent(tess, vEvent)
        } else {
            /* The new vertex is in a region which does not belong to the polygon.
             * We don''t need to connect this vertex to the rest of the mesh.
             */
            AddRightEdges(tess, regUp, vEvent.anEdge!!, vEvent.anEdge!!, null, true)
        }
    }

    fun SweepEvent(
        tess: GLUtessellatorImpl,
        vEvent: GLUvertex
    ) /*
 * Does everything necessary when the sweep line crosses a vertex.
 * Updates the mesh and the edge dictionary.
 */ {
        val regUp: ActiveRegion?
        var e: GLUhalfEdge
        tess.event = vEvent /* for access in EdgeLeq() */
        DebugEvent(tess)

        /* Check if this vertex is the right endpoint of an edge that is
         * already in the dictionary.  In this case we don't need to waste
         * time searching for the location to insert new edges.
         */
        e = vEvent.anEdge!!
        while (e.activeRegion == null) {
            e = e.Onext!!
            if (e === vEvent.anEdge) {
                /* All edges go right -- not incident to any processed edges */
                ConnectLeftVertex(tess, vEvent)
                return
            }
        }

        /* Processing consists of two phases: first we "finish" all the
         * active regions where both the upper and lower edges terminate
         * at vEvent (ie. vEvent is closing off these regions).
         * We mark these faces "inside" or "outside" the polygon according
         * to their winding number, and delete the edges from the dictionary.
         * This takes care of all the left-going edges from vEvent.
         */regUp = TopLeftRegion(e.activeRegion!!)!!
        //if (regUp == null) throw RuntimeException()
        val reg: ActiveRegion = RegionBelow(regUp)!!
        val eTopLeft: GLUhalfEdge = reg.eUp!!
        val eBottomLeft: GLUhalfEdge = FinishLeftRegions(tess, reg, null)

        /* Next we process all the right-going edges from vEvent.  This
         * involves adding the edges to the dictionary, and creating the
         * associated "active regions" which record information about the
         * regions between adjacent dictionary edges.
         */if (eBottomLeft.Onext === eTopLeft) {
            /* No right-going edges -- add a temporary "fixable" edge */
            ConnectRightVertex(tess, regUp, eBottomLeft)
        } else {
            AddRightEdges(tess, regUp, eBottomLeft.Onext!!, eTopLeft, eTopLeft, true)
        }
    }

    /* Make the sentinel coordinates big enough that they will never be
 * merged with real input features.  (Even with the largest possible
 * input contour and the maximum tolerance of 1.0, no merging will be
 * done with coordinates larger than 3 * GLU_TESS_MAX_COORD).
 */
    private const val SENTINEL_COORD: Double = 4.0 * GLU.GLU_TESS_MAX_COORD
    fun AddSentinel(tess: GLUtessellatorImpl, t: Double) /*
 * We add two sentinel edges above and below all other edges,
 * to avoid special cases at the top and bottom.
 */ {
        val reg = ActiveRegion()
        val e: GLUhalfEdge = Mesh.__gl_meshMakeEdge(tess.mesh!!)!!
        if (e == null) throw RuntimeException()
        e.Org!!.s = SENTINEL_COORD
        e.Org!!.t = t
        e.Sym!!.Org!!.s = -SENTINEL_COORD
        e.Sym!!.Org!!.t = t
        tess.event = e.Sym!!.Org /* initialize it */
        reg.eUp = e
        reg.windingNumber = 0
        reg.inside = false
        reg.fixUpperEdge = false
        reg.sentinel = true
        reg.dirty = false
        reg.nodeUp = Dict.dictInsert(tess.dict!!, reg) /* __gl_dictListInsertBefore */
        if (reg.nodeUp == null) throw RuntimeException()
    }

    fun InitEdgeDict(tess: GLUtessellatorImpl) /*
 * We maintain an ordering of edge intersections with the sweep line.
 * This order is maintained in a dynamic dictionary.
 */ {
        /* __gl_dictListNewDict */
        tess.dict = Dict.dictNewDict(
            tess,
            object : Dict.DictLeq {
                override fun leq(frame: Any, key1: Any, key2: Any): Boolean {
                    return EdgeLeq(
                        tess,
                        key1 as ActiveRegion,
                        key2 as ActiveRegion
                    )
                }
            })
        if (tess.dict == null) throw RuntimeException()
        AddSentinel(tess, -SENTINEL_COORD)
        AddSentinel(tess, SENTINEL_COORD)
    }

    fun DoneEdgeDict(tess: GLUtessellatorImpl) {
        var reg: ActiveRegion?
        var fixedEdges = 0

        /* __GL_DICTLISTKEY */ /* __GL_DICTLISTMIN */

        while ((Dict.dictKey(
                Dict.dictMin(
                    tess.dict!!
                )
            ) as ActiveRegion?).also { reg = it } != null
        ) {
            /*
             * At the end of all processing, the dictionary should contain
             * only the two sentinel edges, plus at most one "fixable" edge
             * created by ConnectRightVertex().
             */
            if (!reg!!.sentinel) {
                require(reg!!.fixUpperEdge)
                require(++fixedEdges == 1)
            }
            require(reg!!.windingNumber == 0)
            DeleteRegion(tess, reg)
            /*    __gl_meshDelete( reg.eUp );*/
        }
        Dict.dictDeleteDict(tess.dict!!) /* __gl_dictListDeleteDict */
    }

    fun RemoveDegenerateEdges(tess: GLUtessellatorImpl) /*
 * Remove zero-length edges, and contours with fewer than 3 vertices.
 */ {
        var e: GLUhalfEdge
        var eNext: GLUhalfEdge
        var eLnext: GLUhalfEdge
        val eHead: GLUhalfEdge = tess.mesh!!.eHead

        /*LINTED*/
        e = eHead.next!!
        while (e !== eHead) {
            eNext = e.next!!
            eLnext = e.Lnext!!
            if (Geom.VertEq(e.Org!!, e.Sym!!.Org!!) && e.Lnext!!.Lnext !== e) {
                /* Zero-length edge, contour has at least 3 edges */
                SpliceMergeVertices(tess, eLnext, e) /* deletes e.Org */
                if (!Mesh.__gl_meshDelete(e)) throw RuntimeException() /* e is a self-loop */
                e = eLnext
                eLnext = e.Lnext!!
            }
            if (eLnext.Lnext === e) {
                /* Degenerate contour (one or two edges) */
                if (eLnext !== e) {
                    if (eLnext === eNext || eLnext === eNext.Sym) {
                        eNext = eNext.next!!
                    }
                    if (!Mesh.__gl_meshDelete(eLnext)) throw RuntimeException()
                }
                if (e === eNext || e === eNext.Sym) {
                    eNext = eNext.next!!
                }
                if (!Mesh.__gl_meshDelete(e)) throw RuntimeException()
            }
            e = eNext
        }
    }

    fun InitPriorityQ(tess: GLUtessellatorImpl): Boolean /*
 * Insert all vertices into the priority queue which determines the
 * order in which vertices cross the sweep line.
 */ {
        var v: GLUvertex

        /* __gl_pqSortNewPriorityQ */tess.pq = PriorityQ.pqNewPriorityQ(object :
            PriorityQ.Leq {
            override fun leq(key1: Any?, key2: Any?): Boolean {
                return Geom.VertLeq(
                    key1 as GLUvertex,
                    key2 as GLUvertex
                )
            }
        })
        val pq: PriorityQ? = tess.pq
        if (pq == null) return false
        val vHead: GLUvertex = tess.mesh!!.vHead
        v = vHead.next!!
        while (v !== vHead) {
            v.pqHandle = pq.pqInsert(v) /* __gl_pqSortInsert */
            if (v.pqHandle.toLong() == Long.MAX_VALUE) break
            v = v.next!!
        }
        if (v !== vHead || !pq.pqInit()) { /* __gl_pqSortInit */
            tess.pq!!.pqDeletePriorityQ() /* __gl_pqSortDeletePriorityQ */
            tess.pq = null
            return false
        }
        return true
    }

    fun DonePriorityQ(tess: GLUtessellatorImpl) {
        tess.pq!!.pqDeletePriorityQ() /* __gl_pqSortDeletePriorityQ */
    }

    fun RemoveDegenerateFaces(mesh: GLUmesh): Boolean /*
 * Delete any degenerate faces with only two edges.  WalkDirtyRegions()
 * will catch almost all of these, but it won't catch degenerate faces
 * produced by splice operations on already-processed edges.
 * The two places this can happen are in FinishLeftRegions(), when
 * we splice in a "temporary" edge produced by ConnectRightVertex(),
 * and in CheckForLeftSplice(), where we splice already-processed
 * edges to ensure that our dictionary invariants are not violated
 * by numerical errors.
 *
 * In both these cases it is *very* dangerous to delete the offending
 * edge at the time, since one of the routines further up the stack
 * will sometimes be keeping a pointer to that edge.
 */ {
        var f: GLUface
        var fNext: GLUface
        var e: GLUhalfEdge

        /*LINTED*/
        f = mesh.fHead.next!!
        while (f !== mesh.fHead) {
            fNext = f.next!!
            e = f.anEdge!!
            require(e.Lnext !== e)
            if (e.Lnext!!.Lnext === e) {
                /* A face with only two edges */
                AddWinding(e.Onext!!, e)
                if (!Mesh.__gl_meshDelete(e)) return false
            }
            f = fNext
        }
        return true
    }

    fun __gl_computeInterior(tess: GLUtessellatorImpl): Boolean /*
 * __gl_computeInterior( tess ) computes the planar arrangement specified
 * by the given contours, and further subdivides this arrangement
 * into regions.  Each region is marked "inside" if it belongs
 * to the polygon, according to the rule given by tess.windingRule.
 * Each interior region is guaranteed be monotone.
 */ {
        var v: GLUvertex?
        var vNext: GLUvertex?
        tess.fatalError = false

        /* Each vertex defines an event for our sweep line.  Start by inserting
         * all the vertices in a priority queue.  Events are processed in
         * lexicographic order, ie.
         *
         *	e1 < e2  iff  e1.x < e2.x || (e1.x == e2.x && e1.y < e2.y)
         */RemoveDegenerateEdges(tess)
        if (!InitPriorityQ(tess)) return false /* if error */
        InitEdgeDict(tess)

        /* __gl_pqSortExtractMin */while ((tess.pq!!.pqExtractMin() as GLUvertex?).also {
                v = it
            } != null) {
            while (true) {
                vNext = tess.pq!!.pqMinimum() as GLUvertex? /* __gl_pqSortMinimum */
                if (vNext == null || !Geom.VertEq(vNext, v!!)) break

                /* Merge together all vertices at exactly the same location.
                 * This is more efficient than processing them one at a time,
                 * simplifies the code (see ConnectLeftDegenerate), and is also
                 * important for correct handling of certain degenerate cases.
                 * For example, suppose there are two identical edges A and B
                 * that belong to different contours (so without this code they would
                 * be processed by separate sweep events).  Suppose another edge C
                 * crosses A and B from above.  When A is processed, we split it
                 * at its intersection point with C.  However this also splits C,
                 * so when we insert B we may compute a slightly different
                 * intersection point.  This might leave two edges with a small
                 * gap between them.  This kind of error is especially obvious
                 * when using boundary extraction (GLU_TESS_BOUNDARY_ONLY).
                 */vNext =
                    tess.pq!!.pqExtractMin() as GLUvertex /* __gl_pqSortExtractMin*/
                SpliceMergeVertices(tess, v!!.anEdge!!, vNext.anEdge!!)
            }
            SweepEvent(tess, v!!)
        }

        /* Set tess.event for debugging purposes */
        /* __GL_DICTLISTKEY */ /* __GL_DICTLISTMIN */tess.event =
            (Dict.dictKey(Dict.dictMin(tess.dict!!)) as ActiveRegion).eUp!!.Org
        DebugEvent(tess)
        DoneEdgeDict(tess)
        DonePriorityQ(tess)
        if (!RemoveDegenerateFaces(tess.mesh!!)) return false
        Mesh.__gl_meshCheckMesh(tess.mesh!!)
        return true
    }
}
