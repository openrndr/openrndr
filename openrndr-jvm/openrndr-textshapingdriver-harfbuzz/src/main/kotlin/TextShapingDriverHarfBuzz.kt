import org.lwjgl.system.Configuration
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.freetype.FreeType
import org.lwjgl.util.harfbuzz.HarfBuzz.HB_DIRECTION_LTR
import org.lwjgl.util.harfbuzz.HarfBuzz.HB_DIRECTION_RTL
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_add_utf8
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_create
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_get_glyph_infos
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_get_glyph_positions
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_guess_segment_properties
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_set_direction
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_set_language
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_set_script
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_ft_font_create
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_language_from_string
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_shape
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.internal.Direction
import org.openrndr.draw.font.internal.Script
import org.openrndr.draw.font.internal.ShapeResult
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.math.Vector2
import kotlin.code

fun Direction.hbDirection() = when (this) {
    Direction.LEFT_TO_RIGHT -> HB_DIRECTION_LTR
    Direction.RIGHT_TO_LEFT -> HB_DIRECTION_RTL
    Direction.TOP_TO_BOTTOM -> HB_DIRECTION_LTR
    Direction.BOTTOM_TO_TOP -> HB_DIRECTION_RTL
}

fun Script.hbScript(): Int {
    val c1 = this.tag[0].code
    val c2 = this.tag[1].code
    val c3 = this.tag[2].code
    val c4 = this.tag[3].code
    return ((c1 and 0xFF) shl 24) or ((c2 and 0xFF)shl 16) or ((c3 and 0xFF) shl 8) or (c4 and 0xFF);
}

class TextShapingDriverHarfBuzz : TextShapingDriver {
    init {
        Configuration.HARFBUZZ_LIBRARY_NAME.set(FreeType.getLibrary())
    }

    override fun shape(
        face: Face,
        text: String,
        direction: Direction?,
        script: Script?,
        language: String?
    ): List<ShapeResult> {
        face as FaceFreetype

        val buf = hb_buffer_create()
        val textUtf8 = MemoryUtil.memUTF8(text)

        hb_buffer_add_utf8(buf, text, 0, -1)

        if (direction != null) {
            hb_buffer_set_direction(buf, direction.hbDirection())
        }
        if (script != null) {
            hb_buffer_set_script(buf, script.hbScript())
        }
        if (language != null) {
            hb_buffer_set_language(buf, hb_language_from_string(language));
        }

        if (direction == null && script == null && language == null) {
            hb_buffer_guess_segment_properties(buf)
        }

        require(face.ftFace.address() != 0L) {
            "FT_Face is not initialized"
        }

        val hbFont = hb_ft_font_create(face.ftFace.address(), null)
        hb_shape(hbFont, buf, null)

        val glyphInfo = hb_buffer_get_glyph_infos(buf)!!
        val glyphPosition = hb_buffer_get_glyph_positions(buf)!!

        val shapeResult = (0 until glyphInfo.count()).map {
            val gp = glyphPosition[it]!!
            val position = Vector2(gp.x_offset() / 64.0, gp.y_offset() / 64.0)
            val advance = Vector2(gp.x_advance() / 64.0, gp.y_advance() / 64.0)
            ShapeResult(glyphInfo.get(it)!!.codepoint(), position, advance)
        }

        MemoryUtil.memFree(textUtf8)
        return shapeResult
    }
}

