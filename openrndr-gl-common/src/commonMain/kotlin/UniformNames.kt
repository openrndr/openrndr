package org.openrndr.internal.glcommon

/**
 * The names of the uniforms and bindings of style parameters, made once for every key: the keys of the parameter
 * maps of styles are the same strings from draw to draw, so they are looked up without building names
 */
class UniformNames {
    private val parameters = HashMap<String, String>()
    private val shaderStorageBlocks = HashMap<String, String>()
    private val atomicCounters = HashMap<String, String>()
    private val elements = HashMap<String, Array<String>>()
    private val fields = HashMap<String, HashMap<String, String>>()

    /** `p_<key>`, the uniform of a parameter or the image unit of an image binding */
    fun parameter(key: String): String = parameters.getOrPut(key) { "p_$key" }

    /** `B_<key>`, the block of a shader storage buffer */
    fun shaderStorageBlock(key: String): String = shaderStorageBlocks.getOrPut(key) { "B_$key" }

    /** `b_<key>[0]`, the first atomic counter of an atomic counter buffer */
    fun atomicCounter(key: String): String = atomicCounters.getOrPut(key) { "b_$key[0]" }

    /** `<name>[<index>]`, an element of the array uniform [name] */
    fun element(name: String, index: Int): String {
        val names = elements[name]
        if (names != null && index < names.size) return names[index]
        val grown = Array(maxOf(index + 1, (names?.size ?: 0) * 2, 4)) { names?.getOrNull(it) ?: "$name[$it]" }
        elements[name] = grown
        return grown[index]
    }

    /** `<name>.<field>`, a field of the struct uniform [name] */
    fun field(name: String, field: String): String =
        fields.getOrPut(name) { HashMap() }.getOrPut(field) { "$name.$field" }
}
