package org.openrndr.draw

/**
 * The values of the members of a [ComposedShadeStyle] under their namespaced keys, `amount` of member 1 is
 * `s1_amount`. Reads and writes go to the members, so values changed on a member are used by the composition.
 */
private class NamespacedValues<V>(
    private val members: List<ShadeStyle>,
    private val select: (ShadeStyle) -> MutableMap<String, V>
) : AbstractMutableMap<String, V>() {

    private fun member(key: String): Pair<MutableMap<String, V>, String>? {
        val separator = key.indexOf('_')
        if (!key.startsWith("s") || separator < 2) return null
        val index = key.substring(1, separator).toIntOrNull() ?: return null
        return members.getOrNull(index)?.let { select(it) to key.substring(separator + 1) }
    }

    override fun put(key: String, value: V): V? {
        val (map, name) = member(key) ?: error("'$key' is not a namespaced key of a composed shade style")
        return map.put(name, value)
    }

    override val entries: MutableSet<MutableMap.MutableEntry<String, V>>
        get() {
            val result = LinkedHashMap<String, V>()
            for ((index, member) in members.withIndex()) {
                for ((key, value) in select(member)) {
                    result[namespaced(index, key)] = value
                }
            }
            return result.entries
        }

    override fun get(key: String): V? = member(key)?.let { (map, name) -> map[name] }
}

private fun namespace(index: Int) = "s${index}_"
private fun namespaced(index: Int, name: String) = namespace(index) + name

/**
 * A shade style made of [members] that are applied in order, made with `a + b` or [composeShadeStyles]. Every member keeps its own namespace: its parameters,
 * buffers and images, and the functions, variables, structs and macros declared in its preambles are prefixed with
 * `s<index>_`, so members can use the same names, and the same shade style can be a member more than once. The
 * transforms of the members are applied in order, each in its own scope, `x_fill` and the other `x_` values flow
 * from one member to the next.
 *
 * Changes to the members are used by the composition: values of parameters directly, changes of structure when the
 * composition is drawn next.
 */
class ComposedShadeStyle(val members: List<ShadeStyle>) : ShadeStyle() {

    override var parameterValues: MutableMap<String, Any> = NamespacedValues(members) { it.parameterValues }

    override var bufferValues: MutableMap<String, Any> = NamespacedValues(members) { it.bufferValues }

    override val imageValues: MutableMap<String, Array<out ImageBinding>> = NamespacedValues(members) { it.imageValues }

    private var memberVersions = emptyList<Long>()

    override var dirty: Boolean
        get() {
            update()
            return super.dirty
        }
        set(value) {
            super.dirty = value
        }

    init {
        update()
    }

    /**
     * Generates the namespaced code and structure when the structure of a member changed
     */
    private fun update() {
        val versions = members.map { it.structureVersion }
        if (versions == memberVersions) return
        memberVersions = versions

        val vertexPreambles = mutableListOf<String>()
        val geometryPreambles = mutableListOf<String>()
        val fragmentPreambles = mutableListOf<String>()
        val vertexTransforms = mutableListOf<String>()
        val geometryTransforms = mutableListOf<String>()
        val fragmentTransforms = mutableListOf<String>()

        parameterTypes.clear()
        buffers.clear(); bufferTypes.clear(); bufferFlags.clear(); bufferAccess.clear()
        imageTypes.clear(); imageAccess.clear(); imageFlags.clear(); imageArrayLength.clear(); imageBindings.clear()
        outputs.clear()
        attributes.clear()

        for ((index, member) in members.withIndex()) {
            val preambles = listOfNotNull(member.vertexPreamble, member.geometryPreamble, member.fragmentPreamble)
            val renames = buildMap {
                for (name in preambles.flatMap { glslTopLevelNames(it) }) put(name, namespaced(index, name))
                for (name in member.parameterTypes.keys) put("p_$name", "p_" + namespaced(index, name))
                for (name in member.imageTypes.keys) put("p_$name", "p_" + namespaced(index, name))
                for (name in member.bufferTypes.keys + member.buffers.keys) put("b_$name", "b_" + namespaced(index, name))
            }
            member.vertexPreamble?.let { vertexPreambles.add(glslRename(it, renames)) }
            member.geometryPreamble?.let { geometryPreambles.add(glslRename(it, renames)) }
            member.fragmentPreamble?.let { fragmentPreambles.add(glslRename(it, renames)) }
            member.vertexTransform?.let { vertexTransforms.add(scope(glslRename(it, renames))) }
            member.geometryTransform?.let { geometryTransforms.add(scope(glslRename(it, renames))) }
            member.fragmentTransform?.let { fragmentTransforms.add(scope(glslRename(it, renames))) }

            for ((name, type) in member.parameterTypes) parameterTypes[namespaced(index, name)] = type
            for ((name, value) in member.buffers) buffers[namespaced(index, name)] = value
            for ((name, value) in member.bufferTypes) bufferTypes[namespaced(index, name)] = value
            for ((name, value) in member.bufferFlags) bufferFlags[namespaced(index, name)] = value
            for ((name, value) in member.bufferAccess) bufferAccess[namespaced(index, name)] = value
            for ((name, value) in member.imageTypes) imageTypes[namespaced(index, name)] = value
            for ((name, value) in member.imageAccess) imageAccess[namespaced(index, name)] = value
            for ((name, value) in member.imageFlags) imageFlags[namespaced(index, name)] = value
            for ((name, value) in member.imageArrayLength) imageArrayLength[namespaced(index, name)] = value
            for ((name, value) in member.imageBindings) imageBindings[namespaced(index, name)] = value
            // outputs are shared by the members, they write to the same render target attachments
            outputs.putAll(member.outputs)
            attributes.addAll(member.attributes)
        }

        vertexPreamble = vertexPreambles.joinOrNull()
        geometryPreamble = geometryPreambles.joinOrNull()
        fragmentPreamble = fragmentPreambles.joinOrNull()
        vertexTransform = vertexTransforms.joinOrNull()
        geometryTransform = geometryTransforms.joinOrNull()
        fragmentTransform = fragmentTransforms.joinOrNull()
        suppressDefaultOutput = members.any { it.suppressDefaultOutput }
        super.dirty = true
    }
}

private fun scope(code: String) = "{\n$code\n}"

private fun List<String>.joinOrNull() = if (isEmpty()) null else joinToString("\n")

/**
 * Composes [styles] into a shade style that applies them in order, each in its own namespace
 * @see ComposedShadeStyle
 */
fun composeShadeStyles(vararg styles: ShadeStyle): ShadeStyle = ComposedShadeStyle(styles.toList())
