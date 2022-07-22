package org.openrndr.internal.nullgl

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.*
import org.openrndr.utils.buffer.MPPBuffer
import java.nio.ByteBuffer

class BufferWriterNullGL : BufferWriter() {
    override fun write(v: Int) {

    }

    override fun write(v: IntVector2) {
    }

    override fun write(v: IntVector3) {
    }

    override fun write(v: IntVector4) {

    }

    override fun write(v: Byte) {

    }

    override fun write(v: Short) {

    }

    override fun write(v: Vector3) {
    }

    override fun write(v: Vector2) {
    }

    override fun write(v: Vector4) {
    }

    override fun write(v: Matrix33) {
    }

    override fun write(v: Matrix44) {
    }

    override fun write(v: Float) {
    }

    override fun write(x: Float, y: Float) {
    }

    override fun write(x: Float, y: Float, z: Float) {
    }

    override fun write(x: Float, y: Float, z: Float, w: Float) {
    }

    override fun write(v: ColorRGBa) {
    }

    override fun write(a: FloatArray, offset: Int, size: Int) {
    }

    override fun copyBuffer(sourceBuffer: ByteBuffer, sourceOffset: Int, sourceSizeInBytes: Int) {

    }

    override fun write(vararg v: Vector3) {

    }

    override fun rewind() {
    }
    override var position: Int = 0
    override var positionElements: Int = 0
}

class VertexBufferShadowNullGL(override val vertexBuffer: VertexBuffer) : VertexBufferShadow {

    override fun upload(offsetInBytes: Int, sizeInBytes: Int) {
    }

    override fun download() {
    }

    override fun destroy() {
    }

    override fun writer(): BufferWriter {
        return BufferWriterNullGL()
    }
}

class VertexBufferNullGL(override val vertexFormat: VertexFormat, override val vertexCount: Int, override val session: Session?) : VertexBuffer() {
    override val shadow: VertexBufferShadow
        get() = VertexBufferShadowNullGL(this)

    override fun write(data: ByteBuffer, offset: Int) {
    }

    override fun write(source: MPPBuffer, targetByteOffset: Int, sourceByteOffset: Int, byteLength: Int) {
        TODO("Not yet implemented")
    }

    override fun read(data: ByteBuffer, offset: Int) {
    }

    override fun destroy() {
    }
}