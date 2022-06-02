package org.openrndr.shape

import kotlin.test.*

class TestComposition {
    val composition = let { _ ->
        val root = GroupNode().also { it.id = "outer" }
        root.children += GroupNode().also {
            it.id = "inner"
        }
        root.children += ShapeNode(Shape.EMPTY).also {
            it.id = "shape"
        }
        Composition(root)
    }

    @Test
    fun findGroup() {
        assertEquals("outer", composition.findGroup("outer")?.id)
        assertEquals("inner", composition.findGroup("inner")?.id)
        assertNull(composition.findGroup("shape"))
    }

    @Test
    fun findShape() {
        assertEquals("shape", composition.findShape("shape")?.id)
        assertNull(composition.findShape("inner"))
        assertNull(composition.findShape("outer"))
    }

    @Test
    fun findImage() {
        assertNull(composition.findImage("inner"))
        assertNull(composition.findImage("outer"))
        assertNull(composition.findImage("shape"))
    }
}