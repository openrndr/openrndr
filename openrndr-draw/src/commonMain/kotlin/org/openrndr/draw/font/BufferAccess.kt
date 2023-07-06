package org.openrndr.draw.font


enum class BufferFlag {
    VOLATILE,
    COHERENT,
    RESTRICT
}
enum class BufferAccess {
    READ,
    READ_WRITE,
    WRITE
}