@file:Suppress("UNUSED_PARAMETER")

package org.openrndr.ktessellation

@Suppress("FunctionName")
internal abstract class PriorityQ {
    class PQnode {
        var handle = 0
    }

    class PQhandleElem {
        var key: Any? = null
        var node = 0
    }

    interface Leq {
        fun leq(key1: Any?, key2: Any?): Boolean
    }

    abstract fun pqDeletePriorityQ()
    abstract fun pqInit(): Boolean
    abstract fun pqInsert(keyNew: Any?): Int
    abstract fun pqExtractMin(): Any?
    abstract fun pqDelete(hCurr: Int)
    abstract fun pqMinimum(): Any?
    abstract fun pqIsEmpty(): Boolean //    #endif

    companion object {
        const val INIT_SIZE = 32

        //    #ifdef FOR_TRITE_TEST_PROGRAM
        //    private static boolean LEQ(PriorityQCommon.Leq leq, Object x,Object y) {
        //        return pq.leq.leq(x,y);
        //    }
        //    #else
        /* Violates modularity, but a little faster */ //    #include "geom.h"
        fun LEQ(leq: Leq, x: Any, y: Any): Boolean {
            return Geom.VertLeq(
                x as GLUvertex,
                y as GLUvertex
            )
        }

        fun pqNewPriorityQ(leq: Leq): PriorityQ {
            return PriorityQSort(leq)
        }
    }
}
