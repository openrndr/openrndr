package org.openrndr.draw

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class TestVertexFormat {
    @Test
    fun testNoLayout() {
        val f0 = vertexFormat {
            position(3)
        }
        assertEquals(12, f0.size)
        assertEquals(0, f0.items[0].offset)

        val f1 = vertexFormat {
            position(3)
            normal(3)
        }
        assertEquals(0, f1.items[0].offset)
        assertEquals(12, f1.items[1].offset)
        assertEquals(24, f1.size)

        val f2 = vertexFormat {
            textureCoordinate(2)
            position(3)
            normal(3)
        }
        assertEquals(0, f2.items[0].offset)
        assertEquals(8, f2.items[1].offset)
        assertEquals(20, f2.items[2].offset)
        assertEquals(32, f2.size)
    }

    @Test
    fun testStd430AutoLayout() {
        val f0 = vertexFormat(BufferAlignment.STD430) {
            position(3)
        }
        assertTrue(f0.isInStd430Layout)
        assertEquals(16, f0.size)

        val f1 = vertexFormat(BufferAlignment.STD430) {
            position(3)
            normal(3)
        }
        assertTrue(f1.isInStd430Layout)
        assertEquals(32, f1.size)

        val f2 = vertexFormat(BufferAlignment.STD430) {
            textureCoordinate(2)
            position(3)
            normal(3)
        }
        assertTrue(f2.isInStd430Layout)
        assertEquals(48, f2.size)
    }

    @Test
    fun testStd430LayoutCompliance() {
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