package org.openrndr.draw

import org.openrndr.draw.font.BufferAccess
import org.openrndr.draw.font.BufferFlag
import org.openrndr.internal.Driver
import org.openrndr.math.IntVector3
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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
    var computePreamble: String = ""
    var computeTransform: String = ""

    override var textureBaseIndex: Int = 0

    /**
     * The size of the compute work group
     */
    var workGroupSize = IntVector3(1, 1, 1)
    private var dirty = true
    override var parameterValues: MutableMap<String, Any> = mutableMapOf()
    override var parameterTypes: ObservableHashmap<String, String> = ObservableHashmap(mutableMapOf()) { dirty = true }

    override var bufferValues = mutableMapOf<String, Any>()
    override val buffers = mutableMapOf<String, String>()
    override val bufferTypes = mutableMapOf<String, String>()
    override val bufferAccess = mutableMapOf<String, BufferAccess>()
    override val bufferFlags: MutableMap<String, Set<BufferFlag>> = mutableMapOf()

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

