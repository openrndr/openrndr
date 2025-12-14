package org.openrndr.internal.gl3

import android.opengl.GLES30
import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.draw.BufferPrimitiveType
import org.openrndr.draw.BufferReader
import org.openrndr.draw.BufferWriter
import org.openrndr.draw.ByteBufferReader
import org.openrndr.draw.Session
import org.openrndr.draw.ShaderStorageBuffer
import org.openrndr.draw.VertexBuffer
import org.openrndr.draw.VertexBufferShadow
import org.openrndr.draw.VertexElementType
import org.openrndr.draw.VertexFormat
import org.openrndr.draw.shaderStorageFormat
import org.openrndr.utils.buffer.MPPBuffer
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

private val logger = KotlinLogging.logger {}

class VertexBufferShadowGLES(override val vertexBuffer: VertexBuffer) : VertexBufferShadow {

    val buffer: ByteBuffer =
        ByteBuffer.allocateDirect(vertexBuffer.vertexCount * vertexBuffer.vertexFormat.size).apply {
            order(ByteOrder.nativeOrder())
        }

    override fun upload(offsetInBytes: Int, sizeInBytes: Int) {
        logger.trace { "uploading shadow to vertex buffer" }
        (buffer as Buffer).rewind()
        (buffer as Buffer).position(offsetInBytes)
        (buffer as Buffer).limit(offsetInBytes + sizeInBytes)
        vertexBuffer.write(buffer)
        (buffer as Buffer).limit(buffer.capacity())
    }

    override fun download() {
        (buffer as Buffer).rewind()
        vertexBuffer.read(buffer)
    }

    override fun destroy() {
        close()
    }

    override fun writer(): BufferWriter {
        return BufferWriterGLES(buffer, vertexBuffer.vertexFormat.size, vertexBuffer.vertexFormat.alignment, null)
    }

    override fun close() {
        destroy()
    }

    override fun reader(): BufferReader {
        return ByteBufferReader(buffer, vertexBuffer.vertexFormat.alignment, null)
    }
}

class VertexBufferGLES(
    override val vertexFormat: VertexFormat,
    override val vertexCount: Int,
    override val session: Session?
) : VertexBuffer() {

    override fun close() {
        destroy()
    }

    val id: Int
    val sizeInBytes: Int = vertexFormat.size * vertexCount

    private var realShadow: VertexBufferShadowGLES? = null

    internal var isDestroyed: Boolean = false

    init {
        val out = IntArray(1)
        GLES30.glGenBuffers(1, out, 0)
        id = out[0]
        require(id != 0) { "Failed to create GL buffer" }

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, id)
        // Use DYNAMIC_DRAW by default; change to STATIC later if you like
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, sizeInBytes, null, GLES30.GL_DYNAMIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    /** Uploads data.remaining() bytes starting at offsetInBytes. */
    override fun write(data: ByteBuffer, offsetInBytes: Int) {
        val src = ensureDirect(data)
        require(offsetInBytes >= 0 && offsetInBytes + src.remaining() <= sizeInBytes) {
            "write overrun: offset=$offsetInBytes, size=${src.remaining()}, cap=$sizeInBytes"
        }
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, id)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, offsetInBytes, src.remaining(), src)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    override fun read(data: ByteBuffer, offsetInBytes: Int) {
        require(!isDestroyed) { "buffer is destroyed" }
        val bytesToRead = data.remaining()
        require(offsetInBytes >= 0 && bytesToRead >= 0 && offsetInBytes + bytesToRead <= sizeInBytes) {
            "read overrun: offset=$offsetInBytes, size=$bytesToRead, cap=$sizeInBytes"
        }

        // Bind & map
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, id)
        val mapped: java.nio.Buffer? = GLES30.glMapBufferRange(
            GLES30.GL_ARRAY_BUFFER,
            offsetInBytes,
            bytesToRead,
            GLES30.GL_MAP_READ_BIT
        )
        if (mapped == null) {
            // Some drivers return null if the range can't be mapped; fall back to a slow path
            // (optional) Or throw:
            throw IllegalStateException("glMapBufferRange returned null (offset=$offsetInBytes, size=$bytesToRead)")
        }

        // Copy from mapped to data
        val src = (mapped as ByteBuffer).order(ByteOrder.nativeOrder())
        val oldDataPos = data.position()
        data.put(src)              // copies bytesToRead bytes
        data.position(oldDataPos)   // leave data position unchanged if that’s your convention

        // Unmap & unbind
        GLES30.glUnmapBuffer(GLES30.GL_ARRAY_BUFFER)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    override val shadow: VertexBufferShadow
        get() {
            if (isDestroyed) error("buffer is destroyed")
            if (realShadow == null) {
                realShadow = VertexBufferShadowGLES(this)
            }
            return realShadow ?: error("no shadow")
        }

    /**
     * Binds this VBO’s attributes with the conventional locations used by the basic pipeline:
     *  position→0, normal→1, color→2, texCoordN→3+N, others start at 8+index.
     *
     * Call this with a VAO bound (Driver ensures that).
     */
    fun bindAttributes() {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, id)
        val stride = vertexFormat.size

        vertexFormat.items.forEachIndexed { rawIndex, elem ->
            if (elem.attribute == "_") return@forEachIndexed // padding

            val (size, glType, normalized, isInteger) = glAttribOf(elem.type)
            val loc = attributeLocation(elem.attribute, rawIndex)

            if (loc < 0) return@forEachIndexed   // not used by current shader

            GLES30.glEnableVertexAttribArray(loc)
            val pointer = elem.offset

            if (isInteger) {
                GLES30.glVertexAttribIPointer(loc, size, glType, stride, pointer)
            } else {
                GLES30.glVertexAttribPointer(loc, size, glType, normalized, stride, pointer)
            }
            // divisor 0 for per-vertex data
            GLES30.glVertexAttribDivisor(loc, 0)
        }
        // leave ARRAY_BUFFER bound or unbound — driver will rebind as needed
        // GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    override fun destroy() {
        if (!isDestroyed) {
            logger.debug { "destroying vertex buffer with id $id" }

            // 1) Untrack from sessions
            session?.untrack(this)
            Session.active.untrack(this)

            // 2) Mark & drop shadow
            isDestroyed = true
            realShadow = null

            // 3) Delete the GL buffer
            val arr = intArrayOf(id)
            GLES30.glDeleteBuffers(1, arr, 0)

            // 4) Ask the driver to invalidate VAOs that reference this VBO (if supported)
            (org.openrndr.internal.Driver.driver as DriverAndroidGLES).destroyVAOsForVertexBuffer(this)

            // 5) Optional: check GL error once in debug builds
            val err = GLES30.glGetError()
            if (err != GLES30.GL_NO_ERROR) {
                logger.warn { "glDeleteBuffers($id) -> GL error $err" }
            }
        }
    }

    override fun write(
        source: MPPBuffer,
        targetByteOffset: Int,
        sourceByteOffset: Int,
        byteLength: Int
    ) {
        source.byteBuffer.position(sourceByteOffset)
        source.byteBuffer.limit(sourceByteOffset + byteLength)
        write(source.byteBuffer, targetByteOffset)
    }

    override fun shaderStorageBufferView(): ShaderStorageBuffer {
        // SSBOs require GLES 3.1
        val supportsSSBO = try {
            Class.forName("android.opengl.GLES31"); true
        } catch (_: Throwable) { false }
        if (!supportsSSBO) {
            throw UnsupportedOperationException("Shader storage buffers require OpenGL ES 3.1+")
        }

        val ssf = shaderStorageFormat {
            struct("Vertex_${vertexFormat.hashCode().toUInt()}", "vertex", vertexCount) {
                for (item in vertexFormat.items) {
                    if (item.attribute == "_") {
                        continue
                    }
                    val bpt = when (item.type) {
                        VertexElementType.INT32 -> BufferPrimitiveType.INT32
                        VertexElementType.UINT32 -> BufferPrimitiveType.UINT32
                        VertexElementType.VECTOR2_INT32 -> BufferPrimitiveType.VECTOR2_INT32
                        VertexElementType.VECTOR3_INT32 -> BufferPrimitiveType.VECTOR3_INT32
                        VertexElementType.VECTOR4_INT32 -> BufferPrimitiveType.VECTOR4_INT32
                        VertexElementType.VECTOR2_UINT32 -> BufferPrimitiveType.VECTOR2_UINT32
                        VertexElementType.VECTOR3_UINT32 -> BufferPrimitiveType.VECTOR3_UINT32
                        VertexElementType.VECTOR4_UINT32 -> BufferPrimitiveType.VECTOR4_UINT32
                        VertexElementType.FLOAT32 -> BufferPrimitiveType.FLOAT32
                        VertexElementType.VECTOR2_FLOAT32 -> BufferPrimitiveType.VECTOR2_FLOAT32
                        VertexElementType.VECTOR3_FLOAT32 -> BufferPrimitiveType.VECTOR3_FLOAT32
                        VertexElementType.VECTOR4_FLOAT32 -> BufferPrimitiveType.VECTOR4_FLOAT32
                        VertexElementType.MATRIX22_FLOAT32 -> BufferPrimitiveType.MATRIX22_FLOAT32
                        VertexElementType.MATRIX33_FLOAT32 -> BufferPrimitiveType.MATRIX33_FLOAT32
                        VertexElementType.MATRIX44_FLOAT32 -> BufferPrimitiveType.MATRIX44_FLOAT32
                        else -> error("Unsupported vertex element type: ${item.type}, it is not compatible with the STD430 layout rules.")
                    }
                    primitive(item.attribute, bpt, item.arraySize)
                }
            }
        }

        return ShaderStorageBufferGLES(this, ssf)
    }

    // --- Helpers ---

    private data class GlAttrib(val size: Int, val glType: Int, val normalized: Boolean, val isInteger: Boolean)

    private fun glAttribOf(type: VertexElementType): GlAttrib = when (type) {
        VertexElementType.FLOAT32         -> GlAttrib(1, GLES30.GL_FLOAT, false, false)
        VertexElementType.VECTOR2_FLOAT32 -> GlAttrib(2, GLES30.GL_FLOAT, false, false)
        VertexElementType.VECTOR3_FLOAT32 -> GlAttrib(3, GLES30.GL_FLOAT, false, false)
        VertexElementType.VECTOR4_FLOAT32 -> GlAttrib(4, GLES30.GL_FLOAT, false, false)

        VertexElementType.INT32           -> GlAttrib(1, GLES30.GL_INT, false, true)
        VertexElementType.VECTOR2_INT32   -> GlAttrib(2, GLES30.GL_INT, false, true)
        VertexElementType.VECTOR3_INT32   -> GlAttrib(3, GLES30.GL_INT, false, true)
        VertexElementType.VECTOR4_INT32   -> GlAttrib(4, GLES30.GL_INT, false, true)

        VertexElementType.UINT32          -> GlAttrib(1, GLES30.GL_UNSIGNED_INT, false, true)
        VertexElementType.VECTOR2_UINT32  -> GlAttrib(2, GLES30.GL_UNSIGNED_INT, false, true)
        VertexElementType.VECTOR3_UINT32  -> GlAttrib(3, GLES30.GL_UNSIGNED_INT, false, true)
        VertexElementType.VECTOR4_UINT32  -> GlAttrib(4, GLES30.GL_UNSIGNED_INT, false, true)

        VertexElementType.UINT8           -> GlAttrib(1, GLES30.GL_UNSIGNED_BYTE, true,  false)

        // matrices should be expanded in VertexFormat upstream; error if encountered here
        VertexElementType.MATRIX22_FLOAT32,
        VertexElementType.MATRIX33_FLOAT32,
        VertexElementType.MATRIX44_FLOAT32 ->
            error("Matrix vertex attributes should be expanded to vectors in VertexFormat")

        else -> error("Unsupported vertex element type: ${type}, it is not compatible with the STD430 layout rules.")
    }

    private fun attributeLocation(name: String, fallbackIndex: Int): Int = when {
        name == "position" || name == "a_position" -> 0
        name == "normal"   || name == "a_normal"   -> 1
        name == "color"    || name == "a_color"    -> 2
        name.startsWith("texCoord") || name.startsWith("a_tex") -> {
            val idx = name.removePrefix("texCoord").removePrefix("a_tex").toIntOrNull() ?: 0
            3 + idx
        }
        else -> 8 + fallbackIndex
    }

    private fun ensureDirect(src: ByteBuffer): ByteBuffer {
        if (src.isDirect) return src
        val copy = ByteBuffer.allocateDirect(src.remaining()).order(ByteOrder.nativeOrder())
        val pos = src.position()
        copy.put(src).flip()
        src.position(pos)
        return copy
    }
}