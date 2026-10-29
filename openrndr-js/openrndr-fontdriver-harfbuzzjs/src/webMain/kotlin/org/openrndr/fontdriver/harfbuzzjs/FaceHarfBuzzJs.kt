@file:OptIn(ExperimentalWasmJsInterop::class)

package org.openrndr.fontdriver.harfbuzzjs

import org.openrndr.draw.font.Face
import org.openrndr.draw.font.FaceMaster
import org.openrndr.draw.font.Glyph
import org.openrndr.shape.Rectangle
import kotlin.math.absoluteValue

/**
 * The font data of a face, shared by the faces derived from it with [Face.withSize] and [Face.withMaster]
 * @property axisTags the tags of the variation axes, by the names used in [FaceMaster]
 */
class HarfBuzzFaceData internal constructor(
    val harfBuzz: HarfBuzzModule,
    val hbFace: HarfBuzzObject,
    internal val axisTags: Map<String, String>,
    internal val defaultMaster: FaceMaster
) {
    val unitsPerEm: Int = hbUpem(hbFace)
}

/**
 * A [Face] backed by harfbuzzjs. Like the FreeType faces on the JVM, sizes are in pixels at 72 dpi, so an em is
 * [sizeInPoints] pixels, and outlines and bounds are in pixels with y pointing down.
 */
class FaceHarfBuzzJs internal constructor(
    val data: HarfBuzzFaceData,
    override val sizeInPoints: Double,
    private val realMaster: FaceMaster
) : Face {

    /**
     * The harfbuzzjs font of this face, at a scale of one unit per font unit, with the variation of [master]
     */
    val hbFont: HarfBuzzObject = hbCreateFont(data.harfBuzz, data.hbFace).also { font ->
        if (realMaster.isNotEmpty()) {
            val tags = realMaster.keys.map { data.axisTags.getValue(it) }
            hbSetVariations(data.harfBuzz, font, tags.joinToString(","), createFloat64Array(realMaster.values.toDoubleArray()))
        }
    }

    /** pixels per font unit */
    val scale: Double get() = sizeInPoints / data.unitsPerEm

    private val extents by lazy { hbHorizontalExtents(hbFont) }

    override val master: FaceMaster
        get() = realMaster.copy()

    override fun withMaster(master: FaceMaster): Face = FaceHarfBuzzJs(data, sizeInPoints, master.copy())

    override fun withSize(sizeInPoints: Double): Face = FaceHarfBuzzJs(data, sizeInPoints, realMaster.copy())

    override fun hashCode(): Int {
        var hash = data.hashCode()
        hash = hash * 31 + sizeInPoints.hashCode()
        return hash * 31 + realMaster.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceHarfBuzzJs) return false
        return data === other.data && sizeInPoints == other.sizeInPoints && realMaster == other.realMaster
    }

    override fun allCodePoints(): Sequence<Int> {
        val unicodes = hbCollectUnicodes(data.hbFace)
        return (0 until int32Length(unicodes)).asSequence().map { int32At(unicodes, it) }
    }

    override fun ascentMetrics(): Int = int32At(extents, 0)

    override fun descentMetrics(): Int = int32At(extents, 1)

    override fun lineGapMetrics(): Int = int32At(extents, 2)

    override fun unitsPerEm(): Int = data.unitsPerEm

    override val height: Double
        get() = (ascentMetrics() - descentMetrics() + lineGapMetrics()) * scale

    override val ascent: Double
        get() = ascentMetrics() * scale

    override val descent: Double
        get() = descentMetrics() * scale

    override val lineGap: Double
        get() = height - ascent - descent.absoluteValue

    override val xHeight: Double
        get() = glyphForCharacter('x').bounds().position(0.0, 0.0).y.absoluteValue

    override val capHeight: Double
        get() = glyphForCharacter('H').bounds().position(0.0, 0.0).y.absoluteValue

    override val emWidth: Double
        get() = scale

    override fun kernAdvance(left: Char, right: Char): Double {
        val shaped = shapeGlyphs("$left$right", kerningFeatures)
        if (shaped.size != 2 * SHAPED_STRIDE) {
            return 0.0
        }
        val glyph = shaped[0]
        return (shaped[2] - hbGlyphHAdvance(hbFont, glyph)) * scale
    }

    /**
     * Shapes [text] with harfbuzz and returns, for each glyph, its glyph index, cluster, x advance, y advance, x offset
     * and y offset, in font units, see [SHAPED_STRIDE]
     * @param features feature settings in harfbuzz's syntax, like `kern` or `-liga`
     * @param direction a harfbuzz direction, 0 to guess it from the text
     * @param script an ISO 15924 script tag, empty to guess it from the text
     * @param language a BCP 47 language tag, empty to guess it from the text
     */
    fun shapeGlyphs(
        text: String,
        features: List<String> = emptyList(),
        direction: Int = 0,
        script: String = "",
        language: String = ""
    ): IntArray {
        val shaped = hbShape(data.harfBuzz, hbFont, text, features.joinToString(","), direction, script, language)
        return IntArray(int32Length(shaped)) { int32At(shaped, it) }
    }

    override fun glyphForCharacter(character: Char): Glyph = glyphForCodePoint(character.code)

    override fun glyphForCodePoint(codePoint: Int): Glyph {
        val index = hbNominalGlyph(hbFont, codePoint)
        return GlyphHarfBuzzJs(this, if (index < 0) 0 else index, codePoint)
    }

    override fun glyphForIndex(glyphIndex: Int): Glyph = GlyphHarfBuzzJs(this, glyphIndex, -1)

    /**
     * The bounding box of all glyphs from the font header, in font units with y pointing up
     */
    override val bounds: Rectangle
        get() {
            val xMin = hbTableInt16(data.hbFace, "head", 36)
            val yMin = hbTableInt16(data.hbFace, "head", 38)
            val xMax = hbTableInt16(data.hbFace, "head", 40)
            val yMax = hbTableInt16(data.hbFace, "head", 42)
            return Rectangle(xMin.toDouble(), yMin.toDouble(), (xMax - xMin).toDouble(), (yMax - yMin).toDouble())
        }

    override val isVariable: Boolean
        get() = data.axisTags.isNotEmpty()

    override fun close() {
        // harfbuzzjs releases its objects when they are garbage collected
    }

    companion object {
        /** the number of values per glyph returned by [shapeGlyphs] */
        const val SHAPED_STRIDE = 6

        private val kerningFeatures = listOf("kern", "-liga", "-clig", "-dlig", "-calt", "-rlig")
    }
}

/**
 * Shapes text, returns the glyph index, cluster, x advance, y advance, x offset and y offset of each glyph
 */
private fun hbShape(
    hb: HarfBuzzModule,
    font: HarfBuzzObject,
    text: String,
    features: String,
    direction: Int,
    script: String,
    language: String
): js.typedarrays.Int32Array<*> = js(
    """(() => {
        const buffer = new hb.Buffer();
        buffer.addText(text);
        if (direction !== 0) buffer.setDirection(direction);
        if (script.length > 0) buffer.setScript(script);
        if (language.length > 0) buffer.setLanguage(language);
        buffer.guessSegmentProperties();
        const settings = features.length === 0 ? [] :
            features.split(',').map((f) => hb.Feature.fromString(f)).filter((f) => f !== undefined);
        hb.shape(font, buffer, settings);
        const infos = buffer.getGlyphInfos();
        const positions = buffer.getGlyphPositions();
        const result = new Int32Array(infos.length * 6);
        for (let i = 0; i < infos.length; i++) {
            result[i * 6] = infos[i].codepoint;
            result[i * 6 + 1] = infos[i].cluster;
            result[i * 6 + 2] = positions[i].xAdvance;
            result[i * 6 + 3] = positions[i].yAdvance;
            result[i * 6 + 4] = positions[i].xOffset;
            result[i * 6 + 5] = positions[i].yOffset;
        }
        return result;
    })()"""
)
