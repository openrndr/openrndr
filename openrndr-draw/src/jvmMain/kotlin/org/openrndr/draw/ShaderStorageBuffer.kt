package org.openrndr.draw

import org.openrndr.internal.Driver
import java.nio.ByteBuffer

actual interface ShaderStorageBuffer {

    fun write(source: ByteBuffer, writeOffset: Int = 0)
    fun read(target: ByteBuffer, readOffset: Int = 0)
    actual val session: Session?
    actual val format: ShaderStorageFormat
    actual fun clear()
    actual fun bind(base: Int)
    actual fun destroy()


}





