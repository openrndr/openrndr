import org.openrndr.draw.ObservableHashmap
import org.openrndr.draw.composeShadeStyles
import org.openrndr.draw.shadeStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The structure version of shade styles counts changes of their structure, shade style managers cache on it
 */
class TestShadeStyleStructure {
    @Test
    fun `setting code that is the same does not change the structure`() {
        val style = shadeStyle { fragmentTransform = "x_fill.rgb = vec3(1.0);" }
        val version = style.structureVersion
        repeat(10) {
            style.fragmentTransform = "x_fill.rgb = vec3(1.0);"
            style.vertexPreamble = null
            style.suppressDefaultOutput = false
        }
        assertEquals(version, style.structureVersion)
        style.fragmentTransform = "x_fill.rgb = vec3(0.0);"
        assertTrue(style.structureVersion > version)
    }

    @Test
    fun `setting a parameter of the same type does not change the structure`() {
        val style = shadeStyle { parameter("amount", 0.5) }
        val version = style.structureVersion
        style.parameter("amount", 0.25)
        style.parameter("amount", 0.75)
        assertEquals(version, style.structureVersion)
        style.parameter("amount", 1)
        assertNotEquals(version, style.structureVersion)
    }

    @Test
    fun `arrays of the same size keep their type`() {
        val style = shadeStyle { parameter("values", doubleArrayOf(1.0, 2.0)) }
        val type = style.parameterTypes["values"]
        val version = style.structureVersion
        style.parameter("values", doubleArrayOf(3.0, 4.0))
        assertTrue(type === style.parameterTypes["values"])
        assertEquals(version, style.structureVersion)
        style.parameter("values", doubleArrayOf(3.0, 4.0, 5.0))
        assertEquals("float, 3", style.parameterTypes["values"])
    }

    @Test
    fun `a composition updates its structure version when a member changes`() {
        val a = shadeStyle { fragmentTransform = "x_fill.r = 1.0;" }
        val b = shadeStyle { fragmentTransform = "x_fill.g = 1.0;" }
        val composed = composeShadeStyles(a, b)
        val version = composed.structureVersion
        assertEquals(version, composed.structureVersion)
        a.fragmentTransform = "x_fill.r = 0.5;"
        // the structure version is read without reading dirty first
        assertTrue(composed.structureVersion > version)
        assertTrue(composed.fragmentTransform!!.contains("0.5"))
    }

    @Test
    fun `the values of a composition are namespaced views of its members`() {
        val a = shadeStyle { parameter("amount", 0.5) }
        val b = shadeStyle { parameter("amount", 0.25) }
        val composed = composeShadeStyles(a, b)
        composed.parameter("fill", 1.0)
        assertEquals(setOf("fill", "s0_amount", "s1_amount"), composed.parameterValues.keys)
        assertEquals(3, composed.parameterValues.size)
        // the keys are the same strings every time they are read
        val first = composed.parameterValues.keys.first { it == "s0_amount" }
        val second = composed.parameterValues.keys.first { it == "s0_amount" }
        assertTrue(first === second)
        a.parameter("amount", 0.75)
        assertEquals(0.75f, composed.parameterValues["s0_amount"])
        composed.parameterValues.entries.first { it.key == "s1_amount" }.setValue(0.125f)
        assertEquals(0.125f, b.parameterValues["amount"])
    }

    @Test
    fun `observable maps signal changes only`() {
        var changes = 0
        val map = ObservableHashmap(mutableMapOf<String, String>()) { changes++ }
        map["a"] = "x"
        map["a"] = "x"
        map.remove("b")
        assertEquals(1, changes)
        map["a"] = "y"
        map.remove("a")
        assertEquals(3, changes)
    }
}
