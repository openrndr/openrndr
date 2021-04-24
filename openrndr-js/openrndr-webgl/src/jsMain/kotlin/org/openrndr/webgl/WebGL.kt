import org.khronos.webgl.ArrayBufferView

external interface WebGLRenderingFixedCompressedTexImage {
    fun compressedTexImage2D(
        target: Int,
        level: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        border: Int,
        data: ArrayBufferView?
    )
}