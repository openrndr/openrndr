package org.openrndr.internal.gl3

import org.lwjgl.BufferUtils
import org.lwjgl.stb.STBImage
import org.openrndr.resourceUrl
import java.net.URL
import java.nio.Buffer
import java.nio.ByteBuffer

internal fun loadIcon(url: String): ByteBuffer {
    val byteArray = URL(resourceUrl(url)).readBytes()
    val buffer = BufferUtils.createByteBuffer(byteArray.size)

    (buffer as Buffer).rewind()
    buffer.put(byteArray)
    (buffer as Buffer).rewind()

    val wa = IntArray(1)
    val ha = IntArray(1)
    val ca = IntArray(1)
    STBImage.stbi_set_flip_vertically_on_load(true)
    STBImage.stbi_set_unpremultiply_on_load(false)

    val data = STBImage.stbi_load_from_memory(buffer, wa, ha, ca, 4)

    return data!!
}