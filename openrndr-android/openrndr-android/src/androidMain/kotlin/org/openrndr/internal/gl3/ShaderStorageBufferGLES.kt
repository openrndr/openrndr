package org.openrndr.internal.gl3

import android.opengl.GLES30
import android.opengl.GLES31
import org.openrndr.draw.BufferWriter
import org.openrndr.draw.Session
import org.openrndr.draw.ShaderStorageBuffer
import org.openrndr.draw.ShaderStorageBufferShadow
import org.openrndr.draw.ShaderStorageFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * SSBO "view" over an existing VertexBufferGLES.
 * It does not own the GL buffer; destroy() is a no-op here (the VBO owns deletion).
 */
class ShaderStorageBufferGLES(
    val vbo: VertexBufferGLES,
    override val format: ShaderStorageFormat,
) : ShaderStorageBuffer {

    override val session: Session? get() = vbo.session

    private var destroyed = false

    override fun clear() {
        // Zero the buffer with a small zero slab to avoid allocating a giant array on big SSBOs
        GLES30.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, vbo.id)
        val chunk = 1 shl 16 // 64 KiB
        val zeros = ZeroCache.buf(chunk)
        var remaining = vbo.sizeInBytes
        var off = 0
        while (remaining > 0) {
            val n = minOf(remaining, chunk)
            zeros.limit(n)
            GLES30.glBufferSubData(GLES31.GL_SHADER_STORAGE_BUFFER, off, n, zeros)
            zeros.clear()
            off += n
            remaining -= n
        }
        GLES30.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, 0)
    }

    internal var realShadow: ShaderStorageBufferShadowGLES? = null
    override val shadow: ShaderStorageBufferShadow
        get() {
            if (destroyed) {
                throw IllegalStateException("buffer is destroyed")
            }
            if (realShadow == null) {
                realShadow = ShaderStorageBufferShadowGLES(this)
            }
            return realShadow!!
        }

    override fun write(source: ByteBuffer, writeOffset: Int) {
        val src = ensureDirect(source)
        require(writeOffset >= 0 && writeOffset + src.remaining() <= vbo.sizeInBytes) {
            "SSBO write out of bounds (offset=$writeOffset, size=${src.remaining()}, cap=${vbo.sizeInBytes})"
        }
        GLES30.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, vbo.id)
        GLES30.glBufferSubData(GLES31.GL_SHADER_STORAGE_BUFFER, writeOffset, src.remaining(), src)
        GLES30.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, 0)
    }

    override fun read(target: ByteBuffer, readOffset: Int) {
        val bytes = target.remaining()
        require(readOffset >= 0 && readOffset + bytes <= vbo.sizeInBytes) {
            "SSBO read out of bounds (offset=$readOffset, size=$bytes, cap=${vbo.sizeInBytes})"
        }
        GLES30.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, vbo.id)
        val mapped = GLES30.glMapBufferRange(
            GLES31.GL_SHADER_STORAGE_BUFFER,
            readOffset,
            bytes,
            GLES30.GL_MAP_READ_BIT
        ) as? ByteBuffer ?: error("glMapBufferRange returned null for SSBO read")
        val old = target.position()
        target.put(mapped)
        target.position(old)
        GLES30.glUnmapBuffer(GLES31.GL_SHADER_STORAGE_BUFFER)
        GLES30.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, 0)
    }

    override fun createByteBuffer(): ByteBuffer =
        ByteBuffer.allocateDirect(vbo.sizeInBytes).order(ByteOrder.nativeOrder())

    override fun put(elementOffset: Int, putter: BufferWriter.() -> Unit): Int {
        val w = shadow.writer()
        w.rewind()
        w.positionElements = elementOffset
        w.putter()

        var position = w.position
        val maxAlignmentInBytes = format.elements.maxOf { it.alignmentInBytes() }

        if (position.mod(format.size) != 0) {
            if (position.mod(maxAlignmentInBytes) != 0) {
                position += maxAlignmentInBytes - (position.mod(maxAlignmentInBytes))
            }
        }

        if (position.mod(format.size) != 0) {
            throw RuntimeException("incomplete members written (position: ${position}, size: ${format.size}). likely violating the specified shaders storage format $format")
        }
        val count = position / format.size
        shadow.uploadElements(elementOffset, count)
        return count
    }

    override fun destroy() {
        // View does not own the underlying GL buffer; nothing to delete here.
        // If you want to detach SSBO binding, you can bind base to 0 in the call site.
    }

    override fun close() = destroy()

    // ---- Helpers ----

    private fun ensureDirect(src: ByteBuffer): ByteBuffer {
        if (src.isDirect) return src
        val copy = ByteBuffer.allocateDirect(src.remaining()).order(ByteOrder.nativeOrder())
        val p = src.position()
        copy.put(src).flip()
        src.position(p)
        return copy
    }

    internal fun bindBase(bindingIndex: Int) {
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, bindingIndex, vbo.id)
    }

    internal fun bindRange(bindingIndex: Int, offsetInBytes: Int, sizeInBytes: Int) {
        require(offsetInBytes >= 0 && sizeInBytes >= 0 && offsetInBytes + sizeInBytes <= vbo.sizeInBytes)
        GLES31.glBindBufferRange(
            GLES31.GL_SHADER_STORAGE_BUFFER,
            bindingIndex,
            vbo.id,
            offsetInBytes,
            sizeInBytes
        )
    }
}