package org.openrndr.draw

actual interface ShaderStorageBuffer {
    actual val session: Session?
    actual val format: ShaderStorageFormat
    actual fun clear()
    actual fun bind(base: Int)
    actual fun destroy()
}