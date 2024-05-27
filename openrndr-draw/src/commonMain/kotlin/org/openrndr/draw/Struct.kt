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

fun <T : Struct<T>> Struct<T>.typeDefImpl(name: String, bufferDefinition: Boolean = false): String {
    var deps = ""
    if (!bufferDefinition) {
        for ((k, v) in values) {
            if (v is Struct<*>) {
                val type = types.getValue(k)
                @Suppress("UNCHECKED_CAST")
                deps = "$deps\n${(v as Struct<T>).typeDefImpl(type.drop(7))}"
            }
        }
    }
    return """
        ${
        if (!bufferDefinition) {
            """$deps
#ifndef STRUCT_$name
#define STRUCT_$name
struct $name {
"""
        } else {
            ""
        }
    }
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
${if (!bufferDefinition) { """};
#endif    
"""} else ""} 
"""
}

inline fun <reified T : Struct<out T>> Struct<out T>.typeDef(name: String = T::class.simpleName!!, bufferDefinition: Boolean = false): String {
    return typeDefImpl(name, bufferDefinition)
}

fun structToShaderStorageFormat(struct: Struct<*>): ShaderStorageFormat {

    fun structToShaderStorageFormatI(
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

            fun structTypeToSsboType(stype: String): BufferPrimitiveType {
                return when (tokens[0]) {
                    "int" -> BufferPrimitiveType.INT32
                    "float" -> BufferPrimitiveType.FLOAT32
                    "bool" -> BufferPrimitiveType.BOOLEAN
                    "Matrix33" -> BufferPrimitiveType.MATRIX33_FLOAT32
                    "Matrix44" -> BufferPrimitiveType.MATRIX44_FLOAT32
                    "Vector2" -> BufferPrimitiveType.VECTOR2_FLOAT32
                    "Vector3" -> BufferPrimitiveType.VECTOR3_FLOAT32
                    "Vector4" -> BufferPrimitiveType.VECTOR4_FLOAT32
                    "IntVector2" -> BufferPrimitiveType.VECTOR2_INT32
                    "IntVector3" -> BufferPrimitiveType.VECTOR3_INT32
                    "IntVector4" -> BufferPrimitiveType.VECTOR4_INT32
                    else -> error("type not supported '$stype'")
                }
            }

            val arraySize = tokens.getOrNull(1)?.toIntOrNull() ?: 1
            if (!isStruct(tokens[0])) {
                val bmt = structTypeToSsboType(tokens[0])
                ssf.primitive(name, bmt, arraySize)
            } else {
                val structValue = struct.values[name] ?: error("no value set for '$name'")
                ssf.struct(structName(tokens[0]), name, arraySize) {
                    structToShaderStorageFormatI(structValue as Struct<*>, this)
                }
            }
        }

        return ssf
    }

    val ssf = structToShaderStorageFormatI(struct)
    ssf.commit()
    return ssf
}
