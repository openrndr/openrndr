package org.openrndr.draw

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class TestVertexFormat {

    @Test
    fun testStd430Layout() {
        val f0 = vertexFormat {
            position(3)
        }
        assertFalse(f0.isInStd430Layout)

        val f1 = vertexFormat {
            position(3)
            paddingFloat(1)
        }
        assertTrue(f1.isInStd430Layout)

        val f2 = vertexFormat {
            textureCoordinate(2)
            position(3)
        }
        assertFalse(f2.isInStd430Layout)


        val f3 = vertexFormat {
            textureCoordinate(2)
            position(3)
            padding(1)
        }
        assertFalse(f3.isInStd430Layout)

        val f4 = vertexFormat {
            textureCoordinate(2)
            paddingFloat(2)
            position(3)
            paddingFloat(1)
        }
        assertTrue(f4.isInStd430Layout)
    }
}