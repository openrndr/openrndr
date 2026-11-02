package org.openrndr.draw

import org.openrndr.utils.collections.LRUCache

/**
 * A parameter of a shade style whose value is resolved in the shader: read from a texture, computed by GLSL code,
 * or a GLSL function. Shade style code reads it like any parameter, as `p_<name>`, and calls a function as
 * `p_<name>(arguments)`.
 *
 * Changing the texture of a [TextureParameter] or the values of the parameters of a [GlslParameter] or
 * [GlslFunction] does not recompile shaders, changing code or coordinates does.
 *
 * @see textureParameter
 * @see glslParameter
 * @see glslFunction
 */
sealed class DynamicParameter {
    /** the GLSL type of the value */
    abstract val glslType: String

    /** describes the structure of the parameter, two parameters with the same signature share shaders */
    internal abstract val signature: String
}

/**
 * The parameters of the code of a [GlslParameter] or [GlslFunction]
 */
internal class ParameterScope : StyleParameters {
    /** counts the changes of the structure of the parameters: their names and types */
    var structureVersion = 0L
        private set

    override var parameterValues: MutableMap<String, Any> = mutableMapOf()
    override var parameterTypes: ObservableHashmap<String, String> = ObservableHashmap(mutableMapOf()) { structureVersion++ }
    override var textureBaseIndex: Int = 0
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
    override val signature: String get() = "texture $glslType ${texture.type.colorSampling} $coordinate"

    companion object {
        /** shows the texture over the bounds of what is drawn like [Drawer.image] does, and like the input of filters */
        const val DEFAULT_COORDINATE = "c_textureCoordinate"
    }
}

/**
 * A parameter computed by GLSL [code]: an expression, or a function body when it contains `return`. The code reads
 * its own parameters as `p_<name>`, which are set on this object and can be dynamic themselves.
 */
class GlslParameter internal constructor(
    override val glslType: String,
    val code: String,
    internal val scope: ParameterScope
) : DynamicParameter(), StyleParameters by scope {
    constructor(glslType: String, code: String) : this(glslType, code, ParameterScope())

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
 * A GLSL function with [arguments], a GLSL parameter list such as `vec2 p, float r`, that returns a [glslType].
 * [code] is an expression or a function body with `return`, it reads its own parameters as `p_<name>`, which are set
 * on this object and can be dynamic themselves. Shade style code calls it as `p_<name>(arguments)`.
 */
class GlslFunction internal constructor(
    override val glslType: String,
    val arguments: String,
    val code: String,
    internal val scope: ParameterScope
) : DynamicParameter(), StyleParameters by scope {
    constructor(glslType: String, arguments: String, code: String) : this(glslType, arguments, code, ParameterScope())

    override val signature: String get() = "function $glslType ($arguments) $code"

    /** the number of arguments */
    internal val arity: Int
        get() {
            var depth = 0
            var count = if (arguments.isBlank()) 0 else 1
            for (c in arguments) {
                when (c) {
                    '(', '[' -> depth++
                    ')', ']' -> depth--
                    ',' -> if (depth == 0) count++
                }
            }
            return count
        }
}

/**
 * A GLSL function that returns a [T], with [arguments], a GLSL parameter list such as `vec2 p`. [code] is an
 * expression or a function body with `return`, [parameters] sets the parameters the code reads. Shade style code calls
 * it as `p_<name>(arguments)`, which makes shade styles that take functions, such as signed distance functions:
 *
 * ```
 * parameter("shape", glslFunction<Double>("vec2 p", "length(p - 0.5) - p_radius") {
 *     parameter("radius", 0.25)
 * })
 * // in the shade style: float d = p_shape(c_boundsPosition.xy);
 * ```
 */
inline fun <reified T> glslFunction(
    arguments: String,
    code: String,
    parameters: StyleParameters.() -> Unit = {}
): GlslFunction = GlslFunction(shadeStyleTypeToGLSL(shadeStyleType<T>()), arguments, code).apply(parameters)

/**
 * A shade style that is [original] with its dynamic parameters replaced by resolver functions and uniforms. Its
 * values are read from [original] when they are used, its structure is updated when the structure of [original]
 * changes.
 */
private class ResolvedShadeStyle(val original: ShadeStyle) : ShadeStyle() {
    override var parameterValues: MutableMap<String, Any> = ResolvedValues { original.parameterValues }
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
 * The parameter values of a style, read by [source], with dynamic parameters expanded to the uniforms of their
 * resolvers
 */
private class ResolvedValues(private val source: () -> Map<String, Any>) : AbstractMutableMap<String, Any>() {
    override fun put(key: String, value: Any): Any? = error("the values of resolved shade styles are read only")

    override val entries: MutableSet<MutableMap.MutableEntry<String, Any>>
        get() {
            val result = LinkedHashMap<String, Any>()
            fun expand(prefix: String, values: Map<String, Any>) {
                for ((key, value) in values) {
                    when (value) {
                        is TextureParameter -> result["$prefix${key}_texture"] = value.texture
                        is GlslParameter -> expand("$prefix${key}_", value.scope.parameterValues)
                        is GlslFunction -> expand("$prefix${key}_", value.scope.parameterValues)
                        else -> result["$prefix$key"] = value
                    }
                }
            }
            expand("", source())
            return result.entries
        }
}

/** the structure versions of the scopes of the dynamic parameters in [values], recursively */
private fun scopeVersions(values: Map<String, Any>): List<Long> = buildList {
    for (value in values.values) {
        scopeOf(value)?.let {
            add(it.structureVersion)
            addAll(scopeVersions(it.parameterValues))
        }
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

/** the parameters of the code of a GLSL parameter or function */
private fun scopeOf(value: Any): ParameterScope? = when (value) {
    is GlslParameter -> value.scope
    is GlslFunction -> value.scope
    else -> null
}

/** the code of [value] and of its nested dynamic parameters */
private fun codeOf(value: DynamicParameter): List<String> = when (value) {
    is TextureParameter -> listOf(value.coordinate)
    is GlslParameter -> listOf(value.code)
    is GlslFunction -> listOf(value.code)
} + (scopeOf(value)?.parameterValues?.values?.filterIsInstance<DynamicParameter>()?.flatMap { codeOf(it) } ?: emptyList())

/**
 * The resolver functions of a dynamic parameter, the uniforms they read, and [definition] which defines `p_<key>`
 * for given context arguments
 */
private class Resolver(
    val functions: String,
    val uniformTypes: Map<String, String>,
    val definition: (contextArguments: List<String>) -> String
)

/**
 * Generates the resolver of the dynamic parameter [value] that is read as `p_[key]`: `r_<key>(arguments, context)`
 * with the functions it calls before it. [context] are the context values the resolver and its nested resolvers read,
 * they are the last arguments of every resolver. `p_<key>` is a macro that calls the resolver with the context
 * arguments where it is defined: the context values in shade style code, the context arguments in resolvers.
 */
private fun resolver(key: String, value: DynamicParameter, context: List<Pair<String, String>>): Resolver {
    val functions = StringBuilder()
    val uniformTypes = mutableMapOf<String, String>()
    // inside resolvers the context values are arguments
    val contextRenames = context.associate { (name, _) -> name to "ctx_" + name.removePrefix("c_") }
    val contextParameters = context.map { (name, type) -> "$type ${contextRenames.getValue(name)}" }
    val contextArguments = context.map { contextRenames.getValue(it.first) }

    /** the body of GLSL code: an expression is returned */
    fun body(code: String) = if (Regex("\\breturn\\b").containsMatchIn(code)) code else "return ${value.glslType}($code);"

    /**
     * Namespaces the parameters of the code with the key of this parameter. Nested dynamic parameters are macros that
     * call their resolvers with the context arguments of this resolver.
     */
    fun resolveScope(scope: ParameterScope): Map<String, String> {
        val renames = contextRenames.toMutableMap()
        for ((name, nested) in scope.parameterValues) {
            val nestedKey = "${key}_$name"
            renames["p_$name"] = "p_$nestedKey"
            if (nested is DynamicParameter) {
                val nestedResolver = resolver(nestedKey, nested, context)
                functions.append(nestedResolver.functions)
                functions.append(nestedResolver.definition(contextArguments))
                uniformTypes.putAll(nestedResolver.uniformTypes)
            } else {
                uniformTypes[nestedKey] = scope.parameterTypes[name] ?: error("no type for parameter '$name'")
            }
        }
        return renames
    }

    val definition: (List<String>) -> String = when (value) {
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
            functions.appendLine("${value.glslType} r_$key(${contextParameters.joinToString(", ")}) { return ${value.glslType}(texture(p_$sampler, $coordinate)$channels); }")
            ({ arguments -> "#define p_$key (r_$key(${arguments.joinToString(", ")}))\n" })
        }

        is GlslParameter -> {
            val code = glslRename(value.code, resolveScope(value.scope)).trim()
            functions.appendLine("${value.glslType} r_$key(${contextParameters.joinToString(", ")}) {\n${body(code)}\n}")
            ({ arguments -> "#define p_$key (r_$key(${arguments.joinToString(", ")}))\n" })
        }

        is GlslFunction -> {
            val code = glslRename(value.code, resolveScope(value.scope)).trim()
            val parameters = (listOf(value.arguments).filter { it.isNotBlank() } + contextParameters).joinToString(", ")
            functions.appendLine("${value.glslType} r_$key($parameters) {\n${body(code)}\n}")
            // GLSL has no variadic macros, the macro has the arity of the function
            val macroArguments = List(value.arity) { "a$it" }
            ({ arguments ->
                if (arguments.isEmpty()) "#define p_$key r_$key\n"
                else "#define p_$key(${macroArguments.joinToString(", ")}) r_$key(${(macroArguments + arguments).joinToString(", ")})\n"
            })
        }
    }
    return Resolver(functions.toString(), uniformTypes, definition)
}

/** does [code] read the identifier [name]? */
private fun reads(code: String?, name: String): Boolean =
    code != null && glslTokens(code).any { it.kind == GlslToken.Kind.IDENTIFIER && it.text == name }

/**
 * The resolvers of the dynamic parameters of a style and the types of the uniforms of the resolved style
 */
private class ParameterResolution(
    val uniformTypes: Map<String, String>,
    private val resolvers: Map<String, Pair<Resolver, List<String>>>
) {
    /**
     * The resolvers of the parameters that are read by [code], the code of a shader stage. `p_<key>` calls the
     * resolver with the context values where it is read.
     */
    fun code(vararg code: String?): String = resolvers.filter { (key, _) ->
        code.any { reads(it, "p_$key") }
    }.values.joinToString("") { (resolver, context) -> resolver.functions + resolver.definition(context) }
}

/**
 * Resolves the dynamic parameters in [values], plain parameters keep their [types]
 */
private fun resolveParameters(values: Map<String, Any>, types: Map<String, String>): ParameterResolution {
    val uniformTypes = mutableMapOf<String, String>()
    val resolvers = mutableMapOf<String, Pair<Resolver, List<String>>>()
    for ((key, value) in values) {
        if (value is DynamicParameter) {
            // the context values read anywhere in the resolver and its nested resolvers
            val code = codeOf(value)
            val context = contextValues.filter { (name, _) -> code.any { reads(it, name) } }
            resolver(key, value, context).also {
                resolvers[key] = it to context.map { (name, _) -> name }
                uniformTypes.putAll(it.uniformTypes)
            }
        } else {
            uniformTypes[key] = types[key] ?: error("no type for parameter '$key'")
        }
    }
    return ParameterResolution(uniformTypes, resolvers)
}

/**
 * Resolutions by the structure they were made for, so that equal shade styles made anew, for example every frame,
 * do not resolve their dynamic parameters again
 */
private val resolutionCache = LRUCache<String, Pair<Map<String, String>, List<String>>>(256)

/** describes the structure of parameters: their names and types, the signatures of dynamic parameters, recursively */
private fun structuralSignature(values: Map<String, Any>, types: Map<String, String>): String = buildString {
    for ((key, value) in values) {
        append(key).append('=')
        if (value is DynamicParameter) {
            append(value.signature)
            scopeOf(value)?.let { scope ->
                append('{').append(structuralSignature(scope.parameterValues, scope.parameterTypes)).append('}')
            }
        } else {
            append(types[key])
        }
        append(';')
    }
}

/**
 * The uniform types and the resolver code of every stage, the code of a stage is the list of code that reads
 * parameters in that stage
 */
private fun cachedResolution(
    values: Map<String, Any>,
    types: Map<String, String>,
    stages: List<List<String?>>
): Pair<Map<String, String>, List<String>> {
    val key = buildString {
        for (stage in stages) {
            for (code in stage) append(code ?: "").append('\u0001')
            append('\u0002')
        }
        append(structuralSignature(values, types))
    }
    return resolutionCache.get(key) ?: resolveParameters(values, types).let { resolution ->
        (resolution.uniformTypes to stages.map { resolution.code(*it.toTypedArray()) }).also { resolutionCache.set(key, it) }
    }
}

private fun withResolvers(resolvers: String, preamble: String?): String? =
    if (resolvers.isEmpty()) preamble else resolvers + (preamble ?: "")

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
    val versions = listOf(structureVersion) + scopeVersions(parameterValues)
    if (versions == resolved.versions) {
        return resolved
    }
    resolved.versions = versions

    val (uniformTypes, stageResolvers) = cachedResolution(
        parameterValues, parameterTypes,
        listOf(listOf(vertexPreamble, vertexTransform), listOf(geometryPreamble, geometryTransform), listOf(fragmentPreamble, fragmentTransform))
    )
    resolved.parameterTypes.clear()
    resolved.parameterTypes.putAll(uniformTypes)

    resolved.vertexPreamble = withResolvers(stageResolvers[0], vertexPreamble)
    resolved.geometryPreamble = withResolvers(stageResolvers[1], geometryPreamble)
    resolved.fragmentPreamble = withResolvers(stageResolvers[2], fragmentPreamble)
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

/**
 * This compute style with its dynamic parameters resolved: the compute style itself when it has none, otherwise a
 * compute style that reads them through resolver functions. Compute style managers use it to make compute shaders.
 *
 * Compute shaders have no shade style language values (`c_`), resolvers can read built-in values such as
 * `gl_GlobalInvocationID`, texture parameters need a coordinate.
 * @see DynamicParameter
 */
fun ComputeStyle.resolveDynamicParameters(): ComputeStyle {
    if (parameterValues.values.none { it is DynamicParameter }) {
        return this
    }
    val (uniformTypes, stageResolvers) = cachedResolution(
        parameterValues, parameterTypes, listOf(listOf(computePreamble, computeTransform))
    )

    val original = this
    return ComputeStyle().apply {
        computePreamble = withResolvers(stageResolvers[0], original.computePreamble) ?: ""
        computeTransform = original.computeTransform
        workGroupSize = original.workGroupSize
        textureBaseIndex = original.textureBaseIndex
        parameterTypes.putAll(uniformTypes)
        parameterValues = ResolvedValues { original.parameterValues }
        bufferValues = original.bufferValues
        buffers.putAll(original.buffers)
        bufferTypes.putAll(original.bufferTypes)
        bufferAccess.putAll(original.bufferAccess)
        bufferFlags.putAll(original.bufferFlags)
        imageBindings.putAll(original.imageBindings)
        imageTypes.putAll(original.imageTypes)
        imageValues.putAll(original.imageValues)
        imageAccess.putAll(original.imageAccess)
        imageFlags.putAll(original.imageFlags)
        imageArrayLength.putAll(original.imageArrayLength)
    }
}
