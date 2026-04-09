import org.lwjgl.BufferUtils
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.freetype.FT_Face
import org.lwjgl.util.freetype.FreeType.FT_Init_FreeType
import org.lwjgl.util.freetype.FreeType.FT_New_Memory_Face
import org.lwjgl.util.freetype.FreeType.FT_Set_Char_Size
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.internal.FontDriver
import java.io.File
import java.nio.ByteBuffer



fun bytesFromFileOrUrl(fileOrUrl: String): ByteArray {
    return if (fileOrUrl.startsWith("http") || fileOrUrl.startsWith("file")) {
        java.net.URL(fileOrUrl).readBytes()
    } else {
        File(fileOrUrl).readBytes()
    }
}


class FontDriverFreetype(val library: Long) : FontDriver {

    override fun loadFace(fileOrUrl: String, sizeInPoints: Double, contentScale: Double): FaceFreetype {

        val face = MemoryUtil.memAllocPointer(1)
        val fontBytes = bytesFromFileOrUrl(fileOrUrl)
        val buffer = MemoryUtil.memCalloc(fontBytes.size)
        buffer.put(fontBytes)
        buffer.flip()


        require(FT_New_Memory_Face(library, buffer, 0L, face) == 0) { "Failed to load font face from $fileOrUrl" }

        val ftFace = FT_Face.create(face.get(0))
        FT_Set_Char_Size(ftFace, 0, (sizeInPoints * 64).toLong(), 72, 72)
        return FaceFreetype(library, ftFace, sizeInPoints, contentScale)
    }
}

fun FontDriverFreetype(): FontDriverFreetype {

    val library = PointerBuffer.allocateDirect(1)
    val result = FT_Init_FreeType(library)
    require(result == 0)
    return FontDriverFreetype(library.get(0))
}