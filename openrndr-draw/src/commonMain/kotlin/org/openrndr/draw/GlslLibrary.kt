package org.openrndr.draw

/**
 * GLSL [code] that is shared by shade styles, compute styles and dynamic parameters: functions, constants, structs
 * and macros, such as noise functions. Styles and GLSL parameters and functions declare the libraries they use, the
 * code of a library is emitted once in every shader stage that reads one of its names, after the libraries in
 * [dependencies] and before the preamble of the style.
 *
 * Libraries are identified by [name]: libraries with the same name and code are emitted once, libraries with the same
 * name and other code are an error. The names a library declares are not namespaced, they should be unique, for
 * example by prefixing them with the name of the library. Library code is emitted before the shade style language
 * values (`c_`) are defined in some shaders, it should not read them.
 *
 * ```
 * val hash = glslLibrary("hash", "float hash_21(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453); }")
 * val style = shadeStyle {
 *     libraries += hash
 *     fragmentTransform = "x_fill.rgb = vec3(hash_21(c_boundsPosition.xy));"
 * }
 * ```
 */
class GlslLibrary(val name: String, val code: String, val dependencies: List<GlslLibrary> = emptyList()) {
    /** describes the library and its dependencies, libraries with the same signature are the same */
    internal val signature: String by lazy {
        "$name[${dependencies.joinToString(",") { it.signature }}]{$code}"
    }

    /** the names declared at the top level of the code */
    internal val names: Set<String> by lazy { glslTopLevelNames(code) }

    override fun toString(): String = "GlslLibrary($name)"
}

/**
 * A [GlslLibrary] named [name] with [code] that uses [dependencies]
 */
fun glslLibrary(name: String, code: String, vararg dependencies: GlslLibrary): GlslLibrary =
    GlslLibrary(name, code, dependencies.toList())

/** the signatures of [libraries], empty when there are none */
internal fun librariesSignature(libraries: List<GlslLibrary>): String =
    if (libraries.isEmpty()) "" else " uses " + libraries.joinToString(",") { it.signature }

/**
 * [libraries] and their dependencies by name
 * @throws IllegalArgumentException when two libraries have the same name and a different code or dependencies
 */
internal fun collectLibraries(libraries: List<GlslLibrary>): Map<String, GlslLibrary> {
    val result = LinkedHashMap<String, GlslLibrary>()
    fun collect(library: GlslLibrary) {
        val existing = result[library.name]
        if (existing != null) {
            require(existing === library || existing.signature == library.signature) {
                "two GLSL libraries are named '${library.name}' but have different code or dependencies"
            }
            return
        }
        library.dependencies.forEach { collect(it) }
        result[library.name] = library
    }
    libraries.forEach { collect(it) }
    return result
}

/**
 * The code of the [libraries] that [code] reads, with their dependencies, dependencies before the libraries that use
 * them. [libraries] are collected by [collectLibraries], their order is kept otherwise.
 */
internal fun libraryCode(libraries: Map<String, GlslLibrary>, code: List<String?>): String {
    if (libraries.isEmpty()) return ""
    val identifiers = code.filterNotNull().flatMapTo(mutableSetOf()) { c ->
        glslTokens(c).filter { it.kind == GlslToken.Kind.IDENTIFIER }.map { it.text }
    }
    val emitted = LinkedHashSet<GlslLibrary>()
    fun emit(library: GlslLibrary) {
        if (library in emitted) return
        library.dependencies.forEach { emit(libraries.getValue(it.name)) }
        emitted.add(library)
    }
    for (library in libraries.values) {
        if (library.names.any { it in identifiers }) emit(library)
    }
    return emitted.joinToString("") { "// -- library ${it.name}\n${it.code.trimEnd()}\n" }
}
