import js.buffer.ArrayBuffer
import js.buffer.ArrayBufferView
import web.gl.GLenum

external interface WebGLRenderingFixedCompressedTexImage {
    fun compressedTexImage2D(
        target: GLenum,
        level: Int,
        internalformat: GLenum,
        width: Int,
        height: Int,
        border: Int,
        data: ArrayBufferView<ArrayBuffer>?
    )
}