package org.openrndr.ktessellation

internal object Mesh {
    /************************ Utility Routines  */ /* MakeEdge creates a new pair of half-edges which form their own loop.
 * No vertex or face structures are allocated, but these must be assigned
 * before the current edge operation is completed.
 */
    fun MakeEdge(eNext: GLUhalfEdge): GLUhalfEdge {
        var eNext: GLUhalfEdge = eNext
        val ePrev: GLUhalfEdge

//        EdgePair * pair = (EdgePair *)
//        memAlloc(sizeof(EdgePair));
//        if (pair == NULL) return NULL;
//
//        e = &pair - > e;
        val e: GLUhalfEdge = GLUhalfEdge(true)
        //        eSym = &pair - > eSym;
        val eSym: GLUhalfEdge = GLUhalfEdge(false)


        /* Make sure eNext points to the first edge of the edge pair */if (!eNext.first) {
            eNext = eNext.Sym ?: error("eNext.Sym == null")
        }

        /* Insert in circular doubly-linked list before eNext.
         * Note that the prev pointer is stored in Sym->next.
         */ePrev = eNext.Sym?.next ?: error("eNext.Sym.next == null")
        eSym.next = ePrev
        ePrev.Sym!!.next = e
        e.next = eNext
        eNext.Sym!!.next = eSym
        e.Sym = eSym
        e.Onext = e
        e.Lnext = eSym
        e.Org = null
        e.Lface = null
        e.winding = 0
        e.activeRegion = null
        eSym.Sym = e
        eSym.Onext = eSym
        eSym.Lnext = e
        eSym.Org = null
        eSym.Lface = null
        eSym.winding = 0
        eSym.activeRegion = null
        return e
    }

    /* Splice( a, b ) is best described by the Guibas/Stolfi paper or the
 * CS348a notes (see mesh.h).  Basically it modifies the mesh so that
 * a->Onext and b->Onext are exchanged.  This can have various effects
 * depending on whether a and b belong to different face or vertex rings.
 * For more explanation see __gl_meshSplice() below.
 */
    fun Splice(a: GLUhalfEdge, b: GLUhalfEdge) {
        val aOnext: GLUhalfEdge = a.Onext ?: error("a.Onext == null")
        val bOnext: GLUhalfEdge = b.Onext ?: error("a.Onext == null")
        aOnext.Sym?.Lnext = b
        bOnext.Sym?.Lnext = a
        a.Onext = bOnext
        b.Onext = aOnext
    }

    /* MakeVertex( newVertex, eOrig, vNext ) attaches a new vertex and makes it the
 * origin of all edges in the vertex loop to which eOrig belongs. "vNext" gives
 * a place to insert the new vertex in the global vertex list.  We insert
 * the new vertex *before* vNext so that algorithms which walk the vertex
 * list will not see the newly created vertices.
 */
    fun MakeVertex(
        newVertex: GLUvertex,
        eOrig: GLUhalfEdge, vNext: GLUvertex
    ) {
        var e: GLUhalfEdge
        val vPrev: GLUvertex
        val vNew: GLUvertex = newVertex
        require(vNew != null)

        /* insert in circular doubly-linked list before vNext */
        vPrev = vNext.prev?:error("vNext.prev == null")
        vNew.prev = vPrev
        vPrev.next = vNew
        vNew.next = vNext
        vNext.prev = vNew
        vNew.anEdge = eOrig
        vNew.data = null
        /* leave coords, s, t undefined */

        /* fix other edges on this vertex loop */e = eOrig
        do {
            e.Org = vNew
            e = e.Onext ?: error(e.Onext == null)
        } while (e !== eOrig)
    }

    /* MakeFace( newFace, eOrig, fNext ) attaches a new face and makes it the left
 * face of all edges in the face loop to which eOrig belongs.  "fNext" gives
 * a place to insert the new face in the global face list.  We insert
 * the new face *before* fNext so that algorithms which walk the face
 * list will not see the newly created faces.
 */
    fun MakeFace(
        newFace: GLUface,
        eOrig: GLUhalfEdge,
        fNext: GLUface
    ) {
        var e: GLUhalfEdge
        val fPrev: GLUface
        val fNew: GLUface = newFace
        require(fNew != null)

        /* insert in circular doubly-linked list before fNext */
        fPrev = fNext.prev ?: error("fNext.prev == null")
        fNew.prev = fPrev
        fPrev.next = fNew
        fNew.next = fNext
        fNext.prev = fNew
        fNew.anEdge = eOrig
        fNew.data = null
        fNew.trail = null
        fNew.marked = false

        /* The new face is marked "inside" if the old one was.  This is a
         * convenience for the common case where a face has been split in two.
         */fNew.inside = fNext.inside

        /* fix other edges on this face loop */e = eOrig
        do {
            e.Lface = fNew
            e = e.Lnext ?: error("e.Lnext == null")
        } while (e !== eOrig)
    }

    /* KillEdge( eDel ) destroys an edge (the half-edges eDel and eDel->Sym),
 * and removes from the global edge list.
 */
    fun KillEdge(eDel: GLUhalfEdge) {
        var eDel: GLUhalfEdge = eDel
        val ePrev: GLUhalfEdge
        val eNext: GLUhalfEdge

        /* Half-edges are allocated in pairs, see EdgePair above */if (!eDel.first) {
            eDel = eDel.Sym ?: error("eDel.Sym == null")
        }

        /* delete from circular doubly-linked list */eNext = eDel.next ?: error("eDel.next == null")
        ePrev = eDel.Sym?.next ?: error("eDel.Sym.next == null")
        eNext.Sym!!.next = ePrev
        ePrev.Sym!!.next = eNext
    }

    /* KillVertex( vDel ) destroys a vertex and removes it from the global
 * vertex list.  It updates the vertex loop to point to a given new vertex.
 */
    fun KillVertex(vDel: GLUvertex, newOrg: GLUvertex?) {
        var e: GLUhalfEdge
        val eStart: GLUhalfEdge = vDel.anEdge ?: error("vDel.anEdge == null")
        val vPrev: GLUvertex
        val vNext: GLUvertex

        /* change the origin of all affected edges */e = eStart
        do {
            e.Org = newOrg
            e = e.Onext ?: error("e.Onext == null")
        } while (e !== eStart)

        /* delete from circular doubly-linked list */vPrev = vDel.prev ?: error("vDel.prev == null")
        vNext = vDel.next ?: error("vDel.next == null")
        vNext.prev = vPrev
        vPrev.next = vNext
    }

    /* KillFace( fDel ) destroys a face and removes it from the global face
 * list.  It updates the face loop to point to a given new face.
 */
    fun KillFace(fDel: GLUface, newLface: GLUface?) {
        var e: GLUhalfEdge
        val eStart: GLUhalfEdge = fDel.anEdge ?: error("fDel.anEdge == null")
        val fPrev: GLUface
        val fNext: GLUface

        /* change the left face of all affected edges */e = eStart
        do {
            e.Lface = newLface
            e = e.Lnext ?: error("e.Lnext == null")
        } while (e !== eStart)

        /* delete from circular doubly-linked list */fPrev = fDel.prev ?: error("fDel.prev == null")
        fNext = fDel.next ?: error("fDel.next == null")
        fNext.prev = fPrev
        fPrev.next = fNext
    }

    /****************** Basic Edge Operations  */ /* __gl_meshMakeEdge creates one edge, two vertices, and a loop (face).
 * The loop consists of the two new half-edges.
 */
    fun __gl_meshMakeEdge(mesh: GLUmesh): GLUhalfEdge? {
        val newVertex1: GLUvertex = GLUvertex()
        val newVertex2: GLUvertex = GLUvertex()
        val newFace: GLUface = GLUface()
        val e: GLUhalfEdge
        e = MakeEdge(mesh.eHead)
        if (e == null) return null
        MakeVertex(newVertex1, e, mesh.vHead)
        MakeVertex(newVertex2, e.Sym ?: error("e.Sym == null"), mesh.vHead)
        MakeFace(newFace, e, mesh.fHead)
        return e
    }

    /* __gl_meshSplice( eOrg, eDst ) is the basic operation for changing the
 * mesh connectivity and topology.  It changes the mesh so that
 *	eOrg->Onext <- OLD( eDst->Onext )
 *	eDst->Onext <- OLD( eOrg->Onext )
 * where OLD(...) means the value before the meshSplice operation.
 *
 * This can have two effects on the vertex structure:
 *  - if eOrg->Org != eDst->Org, the two vertices are merged together
 *  - if eOrg->Org == eDst->Org, the origin is split into two vertices
 * In both cases, eDst->Org is changed and eOrg->Org is untouched.
 *
 * Similarly (and independently) for the face structure,
 *  - if eOrg->Lface == eDst->Lface, one loop is split into two
 *  - if eOrg->Lface != eDst->Lface, two distinct loops are joined into one
 * In both cases, eDst->Lface is changed and eOrg->Lface is unaffected.
 *
 * Some special cases:
 * If eDst == eOrg, the operation has no effect.
 * If eDst == eOrg->Lnext, the new face will have a single edge.
 * If eDst == eOrg->Lprev, the old face will have a single edge.
 * If eDst == eOrg->Onext, the new vertex will have a single edge.
 * If eDst == eOrg->Oprev, the old vertex will have a single edge.
 */
    fun __gl_meshSplice(
        eOrg: GLUhalfEdge,
        eDst: GLUhalfEdge
    ): Boolean {
        var joiningLoops = false
        var joiningVertices = false
        if (eOrg === eDst) return true
        if (eDst.Org !== eOrg.Org) {
            /* We are merging two disjoint vertices -- destroy eDst->Org */
            joiningVertices = true
            KillVertex(eDst.Org ?: error("eDst.Org == null"), eOrg.Org ?: error("eOrg.Org == null"))
        }
        if (eDst.Lface !== eOrg.Lface) {
            /* We are connecting two disjoint loops -- destroy eDst.Lface */
            joiningLoops = true
            KillFace(eDst.Lface ?: error("eDst.Lface == null"), eOrg.Lface ?: error("eOrg.Lface == null"))
        }

        /* Change the edge structure */Splice(eDst, eOrg)
        if (!joiningVertices) {
            val newVertex: GLUvertex = GLUvertex()

            /* We split one vertex into two -- the new vertex is eDst.Org.
             * Make sure the old vertex points to a valid half-edge.
             */MakeVertex(newVertex, eDst, eOrg.Org?:error("eOrg.Org == null"))
            eOrg.Org!!.anEdge = eOrg
        }
        if (!joiningLoops) {
            val newFace: GLUface = GLUface()

            /* We split one loop into two -- the new loop is eDst.Lface.
             * Make sure the old face points to a valid half-edge.
             */MakeFace(newFace, eDst, eOrg.Lface ?: error("eOrg.Lface == null"))
            eOrg.Lface!!.anEdge = eOrg
        }
        return true
    }

    /* __gl_meshDelete( eDel ) removes the edge eDel.  There are several cases:
 * if (eDel.Lface != eDel.Rface), we join two loops into one; the loop
 * eDel.Lface is deleted.  Otherwise, we are splitting one loop into two;
 * the newly created loop will contain eDel.Dst.  If the deletion of eDel
 * would create isolated vertices, those are deleted as well.
 *
 * This function could be implemented as two calls to __gl_meshSplice
 * plus a few calls to memFree, but this would allocate and delete
 * unnecessary vertices and faces.
 */
    fun __gl_meshDelete(eDel: GLUhalfEdge): Boolean {
        val eDelSym: GLUhalfEdge = eDel.Sym ?: error("eDel.Sym == null")
        var joiningLoops = false

        /* First step: disconnect the origin vertex eDel.Org.  We make all
         * changes to get a consistent mesh in this "intermediate" state.
         */if (eDel.Lface !== eDel.Sym!!.Lface) {
            /* We are joining two loops into one -- remove the left face */
            joiningLoops = true
            KillFace(eDel.Lface ?: error("eDel.Lface == null"), eDel.Sym?.Lface ?: error("eDel.Sym.Lface == null"))
        }
        if (eDel.Onext === eDel) {
            KillVertex(eDel.Org ?: error("eDel.Org == null"), null)
        } else {
            /* Make sure that eDel.Org and eDel.Sym.Lface point to valid half-edges */
            eDel.Sym!!.Lface!!.anEdge = eDel.Sym!!.Lnext
            eDel.Org!!.anEdge = eDel!!.Onext
            Splice(eDel, eDel.Sym!!.Lnext!!)
            if (!joiningLoops) {
                val newFace: GLUface = GLUface()

                /* We are splitting one loop into two -- create a new loop for eDel. */MakeFace(
                    newFace,
                    eDel,
                    eDel.Lface ?: error("eDel.Lface == null")
                )
            }
        }

        /* Claim: the mesh is now in a consistent state, except that eDel.Org
         * may have been deleted.  Now we disconnect eDel.Dst.
         */if (eDelSym.Onext === eDelSym) {
            KillVertex(eDelSym.Org ?: error("eDelSym.Org == null"), null)
            KillFace(eDelSym.Lface ?: error("eDelSym.Lface == null"), null)
        } else {
            /* Make sure that eDel.Dst and eDel.Lface point to valid half-edges */
            eDel.Lface!!.anEdge = eDelSym.Sym!!.Lnext
            eDelSym.Org!!.anEdge = eDelSym.Onext
            Splice(eDelSym, eDelSym.Sym!!.Lnext ?: error("eDelSym.Lnext == null"))
        }

        /* Any isolated vertices or faces have already been freed. */
        KillEdge(eDel)
        return true
    }

    /******************** Other Edge Operations  */ /* All these routines can be implemented with the basic edge
 * operations above.  They are provided for convenience and efficiency.
 */
    /* __gl_meshAddEdgeVertex( eOrg ) creates a new edge eNew such that
 * eNew == eOrg.Lnext, and eNew.Dst is a newly created vertex.
 * eOrg and eNew will have the same left face.
 */
    fun __gl_meshAddEdgeVertex(eOrg: GLUhalfEdge): GLUhalfEdge {
        val eNewSym: GLUhalfEdge
        val eNew: GLUhalfEdge = MakeEdge(eOrg)
        eNewSym = eNew.Sym ?: error("eNew.Sym == null")

        /* Connect the new edge appropriately */
        Splice(eNew, eOrg.Lnext ?: error("eOrg.LNext == null"))

        /* Set the vertex and face information */
        eNew.Org = eOrg.Sym!!.Org
        run {
            val newVertex: GLUvertex = GLUvertex()
            MakeVertex(newVertex, eNewSym, eNew.Org ?: error("eNew.Org == null") )
        }
        eNewSym.Lface = eOrg.Lface
        eNew.Lface = eNewSym.Lface
        return eNew
    }

    /* __gl_meshSplitEdge( eOrg ) splits eOrg into two edges eOrg and eNew,
 * such that eNew == eOrg.Lnext.  The new vertex is eOrg.Sym.Org == eNew.Org.
 * eOrg and eNew will have the same left face.
 */
    fun __gl_meshSplitEdge(eOrg: GLUhalfEdge): GLUhalfEdge {
        val eNew: GLUhalfEdge
        val tempHalfEdge: GLUhalfEdge = __gl_meshAddEdgeVertex(eOrg)
        eNew = tempHalfEdge.Sym ?: error("tempHalfEdge.Sym == null")

        /* Disconnect eOrg from eOrg.Sym.Org and connect it to eNew.Org */
        Splice(eOrg.Sym ?: error("eOrg.sym == null"), eOrg.Sym?.Sym?.Lnext ?: error("eOrg.Sym.Sym.Lnext == null"))
        Splice(eOrg.Sym ?: error("eOrg.Sym == null"), eNew)

        /* Set the vertex and face information */
        eOrg.Sym?.Org = eNew.Org
        eNew.Sym?.Org?.anEdge = eNew.Sym /* may have pointed to eOrg.Sym */
        eNew.Sym?.Lface = eOrg.Sym?.Lface ?: error("eOrg.Sym.LFace == null")
        eNew.winding = eOrg.winding /* copy old winding information */
        eNew.Sym!!.winding = eOrg.Sym?.winding ?: error("eOrg.Sym.window == null")
        return eNew
    }

    /* __gl_meshConnect( eOrg, eDst ) creates a new edge from eOrg.Sym.Org
 * to eDst.Org, and returns the corresponding half-edge eNew.
 * If eOrg.Lface == eDst.Lface, this splits one loop into two,
 * and the newly created loop is eNew.Lface.  Otherwise, two disjoint
 * loops are merged into one, and the loop eDst.Lface is destroyed.
 *
 * If (eOrg == eDst), the new face will have only two edges.
 * If (eOrg.Lnext == eDst), the old face is reduced to a single edge.
 * If (eOrg.Lnext.Lnext == eDst), the old face is reduced to two edges.
 */
    fun __gl_meshConnect(
        eOrg: GLUhalfEdge,
        eDst: GLUhalfEdge
    ): GLUhalfEdge {
        val eNewSym: GLUhalfEdge
        var joiningLoops = false
        val eNew: GLUhalfEdge = MakeEdge(eOrg)
        eNewSym = eNew.Sym ?: error("eNew.Sym == null")
        if (eDst.Lface !== eOrg.Lface) {
            /* We are connecting two disjoint loops -- destroy eDst.Lface */
            joiningLoops = true
            KillFace(eDst.Lface ?: error("eDst.Lface == null"), eOrg.Lface)
        }

        /* Connect the new edge appropriately */
        Splice(eNew, eOrg.Lnext ?: error("eOrg.Lnext == null"))
        Splice(eNewSym, eDst)

        /* Set the vertex and face information */
        eNew.Org = eOrg.Sym?.Org
        eNewSym.Org = eDst.Org
        eNewSym.Lface = eOrg.Lface
        eNew.Lface = eNewSym.Lface

        /* Make sure the old face points to a valid half-edge */
        eOrg.Lface!!.anEdge = eNewSym
        if (!joiningLoops) {
            val newFace: GLUface = GLUface()

            /* We split one loop into two -- the new loop is eNew.Lface */
            MakeFace(newFace, eNew, eOrg.Lface?:error("eOrg.Lface == null"))
        }
        return eNew
    }

    /******************** Other Operations  */ /* __gl_meshZapFace( fZap ) destroys a face and removes it from the
 * global face list.  All edges of fZap will have a null pointer as their
 * left face.  Any edges which also have a null pointer as their right face
 * are deleted entirely (along with any isolated vertices this produces).
 * An entire mesh can be deleted by zapping its faces, one at a time,
 * in any order.  Zapped faces cannot be used in further mesh operations!
 */
    fun __gl_meshZapFace(fZap: GLUface) {
        val eStart: GLUhalfEdge = fZap.anEdge ?: error("fZap.anEdge == null")
        var e: GLUhalfEdge
        var eNext: GLUhalfEdge
        var eSym: GLUhalfEdge
        val fPrev: GLUface
        val fNext: GLUface

        /* walk around face, deleting edges whose right face is also null */
        eNext = eStart.Lnext ?: error("eStart.Lnext == null")
        do {
            e = eNext
            eNext = e.Lnext ?: error(e.Lnext == null)
            e.Lface = null
            if (e.Sym!!.Lface == null) {
                /* delete the edge -- see __gl_MeshDelete above */
                if (e.Onext === e) {
                    KillVertex(e.Org ?: error("e.Org == null"), null)
                } else {
                    /* Make sure that e.Org points to a valid half-edge */
                    e.Org!!.anEdge = e.Onext
                    Splice(e, e.Sym?.Lnext ?: error("e.Sym.Lnext == null"))
                }
                eSym = e.Sym ?: error("e.Sym == null")
                if (eSym.Onext === eSym) {
                    KillVertex(eSym.Org ?: error("eSym.Org == null"), null)
                } else {
                    /* Make sure that eSym.Org points to a valid half-edge */
                    eSym.Org!!.anEdge = eSym.Onext
                    Splice(eSym, eSym.Sym?.Lnext ?: error("eSym.Sym.Lnext == null"))
                }
                KillEdge(e)
            }
        } while (e !== eStart)

        /* delete from circular doubly-linked list */
        fPrev = fZap.prev ?: error("fZap.prev == null")
        fNext = fZap.next ?: error("fZap.next == null")
        fNext.prev = fPrev
        fPrev.next = fNext
    }

    /* __gl_meshNewMesh() creates a new mesh with no edges, no vertices,
 * and no loops (what we usually call a "face").
 */
    fun __gl_meshNewMesh(): GLUmesh {
        val v: GLUvertex
        val f: GLUface
        val e: GLUhalfEdge
        val eSym: GLUhalfEdge
        val mesh: GLUmesh = GLUmesh()
        v = mesh.vHead
        f = mesh.fHead
        e = mesh.eHead
        eSym = mesh.eHeadSym
        v.prev = v
        v.next = v.prev
        v.anEdge = null
        v.data = null
        f.prev = f
        f.next = f.prev
        f.anEdge = null
        f.data = null
        f.trail = null
        f.marked = false
        f.inside = false
        e.next = e
        e.Sym = eSym
        e.Onext = null
        e.Lnext = null
        e.Org = null
        e.Lface = null
        e.winding = 0
        e.activeRegion = null
        eSym.next = eSym
        eSym.Sym = e
        eSym.Onext = null
        eSym.Lnext = null
        eSym.Org = null
        eSym.Lface = null
        eSym.winding = 0
        eSym.activeRegion = null
        return mesh
    }

    /* __gl_meshUnion( mesh1, mesh2 ) forms the union of all structures in
 * both meshes, and returns the new mesh (the old meshes are destroyed).
 */
    fun __gl_meshUnion(
        mesh1: GLUmesh,
        mesh2: GLUmesh
    ): GLUmesh {
        val f1: GLUface = mesh1.fHead
        val v1: GLUvertex = mesh1.vHead
        val e1: GLUhalfEdge = mesh1.eHead
        val f2: GLUface = mesh2.fHead
        val v2: GLUvertex = mesh2.vHead
        val e2: GLUhalfEdge = mesh2.eHead

        /* Add the faces, vertices, and edges of mesh2 to those of mesh1 */
        if (f2.next !== f2) {
            f1.prev!!.next = f2.next
            f2.next!!.prev = f1.prev
            f2.prev!!.next = f1
            f1.prev = f2.prev
        }
        if (v2.next !== v2) {
            v1.prev!!.next = v2.next
            v2.next!!.prev = v1.prev
            v2.prev!!.next = v1
            v1.prev = v2.prev
        }
        if (e2.next !== e2) {
            e1.Sym!!.next!!.Sym!!.next = e2.next
            e2.next!!.Sym!!.next = e1.Sym!!.next
            e2.Sym!!.next!!.Sym!!.next = e1
            e1.Sym!!.next = e2!!.Sym!!.next
        }
        return mesh1
    }

    /* __gl_meshDeleteMesh( mesh ) will free all storage for any valid mesh.
 */
    fun __gl_meshDeleteMeshZap(mesh: GLUmesh) {
        val fHead: GLUface = mesh.fHead
        while (fHead.next !== fHead) {
            __gl_meshZapFace(fHead.next ?: error("fHead.next == null"))
        }
        require(mesh.vHead.next === mesh.vHead)
    }

    /* __gl_meshDeleteMesh( mesh ) will free all storage for any valid mesh.
 */
    fun __gl_meshDeleteMesh(mesh: GLUmesh) {
        var f: GLUface
        var fNext: GLUface
        var v: GLUvertex
        var vNext: GLUvertex
        var e: GLUhalfEdge
        var eNext: GLUhalfEdge
        f = mesh.fHead.next ?: error("mesh.fHead.next == null")
        while (f !== mesh.fHead) {
            fNext = f.next ?: error("f.next == null")
            f = fNext
        }
        v = mesh.vHead.next ?: error("mesh.vHead.next == null")
        while (v !== mesh.vHead) {
            vNext = v.next ?: error("v.next == null")
            v = vNext
        }
        e = mesh.eHead.next ?: error("mesh.eHead.next == null")
        while (e !== mesh.eHead) {

            /* One call frees both e and e.Sym (see EdgePair above) */
            eNext = e.next ?: error("e.next == null")
            e = eNext
        }
    }

    /* __gl_meshCheckMesh( mesh ) checks a mesh for self-consistency.
 */
    fun __gl_meshCheckMesh(mesh: GLUmesh) {
        val fHead: GLUface = mesh.fHead
        val vHead: GLUvertex = mesh.vHead
        val eHead: GLUhalfEdge = mesh.eHead
        var f: GLUface
        var fPrev: GLUface
        var v: GLUvertex
        var vPrev: GLUvertex
        var e: GLUhalfEdge
        var ePrev: GLUhalfEdge
        fPrev = fHead
        fPrev = fHead
        while (fPrev.next.also { f = it?:error("it == null") } !== fHead) {
            require(f.prev === fPrev)
            e = f.anEdge ?: error("f.anEdge == null")
            do {
                require(e.Sym !== e)
                require(e.Sym!!.Sym === e)
                require(e.Lnext!!.Onext!!.Sym === e)
                require(e.Onext!!.Sym!!.Lnext === e)
                require(e.Lface === f)
                e = e.Lnext ?: error("error e.Lnext == null")
            } while (e !== f.anEdge)
            fPrev = f
        }
        require(f.prev === fPrev && f.anEdge == null && f.data == null)
        vPrev = vHead
        vPrev = vHead
        while (vPrev.next.also { v = it ?: error("it == null") } !== vHead) {
            require(v.prev === vPrev)
            e = v.anEdge ?: error("v.anEdge == null")
            do {
                require(e.Sym !== e)
                require(e.Sym!!.Sym === e)
                require(e.Lnext!!.Onext!!.Sym === e)
                require(e.Onext!!.Sym!!.Lnext === e)
                require(e.Org === v)
                e = e.Onext ?: error("e.Onext == null")
            } while (e !== v.anEdge)
            vPrev = v
        }
        require(v.prev === vPrev && v.anEdge == null && v.data == null)
        ePrev = eHead
        ePrev = eHead
        while (ePrev.next.also { e = it?:error("it == null") } !== eHead) {
            require(e.Sym!!.next === ePrev.Sym)
            require(e.Sym !== e)
            require(e.Sym!!.Sym === e)
            require(e.Org != null)
            require(e.Sym!!.Org != null)
            require(e.Lnext!!.Onext!!.Sym === e)
            require(e.Onext!!.Sym!!.Lnext === e)
            ePrev = e
        }
        require(e.Sym!!.next === ePrev.Sym && e.Sym === mesh.eHeadSym && e.Sym!!.Sym === e && e.Org == null && e.Sym!!.Org == null && e.Lface == null && e.Sym!!.Lface == null)
    }
}
