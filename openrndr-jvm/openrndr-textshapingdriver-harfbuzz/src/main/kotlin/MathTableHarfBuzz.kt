package org.openrndr.textshapingdriver.harfbuzz

import org.lwjgl.system.MemoryStack
import org.lwjgl.util.harfbuzz.HarfBuzz.HB_DIRECTION_BTT
import org.lwjgl.util.harfbuzz.HarfBuzz.HB_DIRECTION_LTR
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_font_get_face
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_font_get_glyph_h_advance
import org.lwjgl.util.harfbuzz.OpenType
import org.lwjgl.util.harfbuzz.OpenType.*
import org.lwjgl.util.harfbuzz.hb_ot_math_glyph_part_t
import org.lwjgl.util.harfbuzz.hb_ot_math_glyph_variant_t
import org.openrndr.draw.font.*
import org.openrndr.fontdriver.freetype.FaceFreetype

/** the HarfBuzz values of the [MathConstant]s, by ordinal */
private val hbMathConstants = IntArray(MathConstant.entries.size) {
    OpenType::class.java.getField("HB_OT_MATH_CONSTANT_${MathConstant.entries[it].name}").getInt(null)
}

private fun MathDirection.hbDirection() = when (this) {
    MathDirection.HORIZONTAL -> HB_DIRECTION_LTR
    MathDirection.VERTICAL -> HB_DIRECTION_BTT
}

private fun MathKernCorner.hbCorner() = when (this) {
    MathKernCorner.TOP_RIGHT -> HB_OT_MATH_KERN_TOP_RIGHT
    MathKernCorner.TOP_LEFT -> HB_OT_MATH_KERN_TOP_LEFT
    MathKernCorner.BOTTOM_RIGHT -> HB_OT_MATH_KERN_BOTTOM_RIGHT
    MathKernCorner.BOTTOM_LEFT -> HB_OT_MATH_KERN_BOTTOM_LEFT
}

/**
 * The MATH table of [face], read through its HarfBuzz font. HarfBuzz scales the values of the table to the size of
 * the face in 26.6 pixels, like the positions of shaping results, they are divided by 64.
 */
internal class MathTableHarfBuzz(private val face: FaceFreetype) : MathTable {
    private fun Int.pixels() = this / 64.0

    override fun constant(constant: MathConstant): Double = face.shaping { font ->
        val value = hb_ot_math_get_constant(font, hbMathConstants[constant.ordinal])
        if (constant.isPercent) value.toDouble() else value.pixels()
    }

    override fun italicCorrection(glyphIndex: Int): Double = face.shaping { font ->
        hb_ot_math_get_glyph_italics_correction(font, glyphIndex).pixels()
    }

    override fun topAccentAttachment(glyphIndex: Int): Double? = face.shaping { font ->
        // HarfBuzz gives half the advance when the font has no attachment for the glyph. A font that stores half the
        // advance gives null too, which callers treat the same way
        val attachment = hb_ot_math_get_glyph_top_accent_attachment(font, glyphIndex)
        if (attachment == hb_font_get_glyph_h_advance(font, glyphIndex) / 2) null else attachment.pixels()
    }

    override fun isExtendedShape(glyphIndex: Int): Boolean = face.shaping { font ->
        hb_ot_math_is_glyph_extended_shape(hb_font_get_face(font), glyphIndex)
    }

    override fun kern(glyphIndex: Int, corner: MathKernCorner, correctionHeight: Double): Double =
        face.shaping { font ->
            hb_ot_math_get_glyph_kerning(font, glyphIndex, corner.hbCorner(), (correctionHeight * 64.0).toInt())
                .pixels()
        }

    override fun variants(glyphIndex: Int, direction: MathDirection): List<MathGlyphVariant> =
        face.shaping { font ->
            MemoryStack.stackPush().use { stack ->
                val count = stack.ints(0)
                val none = hb_ot_math_glyph_variant_t.calloc(1, stack)
                val total = hb_ot_math_get_glyph_variants(font, glyphIndex, direction.hbDirection(), 0, count, none)
                if (total == 0) return@shaping emptyList()
                val variants = hb_ot_math_glyph_variant_t.calloc(total)
                try {
                    count.put(0, total)
                    hb_ot_math_get_glyph_variants(font, glyphIndex, direction.hbDirection(), 0, count, variants)
                    List(count.get(0)) { MathGlyphVariant(variants[it].glyph(), variants[it].advance().pixels()) }
                } finally {
                    variants.free()
                }
            }
        }

    override fun assembly(glyphIndex: Int, direction: MathDirection): MathGlyphAssembly? = face.shaping { font ->
        MemoryStack.stackPush().use { stack ->
            val count = stack.ints(0)
            val italics = stack.ints(0)
            val none = hb_ot_math_glyph_part_t.calloc(1, stack)
            val total = hb_ot_math_get_glyph_assembly(font, glyphIndex, direction.hbDirection(), 0, count, none, italics)
            if (total == 0) return@shaping null
            val parts = hb_ot_math_glyph_part_t.calloc(total)
            try {
                count.put(0, total)
                hb_ot_math_get_glyph_assembly(font, glyphIndex, direction.hbDirection(), 0, count, parts, italics)
                MathGlyphAssembly(List(count.get(0)) {
                    val part = parts[it]
                    MathGlyphPart(
                        part.glyph(),
                        part.start_connector_length().pixels(),
                        part.end_connector_length().pixels(),
                        part.full_advance().pixels(),
                        (part.flags() and HB_OT_MATH_GLYPH_PART_FLAG_EXTENDER) != 0
                    )
                }, italics.get(0).pixels())
            } finally {
                parts.free()
            }
        }
    }

    override fun minConnectorOverlap(direction: MathDirection): Double = face.shaping { font ->
        hb_ot_math_get_min_connector_overlap(font, direction.hbDirection()).pixels()
    }
}
