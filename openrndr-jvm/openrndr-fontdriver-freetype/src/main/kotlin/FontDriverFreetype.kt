package org.openrndr.fontdriver.freetype

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.PointerBuffer
import org.lwjgl.system.Configuration
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.freetype.FT_Face
import org.lwjgl.util.freetype.FreeType
import org.lwjgl.util.freetype.FreeType.FT_Init_FreeType
import org.lwjgl.util.freetype.FreeType.FT_New_Memory_Face
import org.openrndr.draw.font.internal.FontDriver
import java.io.File


private val logger = KotlinLogging.logger {  }
fun bytesFromFileOrUrl(fileOrUrl: String): ByteArray {
    return if (fileOrUrl.startsWith("http") || fileOrUrl.startsWith("file") || fileOrUrl.startsWith("jar")) {
        java.net.URL(fileOrUrl).readBytes()
    } else {
        File(fileOrUrl).readBytes()
    }
}

class FontDriverFreetype(val library: Long) : FontDriver {
    init {
        // HarfBuzz (used for kerning) is bundled with the FreeType library
        Configuration.HARFBUZZ_LIBRARY_NAME.set(FreeType.getLibrary())
    }

    override fun loadFace(fileOrUrl: String): FaceFreetype {
        val face = MemoryUtil.memAllocPointer(1)
        val fontBytes = bytesFromFileOrUrl(fileOrUrl)
        val buffer = MemoryUtil.memCalloc(fontBytes.size)
        buffer.put(fontBytes)
        buffer.flip()

        require(FT_New_Memory_Face(library, buffer, 0L, face) == 0) { "Failed to load font face from $fileOrUrl" }
        val ftFace = FT_Face.create(face.get(0))
        return FaceFreetype(
            library,
            ftFace,
            FontDriver.DEFAULT_SIZE_IN_POINTS,
            FaceFreetype.masterForFace(library, ftFace)
        )
    }
}

fun FontDriverFreetype(): FontDriverFreetype {
    logger.info { "Initializing freetype" }
    val library = PointerBuffer.allocateDirect(1)
    val result = FT_Init_FreeType(library)
    require(result == 0)
    return FontDriverFreetype(library.get(0))
}