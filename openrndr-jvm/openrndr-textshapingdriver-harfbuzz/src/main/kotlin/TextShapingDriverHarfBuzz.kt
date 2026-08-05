package org.openrndr.textshapingdriver.harfbuzz

import org.lwjgl.BufferUtils
import org.lwjgl.system.Configuration
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.freetype.FreeType
import org.lwjgl.util.harfbuzz.HarfBuzz.HB_DIRECTION_LTR
import org.lwjgl.util.harfbuzz.HarfBuzz.HB_DIRECTION_RTL
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_add_codepoints
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_create
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_destroy
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_get_glyph_infos
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_get_glyph_positions
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_guess_segment_properties
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_set_direction
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_set_language
import org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_set_script

import org.lwjgl.util.harfbuzz.HarfBuzz.*
import org.lwjgl.util.harfbuzz.OpenType.*
import org.lwjgl.util.harfbuzz.hb_feature_t
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.internal.Direction
import org.openrndr.draw.font.internal.Script
import org.openrndr.draw.font.internal.ShapeFeature
import org.openrndr.draw.font.internal.ShapeResult
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.fontdriver.freetype.FaceFreetype
import org.openrndr.math.Vector2
import java.nio.IntBuffer
import kotlin.code

private fun Direction.hbDirection() = when (this) {
    Direction.LEFT_TO_RIGHT -> HB_DIRECTION_LTR
    Direction.RIGHT_TO_LEFT -> HB_DIRECTION_RTL
    Direction.TOP_TO_BOTTOM -> HB_DIRECTION_LTR
    Direction.BOTTOM_TO_TOP -> HB_DIRECTION_RTL
}

private fun Script.hbScript(): Int {
    val c1 = this.tag[0].code
    val c2 = this.tag[1].code
    val c3 = this.tag[2].code
    val c4 = this.tag[3].code
    return ((c1 and 0xFF) shl 24) or ((c2 and 0xFF) shl 16) or ((c3 and 0xFF) shl 8) or (c4 and 0xFF);
}

class TextShapingDriverHarfBuzz : TextShapingDriver {
    init {
        // Tell Harfbuzz to use the FreeType library
        Configuration.HARFBUZZ_LIBRARY_NAME.set(FreeType.getLibrary())
    }

    override fun shape(
        face: Face,
        text: String,
        features: List<ShapeFeature>,
        direction: Direction?,
        script: Script?,
        language: String?
    ): List<ShapeResult> {
        face as FaceFreetype
        val codePoints = IntArray(text.length) { text.codePointAt(it) }

        // Allocate a buffer for the code points plus a null terminator
        val codePointsBuffer = MemoryUtil.memCallocInt(codePoints.size + 4)
        codePointsBuffer.put(codePoints)
        codePointsBuffer.flip()

        val buf = hb_buffer_create()

        try {
            hb_buffer_add_codepoints(buf, codePointsBuffer, 0, codePoints.size)

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
            require(hbFont != 0L) {
                "Failed to create HarfBuzz font"
            }

            val featureBuffer = if (features.isNotEmpty()) hb_feature_t.calloc(features.size) else null

            if (featureBuffer != null) {
                for ((index, feature) in features.withIndex()) {
                    featureBuffer[index].tag(hb_tag_from_string(feature.tag))
                    featureBuffer[index].value(feature.value)
                    featureBuffer[index].start(0) //feature.range.first.toInt())
                    featureBuffer[index].end(-1) //feature.range.last.toInt())
                }
            }

            hb_shape(hbFont, buf, featureBuffer)

            val glyphInfo = hb_buffer_get_glyph_infos(buf) ?: error("No glyph info")
            val glyphPosition = hb_buffer_get_glyph_positions(buf) ?: error("No glyph position")

            return (0 until glyphInfo.count()).map {
                val gp = glyphPosition[it] ?: error("No glyph position at index $it")
                val position = Vector2(gp.x_offset() / 64.0, gp.y_offset() / 64.0)
                val advance = Vector2(gp.x_advance() / 64.0, gp.y_advance() / 64.0)
                ShapeResult(glyphInfo.get(it)!!.codepoint(), position, advance)
            }
        } finally {
            // Clean up resources
            hb_buffer_destroy(buf)
            MemoryUtil.memFree(codePointsBuffer)
        }
    }

    override fun querySubstitutionFeatures(
        face: Face,
        script: Script
    ): List<String> {
        return queryOpenTypeFeatures(face, script, HB_OT_TAG_GSUB)
    }

    override fun queryPositionFeatures(
        face: Face,
        script: Script
    ): List<String> {
        return queryOpenTypeFeatures(face, script, HB_OT_TAG_GPOS)
    }

    fun scriptTagToString(tag: Int): String {
        val c1 = (tag shr 24) and 0xFF
        val c2 = (tag shr 16) and 0xFF
        val c3 = (tag shr 8) and 0xFF
        val c4 = tag and 0xFF
        return String(charArrayOf(c1.toChar(), c2.toChar(), c3.toChar(), c4.toChar()))
    }

    override fun querySupportedScripts(face: Face): List<Script> {
        face as FaceFreetype
        val hbFont = hb_ft_font_create(face.ftFace.address(), null)
        if (hbFont == 0L) return emptyList()

        val hbFace = hb_font_get_face(hbFont)
        if (hbFace == 0L) {
            hb_font_destroy(hbFont)
            return emptyList()
        }

        try {
            val scriptTagsSet = mutableSetOf<Int>()

            for (tableTag in intArrayOf(HB_OT_TAG_GSUB, HB_OT_TAG_GPOS)) {
                val scriptCount = BufferUtils.createIntBuffer(1)
                scriptCount.put(0, 32)
                val scriptTags = BufferUtils.createIntBuffer(32)

                hb_ot_layout_table_get_script_tags(hbFace, tableTag, 0, scriptCount, scriptTags)

                for (i in 0 until scriptCount.get(0)) {
                    scriptTagsSet.add(scriptTags.get(i))
                }
            }

            val scriptsByTag = Script.entries.associateBy { it.tag.lowercase() }

            return scriptTagsSet.mapNotNull { tag ->
                val tagString = scriptTagToString(tag).lowercase().trim()
                scriptsByTag[tagString]
            }.distinct()

        } finally {
            hb_font_destroy(hbFont)
        }
    }

    fun queryOpenTypeFeatures(face: Face, script: Script, tableTag: Int): List<String> {
        face as FaceFreetype
        val hbFont = hb_ft_font_create(face.ftFace.address(), null)
        if (hbFont == 0L) return emptyList()

        val hbFace = hb_font_get_face(hbFont)
        if (hbFace == 0L) {
            hb_font_destroy(hbFont)
            return emptyList()
        }


        try {

            val features = mutableListOf<String>()
            val supportedScripts = querySupportedScripts(face)

            val scriptIndex = supportedScripts.indexOf(script)

            if (scriptIndex == -1) return emptyList()

            val featureCount = BufferUtils.createIntBuffer(1)
            val featureTags = BufferUtils.createIntBuffer(32)
            featureCount.put(0, 32)


            hb_ot_layout_language_get_feature_tags(
                hbFace,
                tableTag,
                scriptIndex,
                HB_OT_LAYOUT_DEFAULT_LANGUAGE_INDEX,
                0,
                featureCount,
                featureTags
            )

            for (j in 0 until featureCount.get(0)) {
                features.add(scriptTagToString(featureTags.get(j)))
            }
            return features

        } finally {
            hb_font_destroy(hbFont)
        }
    }
}

