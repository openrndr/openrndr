package org.openrndr.draw

import kotlin.reflect.KProperty

/**
 * Struct definition
 */
open class Struct<T : Struct<T>> {
    val types = mutableMapOf<String, String>()
    val values = mutableMapOf<String, Any>()
    val names = mutableListOf<String>()

    inner class Field<F : Any>(val type: String) {
        operator fun provideDelegate(any: Any?, kProperty: KProperty<*>): Field<F> {
            types[kProperty.name] = type
            names.add(kProperty.name)
            return this
        }

        operator fun getValue(any: Any?, kProperty: KProperty<*>): F {
            @Suppress("UNCHECKED_CAST")
            return values[kProperty.name] as F
        }

        operator fun setValue(any: Any?, kProperty: KProperty<*>, value: F) {
            values[kProperty.name] = value
        }
    }

    inner class ArrayField<F : Any>(val type: String, val length: Int) {
        operator fun provideDelegate(any: Any?, kProperty: KProperty<*>): ArrayField<F> {
            types[kProperty.name] = "$type, $length"
            names.add(kProperty.name)
            return this
        }

        operator fun getValue(any: Any?, kProperty: KProperty<*>): Array<F> {
            @Suppress("UNCHECKED_CAST")
            return values[kProperty.name] as Array<F>
        }

        operator fun setValue(any: Any?, kProperty: KProperty<*>, value: Array<F>) {
            values[kProperty.name] = value
        }
    }

    /**
     * define a struct field of type [F]
     */
    inline fun <reified F : Any> field(): Field<F> {
        val type = shadeStyleTypeOrNull<F>() ?: "struct ${F::class.simpleName}"
        return Field(type)
    }

    /**
     * define a struct field that is an array of type [F] and size [length]
     */
    inline fun <reified F : Any> arrayField(length: Int): ArrayField<F> {
        return ArrayField(shadeStyleType<F>(), length)
    }
}

fun <T : Struct<T>> Struct<T>.typeDefImpl(name: String): String {
    var deps = ""
    for ((k, v) in values) {
        if (v is Struct<*>) {
            val type = types.getValue(k)
            @Suppress("UNCHECKED_CAST")
            deps = "$deps\n${(v as Struct<T>).typeDefImpl(type.drop(7))}"
        }
    }

    return """$deps
#ifndef STRUCT_$name
#define STRUCT_$name
struct $name {
${
        types.toList().joinToString("\n") {
            val tokens = it.second.split(", ")
            if (tokens.size == 1) {
                "${shadeStyleTypeToGLSL(it.second)} ${it.first};"
            } else {
                "${shadeStyleTypeToGLSL(tokens[0])} ${it.first}[${tokens[1]}];"
            }
        }.prependIndent("    ")
    }
};
#endif
"""
}

inline fun <reified T : Struct<T>> Struct<T>.typeDef(name: String = T::class.simpleName!!): String {
    return typeDefImpl(name)
}

fun structToShaderStorageFormat(
    struct: Struct<*>,
    ssf: ShaderStorageFormat = ShaderStorageFormat()
): ShaderStorageFormat {
    for (name in struct.names) {
        val type = struct.types[name] ?: error("no type for field '$name'")
        val tokens = type.split(", ")

        fun isStruct(stype: String): Boolean {
            val stokens = stype.split(" ")
            return stokens[0] == "struct"
        }

        fun structName(stype: String): String {
            return stype.split(" ")[1]
        }

        fun structTypeToSsboType(stype: String): BufferMemberType {
            return when (tokens[0]) {
                "int" -> BufferMemberType.INT
                "float" -> BufferMemberType.FLOAT
                "bool" -> BufferMemberType.BOOLEAN
                "Matrix33" -> BufferMemberType.MATRIX33_FLOAT
                "Matrix44" -> BufferMemberType.MATRIX44_FLOAT
                "Vector2" -> BufferMemberType.VECTOR2_FLOAT
                "Vector3" -> BufferMemberType.VECTOR3_FLOAT
                "Vector4" -> BufferMemberType.VECTOR4_FLOAT
                "IntVector2" -> BufferMemberType.VECTOR2_INT
                "IntVector3" -> BufferMemberType.VECTOR3_INT
                "IntVector4" -> BufferMemberType.VECTOR4_INT
                else -> error("type not supported '$stype'")
            }
        }

        val arraySize = tokens.getOrNull(1)?.toIntOrNull() ?: 1
        if (!isStruct(tokens[0])) {
            val bmt = structTypeToSsboType(tokens[0])
            ssf.member(name, bmt, arraySize)
        } else {
            val structValue = struct.values[name] ?: error("no value set for '$name'")
            ssf.struct(structName(tokens[0]), name, arraySize) {
                structToShaderStorageFormat(structValue as Struct<*>, this)
            }
        }
    }
    return ssf
}