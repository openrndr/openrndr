package org.openrndr.internal.gl3

import android.opengl.GLES30
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.UniformBlock
import org.openrndr.draw.UniformBlockLayout
import org.openrndr.draw.UniformDescription
import org.openrndr.draw.UniformType
import org.openrndr.math.Matrix44
import org.openrndr.math.Matrix55
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import java.nio.ByteBuffer
import java.nio.ByteOrder

class UniformBlockGLES(
    override val layout: UniformBlockLayout,
    /** GL uniform-buffer object id (created elsewhere or via helper below) */
    private val uboId: Int,
    /** Binding point to which the UBO should be bound (shader must use same binding) */
    private val bindingPoint: Int
) : UniformBlock {

    private val data: ByteBuffer =
        ByteBuffer.allocateDirect(layout.sizeInBytes).order(ByteOrder.nativeOrder())

    override var dirty: Boolean = false
        private set

    // ---- UniformBlock API ----

    override fun uniform(name: String, value: Float) {
        val u = entry(name, UniformType.FLOAT32)
        data.putFloat(u.offset, value)
        dirty = true
    }

    override fun uniform(name: String, value: Vector2) {
        val u = entry(name, UniformType.VECTOR2_FLOAT32)
        // std140 often pads vec2 to 16 bytes; we rely on 'stride' from layout
        data.putFloat(u.offset + 0, value.x.toFloat())
        data.putFloat(u.offset + 4, value.y.toFloat())
        dirty = true
    }

    override fun uniform(name: String, value: Vector3) {
        val u = entry(name, UniformType.VECTOR3_FLOAT32)
        data.putFloat(u.offset + 0, value.x.toFloat())
        data.putFloat(u.offset + 4, value.y.toFloat())
        data.putFloat(u.offset + 8, value.z.toFloat())
        dirty = true
    }

    override fun uniform(name: String, value: Vector4) {
        val u = entry(name, UniformType.VECTOR4_FLOAT32)
        data.putFloat(u.offset + 0, value.x.toFloat())
        data.putFloat(u.offset + 4, value.y.toFloat())
        data.putFloat(u.offset + 8, value.z.toFloat())
        data.putFloat(u.offset + 12, value.w.toFloat())
        dirty = true
    }

    override fun uniform(name: String, value: ColorRGBa) {
        val u = entry(name, UniformType.VECTOR4_FLOAT32)
        data.putFloat(u.offset + 0, value.r.toFloat())
        data.putFloat(u.offset + 4, value.g.toFloat())
        data.putFloat(u.offset + 8, value.b.toFloat())
        data.putFloat(u.offset + 12, value.a.toFloat())
        dirty = true
    }

    override fun uniform(name: String, value: Matrix44) {
        val u = entry(name, UniformType.MATRIX44_FLOAT32)
        // Write 16 floats; OPENGL expects column-major by default.
        // Matrix44 stores row-major/column-major? If unsure, call .column() to be explicit.
        // Here we serialize by columns to match GLSL default.
        var off = u.offset
        for (c in 0 until 4) {
            for (r in 0 until 4) {
                data.putFloat(off, value[c][r].toFloat())
                off += 4
            }
            // std140 uses 16-byte stride per column; your layout.stride already accounts for it,
            // and offsets for each column should be provided if the block has them exploded.
        }
        dirty = true
    }

    override fun uniform(name: String, value: Matrix55) {
        val u = requireEntry(name)
        // Matrix55 already provides a row-major float array (see your code).
        // We’ll just write its 25 floats in the order provided by the layout.
        val fa = value.floatArray

        when (u.type) {
            // Case 1: layout encodes the 5x5 as an array of FLOAT32 (size=25) with std140 stride (likely 16)
            UniformType.FLOAT32 -> {
                // Write scalars with stride; std140 arrays of scalars use 16-byte stride,
                // but we don’t hardcode it: we use u.stride from your layout.
                writeArrayScalars(u, fa, count = minOf(25, u.size))
            }

            // Case 2: layout encodes columns as vec4 (VECTOR4_FLOAT32) array (size>=5),
            // and you store the last 5 scalars elsewhere (recommended split).
            UniformType.VECTOR4_FLOAT32 -> {
                val cols = minOf(5, u.size)
                var src = 0
                var off = u.offset
                repeat(cols) {
                    // write 4 components of column (first 4 rows)
                    put4(off, fa[src], fa[src + 1], fa[src + 2], fa[src + 3])
                    src += 5   // skip to next column start (row-major 5 values per column slice)
                    off += u.stride
                }
                // NOTE: the last (5th) row components (fa indices 4,9,14,19,24) won’t fit here.
                // Provide another uniform entry (e.g., name + "_row4" as FLOAT32 array size=5),
                // and call uniform("<name>_row4", FloatArray(5) { fa[4 + it*5] }) from your code.
            }

            // Case 3: if you chose to encode the whole 5x5 as VECTOR4_FLOAT32 with size=7 (5 vec4 + 1 vec4 + 1 vec1),
            // handle it accordingly; but generally we recommend Case 1 or a clean split as in Case 2.
            else -> {
                // Fallback: write as tightly packed scalars advancing by stride
                writeArrayScalars(u, fa, count = minOf(25, u.size))
            }
        }
        dirty = true
    }

    private fun requireEntry(name: String): UniformDescription =
        layout.entries[name] ?: error("Uniform '$name' not found in layout")

    private fun put4(offset: Int, x: Float, y: Float, z: Float, w: Float) {
        data.putFloat(offset + 0,  x)
        data.putFloat(offset + 4,  y)
        data.putFloat(offset + 8,  z)
        data.putFloat(offset + 12, w)
    }

    private fun writeArrayScalars(u: UniformDescription, src: FloatArray, count: Int) {
        var off = u.offset
        var i = 0
        while (i < count) {
            data.putFloat(off, src[i])
            off += u.stride
            i++
        }
    }

    override fun uniform(name: String, value: Array<Float>) {
        val u = entry(name, UniformType.FLOAT32)
        var off = u.offset
        for (i in value.indices) {
            data.putFloat(off, value[i])
            off += u.stride // stride given by layout lets us honor std140 padding
        }
        dirty = true
    }

    override fun uniform(name: String, value: Array<Vector2>) {
        val u = entry(name, UniformType.VECTOR2_FLOAT32)
        var off = u.offset
        for (i in value.indices) {
            data.putFloat(off + 0, value[i].x.toFloat())
            data.putFloat(off + 4, value[i].y.toFloat())
            off += u.stride
        }
        dirty = true
    }

    override fun uniform(name: String, value: Array<Vector3>) {
        val u = entry(name, UniformType.VECTOR3_FLOAT32)
        var off = u.offset
        for (i in value.indices) {
            data.putFloat(off + 0, value[i].x.toFloat())
            data.putFloat(off + 4, value[i].y.toFloat())
            data.putFloat(off + 8, value[i].z.toFloat())
            off += u.stride
        }
        dirty = true
    }

    override fun uniform(name: String, value: Array<Vector4>) {
        val u = entry(name, UniformType.VECTOR4_FLOAT32)
        var off = u.offset
        for (i in value.indices) {
            data.putFloat(off + 0, value[i].x.toFloat())
            data.putFloat(off + 4, value[i].y.toFloat())
            data.putFloat(off + 8, value[i].z.toFloat())
            data.putFloat(off + 12, value[i].w.toFloat())
            off += u.stride
        }
        dirty = true
    }

    override fun upload() {
        if (!dirty) return
        data.position(0)
        GLES30.glBindBuffer(GLES30.GL_UNIFORM_BUFFER, uboId)
        GLES30.glBufferSubData(GLES30.GL_UNIFORM_BUFFER, 0, layout.sizeInBytes, data)
        GLES30.glBindBuffer(GLES30.GL_UNIFORM_BUFFER, 0)
        // Ensure it’s bound to the expected binding point for the shader(s)
        GLES30.glBindBufferBase(GLES30.GL_UNIFORM_BUFFER, bindingPoint, uboId)
        dirty = false
    }

    // ---- helpers ----
    private fun entry(name: String, expected: UniformType): UniformDescription {
        val e = layout.entries[name]
            ?: error("Uniform '$name' not found in layout entries")
        // We don’t strictly enforce type equality here, because arrays share the same method name.
        // If you want strict checks for scalar/vector cases, uncomment:
        // require(e.type == expected) { "Uniform '$name' type mismatch: layout=${e.type} expected=$expected" }
        return e
    }
}