import org.openrndr.shape.contour
import org.openrndr.shape.contours
import kotlin.test.*

class TestContourBuilder {
    @Test
    fun `contour builder supports a single line segment`() {
        val c = contour {
            moveTo(0.0, 0.0)
            lineTo(100.0, 100.0)
        }
        assertFalse(c.closed)
        assertEquals(1, c.segments.size)
    }

    @Test
    fun `contour builder supports a single quadratic curve segment`() {
        val c = contour {
            moveTo(0.0, 0.0)
            curveTo(20.0, 100.0, 100.0, 100.0)
        }
        assertFalse(c.closed)
        assertEquals(1, c.segments.size)
    }

    @Test
    fun `contour builder supports a single cubic curve segment`() {
        val c = contour {
            moveTo(0.0, 0.0)
            curveTo(100.0, 20.0, 20.0, 100.0, 100.0, 100.0)
        }
        assertFalse(c.closed)
        assertEquals(1, c.segments.size)
    }

    @Test
    fun `contour builder supports a single arc segment `() {
        val c = contour {
            moveTo(0.0, 0.0)
            arcTo(100.0, 100.0, 40.0, largeArcFlag = false, sweepFlag = false, tx = 200.0, ty = 200.0)
        }
        assertFalse(c.closed)
    }

    @Test
    fun `contour builder supports a single quadratic continueTo segment `() {
        val c = contour {
            moveTo(0.0, 0.0)
            continueTo(100.0, 100.0)
        }
        assertFalse(c.closed)
        assertEquals(1, c.segments.size)
    }

    @Test
    fun `contour builder supports a single cubic continueTo segment `() {
        val c = contour {
            moveTo(0.0, 0.0)
            continueTo(20.0, 30.0, 100.0, 100.0)
        }
        assertFalse(c.closed)
        assertEquals(1, c.segments.size)
    }

    @Test
    fun `contour builder supports a simple closed shape`() {
        val c = contour {
            moveTo(0.0, 0.0)
            lineTo(100.0, 100.0)
            lineTo(200.0, 100.0)
            close()
        }
        assertTrue(c.closed)
        assertEquals(3, c.segments.size)
    }
}

class TestContourBuilderErrors {
    @Test
    fun `detect contour builder errors`() {
        assertFailsWith<IllegalArgumentException>("detects multiple moveTo commands") {
            contour {
                moveTo(0.0, 0.0)
                lineTo(100.0, 100.0)
                moveTo(200.0, 200.0)
            }
        }
        assertFailsWith<IllegalArgumentException>("detects lineTo before moveTo") {
            contour {
                lineTo(100.0, 100.0)
            }
        }
        assertFailsWith<IllegalArgumentException>("detects quadratic curveTo before moveTo") {
            contour {
                curveTo(50.0, 50.0, 100.0, 100.0)
            }
        }
        assertFailsWith<IllegalArgumentException>("detects cubic curveTo before moveTo") {
            contour {
                curveTo(20.0, 20.0, 50.0, 50.0, 100.0, 100.0)
            }
        }
        assertFailsWith<IllegalArgumentException>("detects arcTo before moveTo") {
            contour {
                arcTo(100.0, 100.0, 40.0, largeArcFlag = false, sweepFlag = false, tx = 200.0, ty = 200.0)
            }
        }
        assertFailsWith<IllegalArgumentException>("detects quadratic continueTo before moveTo") {
            contour {
                continueTo(200.0, 200.0)
            }
        }
        assertFailsWith<IllegalArgumentException>("detects cubic continueTo before moveTo") {
            contour {
                continueTo(100.0, 300.0, 200.0, 200.0)
            }
        }
    }
}

class TestContourBuilderMultiple {
    @Test
    fun `supports multiple open contours`() {
        val c = contours {
            moveTo(0.0, 0.0)
            lineTo(200.0, 200.0)
            moveTo(300.0, 300.0)
            lineTo(400.0, 400.0)
        }
        assertEquals(2, c.size)
        assertTrue(c.all { !it.closed })
        assertTrue(c.all { it.segments.size == 1 })
    }

    @Test
    fun `supports a single open contour`() {
        val c = contours {
            moveTo(0.0, 0.0)
            lineTo(100.0, 100.0)
        }
        assertEquals(1, c.size)
        assertFalse(c.first().closed)
        assertEquals(1, c.first().segments.size)
    }

    @Test
    fun `supports an open contour followed by a closed contour`() {
        val c = contours {
            moveTo(0.0, 0.0)
            lineTo(100.0, 100.0)
            moveTo(200.0, 200.0)
            lineTo(300.0, 200.0)
            lineTo(200.0, 300.0)
            close()
        }
        assertEquals(2, c.size)
        assertFalse(c[0].closed)
        assertTrue(c[1].closed)
        assertEquals(1, c[0].segments.size)
        assertEquals(3, c[1].segments.size)
    }

    @Test
    fun `supports a closed contour followed by an open contour`() {
        val c = contours {
            moveTo(200.0, 200.0)
            lineTo(300.0, 200.0)
            lineTo(200.0, 300.0)
            close()
            moveTo(0.0, 0.0)
            lineTo(100.0, 100.0)
        }
        assertEquals(2, c.size)
        assertTrue(c[0].closed)
        assertFalse(c[1].closed)
        assertEquals(3, c[0].segments.size)
        assertEquals(1, c[1].segments.size)
    }

    @Test
    fun `supports multiple closed contours`() {
        val c = contours {
            moveTo(200.0, 200.0)
            lineTo(300.0, 200.0)
            lineTo(200.0, 300.0)
            close()
            moveTo(200.0, 200.0)
            lineTo(300.0, 200.0)
            lineTo(200.0, 300.0)
            close()
        }
        assertEquals(2, c.size)
        assertTrue(c[0].closed)
        assertTrue(c[1].closed)
        assertEquals(3, c[0].segments.size)
        assertEquals(3, c[1].segments.size)
    }
}