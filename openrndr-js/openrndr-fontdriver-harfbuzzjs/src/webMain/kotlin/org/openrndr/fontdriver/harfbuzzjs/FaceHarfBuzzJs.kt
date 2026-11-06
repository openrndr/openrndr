@file:OptIn(ExperimentalWasmJsInterop::class)

package org.openrndr.fontdriver.harfbuzzjs

import org.openrndr.draw.font.DecorationMetrics
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

    /**
     * The font of vertical text: harfbuzzjs does not read vertical metrics, this sub font of [hbFont] has the vertical
     * metrics of HarfBuzz's OpenType font functions, like the FreeType driver on the JVM
     */
    private val verticalFont: HarfBuzzObject by lazy { hbVerticalFont(data.harfBuzz, hbFont, data.hbFace) }

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

    override val decorationMetrics: DecorationMetrics
        get() {
            fun metric(tag: String): Double? {
                val code = tag.fold(0) { code, c -> (code shl 8) or c.code }
                return hbMetricPosition(hbFont, code).takeIf { !it.isNaN() }?.let { it * scale }
            }
            // the offsets of the post and OS/2 tables are those of the tops of the lines
            val underlineThickness = metric("unds")
            val underlinePosition = metric("undo")?.let { it - (underlineThickness ?: 0.0) / 2.0 }
            val strikeoutThickness = metric("strs")
            val strikeoutPosition = metric("stro")?.let { it - (strikeoutThickness ?: 0.0) / 2.0 }
            return DecorationMetrics.of(this, underlinePosition, underlineThickness, strikeoutPosition, strikeoutThickness)
        }

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
        val font = if (direction == 6 || direction == 7) verticalFont else hbFont
        val shaped = hbShape(data.harfBuzz, font, text, features.joinToString(","), direction, script, language)
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
 * A sub font of [font] with the vertical metrics of HarfBuzz's OpenType font functions, in font units: advances from
 * vmtx, origins from VORG, or from vmtx and the extents of the glyph, or, without vertical metrics, an advance of the
 * ascender minus the descender with the glyph centered on it
 */
private fun hbVerticalFont(hb: HarfBuzzModule, font: HarfBuzzObject, face: HarfBuzzObject): HarfBuzzObject = js(
    """(() => {
        // the tables are copied, views of the memory of harfbuzz are detached when the memory grows
        const view = (tag) => {
            const table = face.referenceTable(tag);
            return table && table.byteLength > 0 ? new DataView(table.slice().buffer) : null;
        };
        const vmtx = view('vmtx');
        const vhea = view('vhea');
        const vorg = view('VORG');
        const longMetrics = vhea && vhea.byteLength >= 36 ? vhea.getUint16(34) : 0;
        const hasVmtx = vmtx !== null && longMetrics > 0 && vmtx.byteLength >= longMetrics * 4;
        const extents = font.hExtents();
        const fallbackAdvance = extents.ascender - extents.descender;
        const advance = (glyph) => hasVmtx ? vmtx.getUint16(Math.min(glyph, longMetrics - 1) * 4) : fallbackAdvance;
        const topSideBearing = (glyph) => {
            if (!hasVmtx) return null;
            if (glyph < longMetrics) return vmtx.getInt16(glyph * 4 + 2);
            const offset = longMetrics * 4 + (glyph - longMetrics) * 2;
            return offset + 2 <= vmtx.byteLength ? vmtx.getInt16(offset) : null;
        };
        const vorgOrigin = (glyph) => {
            let low = 0;
            let high = vorg.getUint16(6) - 1;
            while (low <= high) {
                const middle = (low + high) >> 1;
                const index = vorg.getUint16(8 + middle * 4);
                if (index === glyph) return vorg.getInt16(10 + middle * 4);
                if (index < glyph) low = middle + 1; else high = middle - 1;
            }
            return vorg.getInt16(4);
        };
        const funcs = new hb.FontFuncs();
        // y points up in harfbuzz, vertical advances are negative
        funcs.setGlyphVAdvanceFunc((f, glyph) => -advance(glyph));
        funcs.setGlyphVOriginFunc((f, glyph) => {
            const x = Math.trunc(font.glyphHAdvance(glyph) / 2);
            if (vorg !== null) return [x, vorgOrigin(glyph)];
            const glyphExtents = font.glyphExtents(glyph);
            if (glyphExtents) {
                const tsb = topSideBearing(glyph);
                if (tsb !== null) return [x, glyphExtents.yBearing + tsb];
                return [x, glyphExtents.yBearing + ((fallbackAdvance + glyphExtents.height) >> 1)];
            }
            return [x, extents.ascender];
        });
        const sub = font.subFont();
        sub.setFuncs(funcs);
        return sub;
    })()"""
)

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
