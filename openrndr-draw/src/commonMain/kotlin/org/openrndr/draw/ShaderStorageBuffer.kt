package org.openrndr.draw

import org.openrndr.internal.Driver
import kotlin.math.max

expect interface ShaderStorageBuffer {
    val session: Session?
    val format: ShaderStorageFormat

    /* Gives a read/write shadow for the shader storage buffer
    */
    val shadow: ShaderStorageBufferShadow

    fun clear()
    fun bind(base: Int)
    fun destroy()

    fun put(elementOffset: Int = 0, putter: BufferWriterStd430.() -> Unit) : Int
}

interface ShaderStorageElement {
    val member: String
    val offset: Int
    val arraySize: Int
}

enum class BufferMemberType(val componentCount: Int, val sizeInBytes: Int, val alignmentInBytes: Int) {
    UINT(1, 4, 4),
    INT(1, 4, 4),
    BOOLEAN(1, 4, 4),
    FLOAT(1, 4, 4),

    DOUBLE(1, 8, 8),

    VECTOR2_BOOLEAN(2, 8, 8),
    VECTOR2_INT(2, 8, 8),
    VECTOR2_UINT(2, 8, 8),
    VECTOR2_FLOAT(2, 8, 8),

    VECTOR2_DOUBLE(2, 16, 16),

    VECTOR3_BOOLEAN(2, 12, 16),
    VECTOR3_INT(2, 12, 16),
    VECTOR3_UINT(2, 12, 16),
    VECTOR3_FLOAT(3, 12, 16),

    VECTOR3_DOUBLE(3, 24, 32),

    VECTOR4_BOOLEAN(2, 16, 16),
    VECTOR4_INT(2, 16, 16),
    VECTOR4_UINT(2, 16, 16),
    VECTOR4_FLOAT(4, 16, 16),

    VECTOR4_DOUBLE(4, 16, 32),

    MATRIX22_FLOAT(4, 4 * 4, 8),
    MATRIX33_FLOAT(9, 9 * 4, 16),
    MATRIX44_FLOAT(16, 16 * 4, 16),
}

data class ShaderStorageMember(
    override val member: String,
    val type: BufferMemberType,
    override val arraySize: Int = 1,
    override var offset: Int = 0,
    var padding: Int = 0
): ShaderStorageElement


data class ShaderStorageStruct(
    val structName: String,
    override val member: String,
    val members: List<ShaderStorageMember>,
    override val arraySize: Int = 1,
    override var offset: Int = 0,
): ShaderStorageElement

class ShaderStorageFormat {
    var members: MutableList<ShaderStorageElement> = mutableListOf()
    private var formatSize = 0

    /**
     * The size of the [ShaderStorageFormat] in bytes
     */
    val size get() = formatSize


    /**
     * Adds a custom member to the [ShaderStorageFormat]
     */
    fun member(name: String, type: BufferMemberType, arraySize: Int = 1) {
        val item = ShaderStorageMember(name, type, arraySize)
        members.add(item)
    }

    fun struct(structName: String,  name: String, arraySize: Int = 1, builder: ShaderStorageFormat.() -> Unit) {
        val structMembers = ShaderStorageFormat().let {
            it.builder()
            it.members
        }.filterIsInstance<ShaderStorageMember>()

        val struct = ShaderStorageStruct(structName, name, structMembers, arraySize)
        members.add(struct)
    }

    override fun toString(): String {
        return "ShaderStorageFormat{" +
                "items=" + members +
                ", formatSize=" + formatSize +
                '}'
    }

    fun hasMember(name: String): Boolean = members.any { it.member == name }

    override fun hashCode(): Int {
        return members.hashCode()
    }

    fun commit() {
        val memberCount = members.sumOf { if (it is ShaderStorageStruct) it.members.size else 1 }
        val paddings = IntArray(memberCount)
        var largestAlign = 0
        var ints = 0

        var paddingIdx = -1
        /* Compute necessary padding after each field */
        for (idx in members.indices) {
            val element = members[idx]

            when(element) {
                is ShaderStorageMember -> {
                    val len = element.arraySize
                    val align = element.type.alignmentInBytes

                    largestAlign = max(largestAlign, align)

                    if (idx >= 1) {
                        val neededPadding = (align - ints % align) % align
                        paddings[paddingIdx] = neededPadding
                        ints += neededPadding
                    }

                    ints += element.type.sizeInBytes * len
                    paddingIdx++
                }
                is ShaderStorageStruct -> {
                    for (sIdx in element.members.indices) {
                        val structMember = element.members[sIdx]
                        val len = structMember.arraySize
                        val align = structMember.type.alignmentInBytes

                        largestAlign = max(largestAlign, align)

                        if (idx + sIdx >= 1) {
                            val neededPadding = (align - ints % align) % align
                            paddings[paddingIdx] = neededPadding
                            ints += neededPadding
                        }

                        ints += structMember.type.sizeInBytes * len
                        paddingIdx++
                    }
                }
            }
        }

        /* Compute padding at the end of the struct */
        val endPadding = (largestAlign - ints % largestAlign) % largestAlign
        paddings[memberCount - 1] = endPadding

        paddingIdx = 0

        for (memberIdx in members.indices) {
            val element = members[memberIdx]

            when (element) {
                is ShaderStorageMember -> {
                    val padding = paddings[paddingIdx]

                    element.offset = element.arraySize * element.type.sizeInBytes
                    element.padding = padding

                    formatSize += element.offset + padding
                    paddingIdx++
                }
                is ShaderStorageStruct -> {
                    var totalSize = 0

                    for (sIdx in element.members.indices) {
                        val structMember = element.members[sIdx]
                        val padding = paddings[paddingIdx]

                        structMember.offset = structMember.arraySize * structMember.type.sizeInBytes
                        structMember.padding = padding

                        totalSize += structMember.offset + padding
                        paddingIdx++
                    }

                    formatSize += totalSize * element.arraySize
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ShaderStorageFormat

        if (members != other.members) return false

        return true
    }
}

fun shaderStorageFormat(builder: ShaderStorageFormat.() -> Unit): ShaderStorageFormat {
    return ShaderStorageFormat().apply {
        builder()
        commit()
    }
}

fun shaderStorageBuffer(format: ShaderStorageFormat): ShaderStorageBuffer {
    return Driver.instance.createShaderStorageBuffer(format)
}

