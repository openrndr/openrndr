package org.openrndr.internal.gl3

import org.lwjgl.stb.STBImage.stbi_info_from_memory
import org.lwjgl.system.MemoryStack.stackPush
import org.openrndr.draw.ImageFileDetails
import org.openrndr.internal.ImageDriver
import org.openrndr.utils.url.resolveFileOrUrl

class ImageDriverStbImage : ImageDriver {
    override fun probeImage(fileOrUrl: String): ImageFileDetails? {
        val (file, url) = resolveFileOrUrl(fileOrUrl)

        val header = ByteArray(1024)
        url?.let { it.openStream().use { stream -> stream.read(header) } }
        file?.let { it.inputStream().use { stream -> stream.read(header) } }

        stackPush().use { stack ->
            val bb = stack.malloc(header.size)
            bb.put(header)
            bb.flip()
            val x = stack.mallocInt(1)
            val y = stack.mallocInt(1)
            val channels = stack.mallocInt(1)
            val result = stbi_info_from_memory(bb, x, y, channels)

            if (result) {
                return ImageFileDetails(x.get(), y.get(), channels.get())
            }
        }
        return null
    }
}