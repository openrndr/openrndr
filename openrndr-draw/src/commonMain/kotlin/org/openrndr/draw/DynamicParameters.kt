package org.openrndr.draw

/**
 * A parameter of a shade style whose value is resolved in the shader: read from a texture or computed by GLSL code.
 * Shade style code reads it like any parameter, as `p_<name>`.
 *
 * Changing the texture of a [TextureParameter] or the values of the parameters of a [GlslParameter] does not
 * recompile shaders, changing the code or the coordinate does.
 *
 * @see textureParameter
 * @see glslParameter
 */
sealed class DynamicParameter {
    /** the GLSL type of the value */
    abstract val glslType: String

    /** describes the structure of the parameter, two parameters with the same signature share shaders */
    internal abstract val signature: String
}

/**
 * A parameter read from [texture] at [coordinate], a GLSL expression of the shade style language. By default the
 * texture covers the bounds of what is drawn, the way [Drawer.image] shows it, and in filters the input.
 */
class TextureParameter(
    val texture: ColorBuffer,
    override val glslType: String,
    val coordinate: String = DEFAULT_COORDINATE
) : DynamicParameter() {
    override val signature: String get() = "texture $glslType $coordinate"

    companion object {
        /** shows the texture over the bounds of what is drawn like [Drawer.image] does, and like the input of filters */
        const val DEFAULT_COORDINATE = "c_textureCoordinate"
    }
}

/**
 * A parameter computed by GLSL [code]: an expression, or a function body when it contains `return`. The code reads
 * its own parameters as `p_<name>`, which are set on this object and can be dynamic themselves.
 */
class GlslParameter(
    override val glslType: String,
    val code: String,
    internal val scope: ShadeStyle = ShadeStyle()
) : DynamicParameter(), StyleParameters by scope {
    override val signature: String get() = "glsl $glslType $code"
}

/**
 * A parameter of type [T] read from [texture] at [coordinate]: a float reads the red channel, a vector the first
 * channels, a color all channels
 */
inline fun <reified T> textureParameter(
    texture: ColorBuffer,
    coordinate: String = TextureParameter.DEFAULT_COORDINATE
): TextureParameter = TextureParameter(texture, shadeStyleTypeToGLSL(shadeStyleType<T>()), coordinate)

/**
 * A parameter of type [T] computed by GLSL [code], an expression or a function body with `return`. [parameters]
 * sets the parameters the code reads.
 *
 * ```
 * parameter("amount", glslParameter<Double>("0.5 + 0.5 * sin(p_time + c_boundsPosition.x * 10.0)") {
 *     parameter("time", seconds)
 * })
 * ```
 */
inline fun <reified T> glslParameter(code: String, parameters: StyleParameters.() -> Unit = {}): GlslParameter =
    GlslParameter(shadeStyleTypeToGLSL(shadeStyleType<T>()), code).apply(parameters)

/**
 * A shade style that is [original] with its dynamic parameters replaced by resolver functions and uniforms. Its
 * values are read from [original] when they are used, its structure is updated when the structure of [original]
 * changes.
 */
private class ResolvedShadeStyle(val original: ShadeStyle) : ShadeStyle() {
    override var parameterValues: MutableMap<String, Any> = ResolvedValues(original)
    override var bufferValues: MutableMap<String, Any>
        get() = original.bufferValues
        set(value) {
            original.bufferValues = value
        }
    override val imageValues: MutableMap<String, Array<out ImageBinding>> get() = original.imageValues
    override var textureBaseIndex: Int
        get() = original.textureBaseIndex
        set(value) {
            original.textureBaseIndex = value
        }

    /** the structure versions of the original and the scopes of its dynamic parameters */
    var versions: List<Long> = emptyList()
}

/**
 * The values of a style with dynamic parameters expanded to the uniforms of their resolvers
 */
private class ResolvedValues(private val original: ShadeStyle) : AbstractMutableMap<String, Any>() {
    override fun put(key: String, value: Any): Any? = error("the values of resolved shade styles are read only")

    override val entries: MutableSet<MutableMap.MutableEntry<String, Any>>
        get() {
            val result = LinkedHashMap<String, Any>()
            fun expand(prefix: String, values: Map<String, Any>) {
                for ((key, value) in values) {
                    when (value) {
                        is TextureParameter -> result["$prefix${key}_texture"] = value.texture
                        is GlslParameter -> expand("$prefix${key}_", value.scope.parameterValues)
                        else -> result["$prefix$key"] = value
                    }
                }
            }
            expand("", original.parameterValues)
            return result.entries
        }
}

/** the structure versions of [style] and of the scopes of its dynamic parameters, recursively */
private fun structureVersions(style: ShadeStyle): List<Long> = buildList {
    add(style.structureVersion)
    for (value in style.parameterValues.values) {
        if (value is GlslParameter) addAll(structureVersions(value.scope))
    }
}

/**
 * The values of the shade style language that resolvers can read, with their GLSL types. They are passed to
 * resolvers as arguments, the `c_` values are not defined before preambles in every shader.
 */
private val contextValues = listOf(
    "c_instance" to "int",
    "c_element" to "int",
    "c_screenPosition" to "vec2",
    "c_screenSize" to "vec2",
    "c_contourPosition" to "float",
    "c_boundsPosition" to "vec3",
    "c_boundsSize" to "vec3",
    "c_textureCoordinate" to "vec2"
)

/** the code of [value] and of its nested dynamic parameters */
private fun codeOf(value: DynamicParameter): List<String> = when (value) {
    is TextureParameter -> listOf(value.coordinate)
    is GlslParameter -> listOf(value.code) + value.scope.parameterValues.values.filterIsInstance<DynamicParameter>().flatMap { codeOf(it) }
}

/**
 * The resolver functions of a dynamic parameter, the uniforms they read and the expression that calls it
 */
private class Resolver(val functions: String, val uniformTypes: Map<String, String>, val call: String)

/**
 * Generates the resolver of the dynamic parameter [value] that is read as `p_[key]`: `<type> r_<key>(context)` with
 * the functions it calls before it. [context] are the context values the resolver and its nested resolvers read.
 */
private fun resolver(key: String, value: DynamicParameter, context: List<Pair<String, String>>): Resolver {
    val functions = StringBuilder()
    val uniformTypes = mutableMapOf<String, String>()
    // inside resolvers the context values are arguments
    val contextRenames = context.associate { (name, _) -> name to "ctx_" + name.removePrefix("c_") }
    val parameters = context.joinToString(", ") { (name, type) -> "$type ${contextRenames.getValue(name)}" }
    fun call(arguments: (String) -> String) = "r_$key(${context.joinToString(", ") { arguments(it.first) }})"

    when (value) {
        is TextureParameter -> {
            val sampler = "${key}_texture"
            uniformTypes[sampler] = when (value.texture.type.colorSampling) {
                ColorSampling.UNSIGNED_INTEGER -> "ColorBuffer_UINT"
                ColorSampling.SIGNED_INTEGER -> "ColorBuffer_SINT"
                else -> "ColorBuffer"
            }
            val channels = when (value.glslType) {
                "float", "int", "bool" -> ".r"
                "vec2", "ivec2" -> ".rg"
                "vec3", "ivec3" -> ".rgb"
                else -> ""
            }
            val coordinate = glslRename(value.coordinate, contextRenames)
            functions.appendLine("${value.glslType} r_$key($parameters) { return ${value.glslType}(texture(p_$sampler, $coordinate)$channels); }")
        }

        is GlslParameter -> {
            // the parameters of the code are namespaced with the key of this parameter, nested dynamic parameters
            // are calls of their resolvers with the context arguments of this resolver
            val scope = value.scope
            val renames = contextRenames.toMutableMap()
            for ((name, nested) in scope.parameterValues) {
                val nestedKey = "${key}_$name"
                if (nested is DynamicParameter) {
                    val nestedResolver = resolver(nestedKey, nested, context)
                    functions.append(nestedResolver.functions)
                    uniformTypes.putAll(nestedResolver.uniformTypes)
                    renames["p_$name"] = "r_$nestedKey(${context.joinToString(", ") { contextRenames.getValue(it.first) }})"
                } else {
                    uniformTypes[nestedKey] = scope.parameterTypes[name] ?: error("no type for parameter '$name'")
                    renames["p_$name"] = "p_$nestedKey"
                }
            }
            val code = glslRename(value.code, renames).trim()
            val body = if (Regex("\\breturn\\b").containsMatchIn(code)) code else "return ${value.glslType}($code);"
            functions.appendLine("${value.glslType} r_$key($parameters) {\n$body\n}")
        }
    }
    return Resolver(functions.toString(), uniformTypes, call { it })
}

/** does [code] read the identifier [name]? */
private fun reads(code: String?, name: String): Boolean =
    code != null && glslTokens(code).any { it.kind == GlslToken.Kind.IDENTIFIER && it.text == name }

/**
 * This shade style with its dynamic parameters resolved: the shade style itself when it has none, otherwise a shade
 * style that reads them through resolver functions. Shade style managers use it to make shaders.
 * @see DynamicParameter
 */
fun ShadeStyle.resolveDynamicParameters(): ShadeStyle {
    if (parameterValues.values.none { it is DynamicParameter }) {
        return this
    }
    val resolved = resolvedStyle as? ResolvedShadeStyle ?: ResolvedShadeStyle(this).also { resolvedStyle = it }
    val versions = structureVersions(this)
    if (versions == resolved.versions) {
        return resolved
    }
    resolved.versions = versions

    // the resolvers of the dynamic parameters, and the plain parameters
    val resolvers = mutableMapOf<String, Resolver>()
    resolved.parameterTypes.clear()
    for ((key, value) in parameterValues) {
        if (value is DynamicParameter) {
            // the context values read anywhere in the resolver and its nested resolvers
            val code = codeOf(value)
            val context = contextValues.filter { (name, _) -> code.any { reads(it, name) } }
            resolver(key, value, context).also {
                resolvers[key] = it
                resolved.parameterTypes.putAll(it.uniformTypes)
            }
        } else {
            resolved.parameterTypes[key] = parameterTypes[key] ?: error("no type for parameter '$key'")
        }
    }

    /**
     * The resolvers of the parameters that are read by the code of a shader stage. `p_<key>` calls the resolver with
     * the context values where it is read.
     */
    fun resolversFor(vararg code: String?): String = resolvers.filter { (key, _) ->
        code.any { reads(it, "p_$key") }
    }.entries.joinToString("") { (key, resolver) -> resolver.functions + "#define p_$key (${resolver.call})\n" }

    fun preamble(resolvers: String, preamble: String?): String? =
        if (resolvers.isEmpty()) preamble else resolvers + (preamble ?: "")

    resolved.vertexPreamble = preamble(resolversFor(vertexPreamble, vertexTransform), vertexPreamble)
    resolved.geometryPreamble = preamble(resolversFor(geometryPreamble, geometryTransform), geometryPreamble)
    resolved.fragmentPreamble = preamble(resolversFor(fragmentPreamble, fragmentTransform), fragmentPreamble)
    resolved.vertexTransform = vertexTransform
    resolved.geometryTransform = geometryTransform
    resolved.fragmentTransform = fragmentTransform

    resolved.buffers.clear(); resolved.buffers.putAll(buffers)
    resolved.bufferTypes.clear(); resolved.bufferTypes.putAll(bufferTypes)
    resolved.bufferFlags.clear(); resolved.bufferFlags.putAll(bufferFlags)
    resolved.bufferAccess.clear(); resolved.bufferAccess.putAll(bufferAccess)
    resolved.imageTypes.clear(); resolved.imageTypes.putAll(imageTypes)
    resolved.imageAccess.clear(); resolved.imageAccess.putAll(imageAccess)
    resolved.imageFlags.clear(); resolved.imageFlags.putAll(imageFlags)
    resolved.imageArrayLength.clear(); resolved.imageArrayLength.putAll(imageArrayLength)
    resolved.imageBindings.clear(); resolved.imageBindings.putAll(imageBindings)
    resolved.outputs.clear(); resolved.outputs.putAll(outputs)
    resolved.attributes.clear(); resolved.attributes.addAll(attributes)
    resolved.suppressDefaultOutput = suppressDefaultOutput
    resolved.dirty = true
    return resolved
}
