package org.openrndr.textshapingdriver.harfbuzz

import org.lwjgl.system.Configuration
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.freetype.FreeType
import org.lwjgl.util.harfbuzz.HarfBuzz.*
import org.lwjgl.util.harfbuzz.OpenType.*
import org.lwjgl.util.harfbuzz.hb_feature_t
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.internal.*
import org.openrndr.fontdriver.freetype.FaceFreetype
import org.openrndr.math.Vector2

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
        val codePoints = text.codePoints().toArray()

        // Allocate a buffer for the code points plus a null terminator
        val codePointsBuffer = MemoryUtil.memCallocInt(codePoints.size + 4)
        codePointsBuffer.put(codePoints)
        codePointsBuffer.flip()

        val buf = hb_buffer_create()
        val featureBuffer = if (features.isNotEmpty()) hb_feature_t.calloc(features.size) else null

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

            // fills in only the properties that were not set above
            hb_buffer_guess_segment_properties(buf)

            require(face.ftFace.address() != 0L) {
                "FT_Face is not initialized"
            }

            if (featureBuffer != null) {
                for ((index, feature) in features.withIndex()) {
                    featureBuffer[index].tag(hb_tag_from_string(feature.tag))
                    featureBuffer[index].value(feature.value)
                    featureBuffer[index].start(0) //feature.range.first.toInt())
                    featureBuffer[index].end(-1) //feature.range.last.toInt())
                }
            }

            face.shaping { hbFont -> hb_shape(hbFont, buf, featureBuffer) }

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
            featureBuffer?.free()
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

    /**
     * Find the index of [script] in the script list of the given OpenType layout table
     * @return the script index or null when the table has no entry for [script]
     */
    private fun findScriptIndex(hbFace: Long, tableTag: Int, script: Script): Int? {
        MemoryStack.stackPush().use { stack ->
            // A script can have multiple OpenType tags (for example 'dev2' and 'deva'), preferred tag first
            val scriptTagCount = stack.ints(HB_OT_MAX_TAGS_PER_SCRIPT)
            val scriptTags = stack.mallocInt(HB_OT_MAX_TAGS_PER_SCRIPT)
            val languageTagCount = stack.ints(0)
            val languageTags = stack.mallocInt(1)
            hb_ot_tags_from_script_and_language(
                script.hbScript(), hb_language_get_default(), scriptTagCount, scriptTags, languageTagCount, languageTags
            )
            scriptTags.limit(scriptTagCount.get(0))
            if (!scriptTags.hasRemaining()) {
                return null
            }

            val scriptIndex = stack.mallocInt(1)
            val chosenScript = stack.mallocInt(1)
            // select_script falls back to the default script but only returns true for an exact match
            return if (hb_ot_layout_table_select_script(hbFace, tableTag, scriptTags, scriptIndex, chosenScript)) {
                scriptIndex.get(0)
            } else {
                null
            }
        }
    }

    override fun querySupportedScripts(face: Face): List<Script> {
        face as FaceFreetype
        return face.shaping { hbFont ->
            val hbFace = hb_font_get_face(hbFont)
            if (hbFace == 0L) return@shaping emptyList()

            Script.entries.filter { script ->
                script != Script.COMMON && script != Script.INHERITED && script != Script.UNKNOWN &&
                        (findScriptIndex(hbFace, HB_OT_TAG_GSUB, script) != null ||
                                findScriptIndex(hbFace, HB_OT_TAG_GPOS, script) != null)
            }
        }
    }

    fun queryOpenTypeFeatures(face: Face, script: Script, tableTag: Int): List<String> {
        face as FaceFreetype
        return face.shaping { hbFont ->
            val hbFace = hb_font_get_face(hbFont)
            if (hbFace == 0L) return@shaping emptyList()

            val scriptIndex = findScriptIndex(hbFace, tableTag, script) ?: return@shaping emptyList()

            val features = mutableListOf<String>()
            MemoryStack.stackPush().use { stack ->
                val featureCount = stack.mallocInt(1)
                val featureTags = stack.mallocInt(32)
                var offset = 0
                do {
                    featureCount.put(0, featureTags.capacity())
                    val total = hb_ot_layout_language_get_feature_tags(
                        hbFace,
                        tableTag,
                        scriptIndex,
                        HB_OT_LAYOUT_DEFAULT_LANGUAGE_INDEX,
                        offset,
                        featureCount,
                        featureTags
                    )
                    for (j in 0 until featureCount.get(0)) {
                        features.add(scriptTagToString(featureTags.get(j)))
                    }
                    offset += featureCount.get(0)
                } while (featureCount.get(0) > 0 && offset < total)
            }
            features
        }
    }
}

