import org.lwjgl.util.freetype.FT_Face
import org.lwjgl.util.freetype.FT_Vector
import org.lwjgl.util.freetype.FreeType.FT_Done_Face
import org.lwjgl.util.freetype.FreeType.FT_Get_Char_Index
import org.lwjgl.util.freetype.FreeType.FT_Get_Kerning
import org.lwjgl.util.freetype.FreeType.FT_KERNING_DEFAULT
import org.lwjgl.util.freetype.FreeType.FT_LOAD_DEFAULT
import org.lwjgl.util.freetype.FreeType.FT_Load_Glyph
import org.lwjgl.util.freetype.FreeType.FT_Get_First_Char
import org.lwjgl.util.freetype.FreeType.FT_Get_Next_Char
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.freetype.FreeType.FT_Set_Char_Size
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.Glyph
import org.openrndr.shape.Rectangle

class FaceFreetype(val ftFace: FT_Face, override val sizeInPoints: Double, override val contentScale: Double) : Face {
    override fun allCodePoints(): Sequence<Int> = sequence {
        MemoryStack.stackPush().use { stack ->
            val glyphIndexPtr = stack.mallocInt(1)
            var charCode = FT_Get_First_Char(ftFace, glyphIndexPtr)

            while (glyphIndexPtr.get(0) != 0) {
                yield(charCode.toInt())
                charCode = FT_Get_Next_Char(ftFace, charCode, glyphIndexPtr)
            }
        }
    }

    override fun ascentMetrics(): Int {
        return ftFace.ascender().toInt()
    }

    override fun descentMetrics(): Int {
        return ftFace.descender().toInt()
    }


    override fun lineGapMetrics(): Int {
        return 0
    }

    override fun unitsPerEm(): Int {
        return ftFace.units_per_EM().toInt()
    }

    val ppxem = ftFace.size()?.metrics()?.x_ppem() ?: 0
    val ppyem = ftFace.size()?.metrics()?.y_ppem() ?: 0

    override val height: Double
        get() = ppyem * ftFace.height().toDouble() / ftFace.units_per_EM().toDouble()

    override val ascent: Double
        get() = ppyem * ftFace.ascender().toDouble() / ftFace.units_per_EM().toDouble()

    override val descent: Double
        get() = ppyem * ftFace.descender().toDouble() / ftFace.units_per_EM().toDouble()

    override val lineGap: Double
        get() = ftFace.height().toDouble() / ftFace.units_per_EM().toDouble() - ascent - descent

    override fun kernAdvance(left: Char, right: Char): Double {
        val leftIndex = FT_Get_Char_Index(ftFace, left.code.toLong())
        val rightIndex = FT_Get_Char_Index(ftFace, right.code.toLong())
        val kerning = FT_Vector.malloc()
        FT_Get_Kerning(ftFace, leftIndex, rightIndex, FT_KERNING_DEFAULT, kerning)
        val result = kerning.x().toDouble()
        kerning.free()
        return result
    }

    override fun glyphForCharacter(character: Char): GlyphFreetype {
        val index = FT_Get_Char_Index(ftFace, character.code.toLong())
        FT_Load_Glyph(ftFace, index, FT_LOAD_DEFAULT)
        return GlyphFreetype(this, character, index)
    }

    override fun glyphForCodePoint(codePoint: Int): Glyph {
        return glyphForCharacter(Char(codePoint))
    }

    /**
     * Handles rasterization of glyphs with temporary adjustment of the character size.
     *
     * The rasterization is performed within the provided `rasterize` function.
     * The method adjusts the FreeType character size settings before and after
     * the rasterization block to accommodate high-resolution rendering and scaling.
     *
     * @param rasterize The lambda function that performs the actual rasterization logic.
     * It will be executed while specific FreeType character size settings are applied.
     */
    fun rasterizing(rasterize: () -> Unit) {
        FT_Set_Char_Size(ftFace, 0, (sizeInPoints * 64).toLong(), (72 * contentScale).toInt(),
            (72 * contentScale).toInt()
        )
        try {
            rasterize()
        } finally {
            FT_Set_Char_Size(ftFace, 0, (sizeInPoints * 64).toLong(), 72, 72)
        }
    }

    override val bounds: Rectangle
        get() {
            val scale = 1.0
            val bbox = ftFace.bbox()
            return Rectangle(
                bbox.xMin() * scale,
                bbox.yMin() * scale,
                (bbox.xMax() - bbox.xMin()) * scale,
                (bbox.yMax() - bbox.yMin()) * scale
            )
        }

    override fun close() {
        FT_Done_Face(ftFace)
    }
}