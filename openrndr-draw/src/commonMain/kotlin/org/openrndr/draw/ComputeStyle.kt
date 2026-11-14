package org.openrndr.draw

import org.openrndr.draw.font.BufferAccess
import org.openrndr.draw.font.BufferFlag
import org.openrndr.internal.Driver
import org.openrndr.math.IntVector3
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmRecord

@JvmRecord
data class ComputeStructure(
    val structDefinitions: String? = null,
    val uniforms: String? = null,
    val buffers: String? = null,
    val computeTransform: String,
    val computePreamble: String,
    val workGroupSize: IntVector3
)


/**
 * ComputeStyle class
 * @since 0.4.4
 */
class ComputeStyle : StyleParameters, StyleBufferBindings, StyleImageBindings {
    /**
     * Counts the changes of the structure of this compute style: its code, libraries, work group size and parameter
     * types. Compute style managers key the structures of a style on it and on its bindings.
     */
    var structureVersion = 0L
        internal set

    var computePreamble: String = ""
        set(value) {
            if (field != value) {
                field = value
                structureVersion++
            }
        }

    var computeTransform: String = ""
        set(value) {
            if (field != value) {
                field = value
                structureVersion++
            }
        }

    /**
     * The GLSL libraries the code of this compute style uses, their code is emitted before the preamble when it reads
     * their names
     * @see GlslLibrary
     */
    var libraries: List<GlslLibrary> = emptyList()
        set(value) {
            if (field != value) {
                field = value
                structureVersion++
            }
        }

    override var textureBaseIndex: Int = 2

    /**
     * The size of the compute work group
     */
    var workGroupSize = IntVector3(1, 1, 1)
        set(value) {
            if (field != value) {
                field = value
                structureVersion++
            }
        }

    override var parameterValues: MutableMap<String, Any> = mutableMapOf()
    override var parameterTypes: ObservableHashmap<String, String> = ObservableHashmap(mutableMapOf()) { structureVersion++ }

    /** the structure version for which [hasDynamicParameters] was determined */
    internal var dynamicCheckVersion = -1L
    internal var hasDynamicParameters = false

    /** this compute style with its dynamic parameters resolved, see [resolveDynamicParameters] */
    internal var resolvedStyle: ComputeStyle? = null
    internal var resolvedVersions: List<Any> = emptyList()
    internal var resolvedSignature: String? = null

    override var bufferValues = mutableMapOf<String, Any>()
    override val buffers = mutableMapOf<String, String>()
    override val bufferTypes = mutableMapOf<String, String>()
    override val bufferAccess = mutableMapOf<String, BufferAccess>()
    override val bufferFlags: MutableMap<String, Set<BufferFlag>> = mutableMapOf()

    override val imageBindings: MutableMap<String, Int> = mutableMapOf()
    override val imageTypes: MutableMap<String, String> = mutableMapOf()
    override val imageValues: MutableMap<String, Array<out ImageBinding>> = mutableMapOf()
    override val imageAccess: MutableMap<String, ImageAccess> = mutableMapOf()
    override val imageFlags: MutableMap<String, Set<ImageFlag>> = mutableMapOf()
    override val imageArrayLength: MutableMap<String, Int> = mutableMapOf()
}

@OptIn(ExperimentalContracts::class)
fun computeStyle(builder: ComputeStyle.() -> Unit): ComputeStyle {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }
    val computeStyle = ComputeStyle()
    computeStyle.builder()
    return computeStyle
}

val computeStyleManager by lazy { Driver.instance.createComputeStyleManager() }
fun ComputeStyle.execute(width: Int = 1, height: Int = 1, depth: Int = 1) {
    val cs = computeStyleManager.shader(this, "compute-style")
    cs.execute(width, height, depth)
}

