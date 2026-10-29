@file:OptIn(ExperimentalWasmJsInterop::class)

package org.openrndr.textshapingdriver.harfbuzzjs

import org.openrndr.draw.font.Face
import org.openrndr.draw.font.internal.Direction
import org.openrndr.draw.font.internal.Script
import org.openrndr.draw.font.internal.ShapeFeature
import org.openrndr.draw.font.internal.ShapeResult
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.fontdriver.harfbuzzjs.FaceHarfBuzzJs
import org.openrndr.fontdriver.harfbuzzjs.hbDefaultLanguageFeatureTags
import org.openrndr.fontdriver.harfbuzzjs.hbOpenTypeScriptTags
import org.openrndr.fontdriver.harfbuzzjs.hbTableScriptTags
import org.openrndr.math.Vector2

// harfbuzz's direction values
private fun Direction.hbDirection() = when (this) {
    Direction.LEFT_TO_RIGHT -> 4
    Direction.RIGHT_TO_LEFT -> 5
    Direction.TOP_TO_BOTTOM -> 6
    Direction.BOTTOM_TO_TOP -> 7
}

private fun String.splitTags() = if (isEmpty()) emptyList() else split(",")

/**
 * A [TextShapingDriver] for browsers and Node.js, backed by harfbuzzjs. Shapes faces of the
 * [org.openrndr.fontdriver.harfbuzzjs.FontDriverHarfBuzzJs], offsets and advances are in pixels at the size of the face.
 */
class TextShapingDriverHarfBuzzJs : TextShapingDriver {

    private fun Face.harfBuzzFace(): FaceHarfBuzzJs =
        this as? FaceHarfBuzzJs ?: error("TextShapingDriverHarfBuzzJs shapes faces loaded by FontDriverHarfBuzzJs, not a ${this::class.simpleName}")

    override fun shape(
        face: Face,
        text: String,
        features: List<ShapeFeature>,
        direction: Direction?,
        script: Script?,
        language: String?
    ): List<ShapeResult> {
        val hbFace = face.harfBuzzFace()
        val shaped = hbFace.shapeGlyphs(
            text,
            features.map { "${it.tag}=${it.value}" },
            direction?.hbDirection() ?: 0,
            script?.tag ?: "",
            language ?: ""
        )
        val scale = hbFace.scale
        val stride = FaceHarfBuzzJs.SHAPED_STRIDE
        return (0 until shaped.size / stride).map { i ->
            val base = i * stride
            ShapeResult(
                shaped[base],
                Vector2(shaped[base + 4] * scale, shaped[base + 5] * scale),
                Vector2(shaped[base + 2] * scale, shaped[base + 3] * scale)
            )
        }
    }

    /**
     * The index of [script] in the script list of [table], null when the table has no entry for it
     */
    private fun scriptIndex(face: FaceHarfBuzzJs, table: String, script: Script): Int? {
        val tableScripts = hbTableScriptTags(face.data.hbFace, table).splitTags()
        // a script can have multiple OpenType tags, like 'dev2' and 'deva', preferred tag first
        return hbOpenTypeScriptTags(face.data.harfBuzz, script.tag).splitTags()
            .map { tableScripts.indexOf(it) }
            .firstOrNull { it >= 0 }
    }

    private fun features(face: Face, script: Script, table: String): List<String> {
        val hbFace = face.harfBuzzFace()
        val index = scriptIndex(hbFace, table, script) ?: return emptyList()
        return hbDefaultLanguageFeatureTags(hbFace.data.hbFace, table, index).splitTags()
    }

    override fun querySubstitutionFeatures(face: Face, script: Script): List<String> = features(face, script, "GSUB")

    override fun queryPositionFeatures(face: Face, script: Script): List<String> = features(face, script, "GPOS")

    override fun querySupportedScripts(face: Face): List<Script> {
        val hbFace = face.harfBuzzFace()
        return Script.entries.filter { script ->
            script != Script.COMMON && script != Script.INHERITED && script != Script.UNKNOWN &&
                    (scriptIndex(hbFace, "GSUB", script) != null || scriptIndex(hbFace, "GPOS", script) != null)
        }
    }
}
