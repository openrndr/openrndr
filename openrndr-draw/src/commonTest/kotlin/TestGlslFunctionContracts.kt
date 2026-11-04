import org.openrndr.draw.glslFunction
import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestGlslFunctionContracts {
    @Test
    fun argumentTypesHaveNoNamesOrPrecisions() {
        fun types(arguments: String) = glslFunction<Double>(arguments, "0.0").argumentTypes
        assertEquals(emptyList(), types(""))
        assertEquals(listOf("vec2"), types("vec2 uv"))
        assertEquals(listOf("vec2", "float"), types("vec2 p,  highp float r"))
        assertEquals(listOf("in float[2]", "out vec3"), types("in mediump float weights[2], out vec3 color"))
        assertEquals(listOf("float[3]", "mat2[2][2]"), types("float[3] a, mat2 m[2][2]"))
    }

    @Test
    fun functionsWithTheSameTypesHaveTheSameContract() {
        val warp = glslFunction<Vector2>("vec2 uv", "uv")
        assertTrue(warp.hasContract("vec2", "vec2"))
        assertTrue(glslFunction<Vector2>("highp vec2 p", "p * 2.0").hasContract("vec2", "vec2"))
        assertFalse(warp.hasContract("float", "vec2"))
        assertFalse(warp.hasContract("vec2", "vec2", "float"))
    }
}
