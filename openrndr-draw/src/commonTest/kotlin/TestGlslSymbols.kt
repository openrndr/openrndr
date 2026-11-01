import org.openrndr.draw.glslRename
import org.openrndr.draw.glslTopLevelNames
import kotlin.test.Test
import kotlin.test.assertEquals

class TestGlslSymbols {
    @Test
    fun findsTopLevelNames() {
        val code = """
            |#define SCALE 2.0
            |#define WAVE(x) sin(x)
            |precision highp float;
            |layout(location = 0) out vec4 o_extra;
            |uniform float u_time;
            |in vec3 va_custom;
            |const float a = 1.0, b[2] = float[](1.0, 2.0);
            |struct Light { vec3 color; float power; } sun;
            |float noise(vec2 p);
            |// float commented(vec2 p) { return 0.0; }
            |vec3 shade(vec3 n, Light light) {
            |    float local = dot(n, vec3(0.0, 1.0, 0.0));
            |    return light.color * local;
            |}
            |float noise(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453); }
        """.trimMargin()
        assertEquals(
            setOf("SCALE", "WAVE", "o_extra", "u_time", "va_custom", "a", "b", "Light", "sun", "noise", "shade"),
            glslTopLevelNames(code)
        )
    }

    @Test
    fun renamesIdentifiersButNotFields() {
        val code = "float noise(vec2 p) { return p.x * noise(p.yx) + p_amount; } // noise"
        assertEquals(
            "float s1_noise(vec2 p) { return p.x * s1_noise(p.yx) + p_s1_amount; } // noise",
            glslRename(code, mapOf("noise" to "s1_noise", "p_amount" to "p_s1_amount", "x" to "nope"))
        )
    }

    @Test
    fun renamesInDirectives() {
        assertEquals("#define s0_SCALE (p_s0_amount * 2.0)\n", glslRename("#define SCALE (p_amount * 2.0)\n",
            mapOf("SCALE" to "s0_SCALE", "p_amount" to "p_s0_amount")))
    }
}
