@file:JvmName("DDSReaderFunctions")
package org.openrndr.dds
import org.openrndr.utils.buffer.MPPBuffer
import java.io.File
import java.io.InputStream
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun loadDDS(file: File) : DDSData {
    val ba = file.readBytes()
    return loadDDS(MPPBuffer(newByteBuffer(ba)))
}

fun loadDDS(buffer: ByteBuffer) : DDSData {
    return loadDDS(MPPBuffer(buffer))
}

fun loadDDS(inputStream: InputStream): DDSData {
    val ba = ByteArray(inputStream.available())
    inputStream.read(ba)
    return loadDDS(MPPBuffer(newByteBuffer(ba)))
}

private fun newByteBuffer(data: ByteArray): ByteBuffer {
    val buffer = ByteBuffer.allocateDirect(data.size).order(ByteOrder.nativeOrder())
    buffer.put(data)
    (buffer as Buffer).flip()
    return buffer
}

