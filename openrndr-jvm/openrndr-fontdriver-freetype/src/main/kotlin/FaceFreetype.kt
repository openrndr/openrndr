package org.openrndr.fontdriver.freetype

import org.lwjgl.system.MemoryStack
import org.lwjgl.util.freetype.FT_Face
import org.lwjgl.util.freetype.FT_MM_Var
import org.lwjgl.util.freetype.FreeType.*
import org.lwjgl.util.harfbuzz.HarfBuzz.*
import org.lwjgl.util.harfbuzz.hb_feature_t
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.FaceMaster
import org.openrndr.draw.font.Glyph
import org.openrndr.shape.Rectangle
import kotlin.math.absoluteValue


private var activeFace: FaceFreetype? = null

class FaceFreetype(
    val ftLibrary: Long,
    val ftFace: FT_Face,
    override val sizeInPoints: Double,
    private val realMaster: FaceMaster
) : Face {

    internal fun makeActive() {
        if (activeFace != this) {
            FT_Set_Char_Size(ftFace, 0, (sizeInPoints * 64).toLong(), 72, 72)
            applyMaster()
            activeFace = this
        }
    }
    private fun applyMaster() {
        if (!isVariable) {
            return
        }

        MemoryStack.stackPush().use { stack ->
            val mmVarPtr = stack.mallocPointer(1)
            val result = FT_Get_MM_Var(ftFace, mmVarPtr)

            if (result != 0) {
                return
            }

            val mmVar = FT_MM_Var.create(mmVarPtr.get(0))
            val numAxes = mmVar.num_axis()

            // Get current design coordinates
            val coords = stack.mallocCLong(numAxes)
            val coordResult = FT_Get_Var_Design_Coordinates(ftFace, coords)

            if (coordResult != 0) {
                FT_Done_MM_Var(ftLibrary, mmVar)
                return
            }

            // Update coordinates for each axis in realMaster
            for (i in 0 until numAxes) {
                val mmAxis = mmVar.axis().get(i)
                val axisName = mmAxis.nameString()

                val value = realMaster[axisName]
                if (value != null) {
                    // Get axis range and clamp the value
                    val minValue = mmAxis.minimum().toDouble() / 65536.0
                    val maxValue = mmAxis.maximum().toDouble() / 65536.0
                    val clampedValue = value.coerceIn(minValue, maxValue)

                    // Update the axis value (convert Double to FT_Fixed)
                    coords.put(i, (clampedValue * 65536.0).toLong())
                }
            }

            coords.rewind()
            // Set all design coordinates with a single call
            FT_Set_Var_Design_Coordinates(ftFace, coords)
            FT_Done_MM_Var(ftLibrary, mmVar)
        }
    }

    override val master: FaceMaster
        get() {
            return realMaster.copy()
        }

    override fun withMaster(master: FaceMaster): Face {
        FT_Reference_Face(ftFace)
        return FaceFreetype(ftLibrary, ftFace, sizeInPoints, master.copy())
    }

    override fun withSize(sizeInPoints: Double): Face {
        FT_Reference_Face(ftFace)
        return FaceFreetype(ftLibrary, ftFace, sizeInPoints, realMaster.copy())
    }

    private var masterHash = realMaster.hashCode()


    private val staticHash = run {
        var hash = 0
        hash = hash * 31 + ftFace.hashCode()
        hash = hash * 31 + sizeInPoints.hashCode()
        hash
    }

    override fun hashCode(): Int {
        return staticHash * 31 + masterHash
    }

    override fun allCodePoints(): Sequence<Int> = sequence {
        makeActive()
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
        makeActive()
        return ftFace.ascender().toInt()
    }

    override fun descentMetrics(): Int {
        makeActive()
        return ftFace.descender().toInt()
    }

    override fun lineGapMetrics(): Int {
        makeActive()
        return 0
    }

    override fun unitsPerEm(): Int {
        makeActive()
        return ftFace.units_per_EM().toInt()
    }

    val ppxem: Short
        get() {
            makeActive()
            return ftFace.size()?.metrics()?.x_ppem() ?: 0
        }

    val ppyem : Short
        get() {
            makeActive()
            return ftFace.size()?.metrics()?.y_ppem() ?: 0
        }

    override val height: Double
        get()  {
            makeActive()
            return ppyem * ftFace.height().toDouble() / ftFace.units_per_EM().toDouble()
        } 


    override val ascent: Double
        get() {
            makeActive()
            return ppyem * ftFace.ascender().toDouble() / ftFace.units_per_EM().toDouble()
        }

    override val descent: Double
        get() {
            makeActive()
            return ppyem * ftFace.descender().toDouble() / ftFace.units_per_EM().toDouble()
        } 
    

    
    
    override val lineGap: Double
        get() {
            makeActive()
            return height - ascent - descent.absoluteValue
        }

    override val xHeight: Double
        get() {
            makeActive()
            return glyphForCharacter('x').bounds().position(0.0, 0.0).y.absoluteValue
        }

    override val capHeight: Double
        get() {
            makeActive()
            return glyphForCharacter('H').bounds().position(0.0, 0.0).y.absoluteValue
        }

    override val emWidth: Double
        get() {
            makeActive()
            return ppxem / ftFace.units_per_EM().toDouble()

        }

    /**
     * HarfBuzz font for this face, created while this face is active such that it uses this face's size and master
     */
    private val hbFontDelegate = lazy {
        makeActive()
        hb_ft_font_create_referenced(ftFace.address()).also {
            require(it != 0L) { "failed to create HarfBuzz font" }
        }
    }
    private val hbFont by hbFontDelegate

    private val hbBufferDelegate = lazy { hb_buffer_create() }
    private val hbBuffer by hbBufferDelegate

    /**
     * Handles shaping with this face's HarfBuzz font.
     *
     * The face is made active before [shape] is executed, such that the HarfBuzz font reads glyph metrics at this
     * face's size and master. The HarfBuzz font is owned by this face and should not be destroyed by the caller.
     *
     * @param shape The lambda function that performs the shaping, it receives the HarfBuzz font handle.
     */
    fun <T> shaping(shape: (hbFont: Long) -> T): T {
        makeActive()
        return shape(hbFont)
    }

    /**
     * Calculates the kerning between two characters by shaping the pair with HarfBuzz. This uses GPOS pair
     * positioning and falls back to the legacy 'kern' table for fonts without GPOS kerning.
     */
    override fun kernAdvance(left: Char, right: Char): Double = shaping { font ->
        val buffer = hbBuffer
        MemoryStack.stackPush().use { stack ->
            hb_buffer_clear_contents(buffer)
            hb_buffer_add_codepoints(buffer, stack.ints(left.code, right.code), 0, 2)
            hb_buffer_set_direction(buffer, HB_DIRECTION_LTR)
            hb_buffer_guess_segment_properties(buffer)
            hb_shape(font, buffer, kerningFeatures)

            val infos = hb_buffer_get_glyph_infos(buffer) ?: return@shaping 0.0
            val positions = hb_buffer_get_glyph_positions(buffer) ?: return@shaping 0.0
            if (infos.remaining() != 2) {
                0.0
            } else {
                val nominalAdvance = hb_font_get_glyph_h_advance(font, infos[0].codepoint())
                (positions[0].x_advance() - nominalAdvance) / 64.0
            }
        }
    }

    override fun glyphForCharacter(character: Char): GlyphFreetype {
        makeActive()
        val index = FT_Get_Char_Index(ftFace, character.code.toLong())
        FT_Load_Glyph(ftFace, index, FT_LOAD_DEFAULT)
        return GlyphFreetype(this, character, index, character.code)
    }

    override fun glyphForCodePoint(codePoint: Int): Glyph {
        makeActive()
        val index = FT_Get_Char_Index(ftFace, codePoint.toLong())
        FT_Load_Glyph(ftFace, index, FT_LOAD_DEFAULT)
        return GlyphFreetype(this, Char(codePoint), index, codePoint)
    }

    override fun glyphForIndex(glyphIndex: Int): Glyph {
        makeActive()
        FT_Load_Glyph(ftFace, glyphIndex, FT_LOAD_DEFAULT)
        return GlyphFreetype(this, Char(0xffff), glyphIndex, -1)
    }

    
    /**
     * Handles rasterization of glyphs with temporary adjustment of the character size.
     *
     * The rasterization is performed within the provided `rasterize` function.
     * The method adjusts the FreeType character size settings before and after
     * the rasterization block to accommodate high-resolution rendering and scaling.
     *
     * @param contentScale The content scale at which to rasterize.
     * @param rasterize The lambda function that performs the actual rasterization logic.
     * It will be executed while specific FreeType character size settings are applied.
     */
    fun <T> rasterizing(contentScale: Double, rasterize: () -> T): T {
        makeActive()
        FT_Set_Char_Size(
            ftFace, 0, (sizeInPoints * 64).toLong(), (72 * contentScale).toInt(),
            (72 * contentScale).toInt()
        )
        return try {
            rasterize()
        } finally {
            FT_Set_Char_Size(ftFace, 0, (sizeInPoints * 64).toLong(), 72, 72)
        }
    }

    override val bounds: Rectangle
        get() {
            makeActive()
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
        if (activeFace == this) {
            activeFace = null
        }
        if (hbFontDelegate.isInitialized()) {
            hb_font_destroy(hbFont)
        }
        if (hbBufferDelegate.isInitialized()) {
            hb_buffer_destroy(hbBuffer)
        }
        FT_Done_Face(ftFace)
    }

    override val isVariable: Boolean
        get() {
            return (ftFace.face_flags() and FT_FACE_FLAG_MULTIPLE_MASTERS.toLong()) != 0L
        }

    companion object {
        /**
         * Features for kerning: kerning on, substitutions that could merge or replace the pair off
         */
        private val kerningFeatures: hb_feature_t.Buffer by lazy {
            val tags = listOf("kern", "-liga", "-clig", "-dlig", "-calt", "-rlig")
            val features = hb_feature_t.calloc(tags.size)
            for ((index, tag) in tags.withIndex()) {
                require(hb_feature_from_string(tag, features[index])) { "invalid feature '$tag'" }
            }
            features
        }


        /**
         * Constructs a `FaceMaster` object containing axis mappings and their associated ranges
         * based on the configuration of a given FreeType font face. The method supports variable fonts
         * and extracts axis-specific details if available.
         *
         * @param ftLibrary The FreeType library pointer, required for memory management of `FT_MM_Var`.
         * @param ftFace The FreeType face object representing the font face.
         * @return A `FaceMaster` object populated with axis mappings and ranges. If the font face
         *         is not a variable font or an error occurs, an empty `FaceMaster` is returned.
         */


        internal fun masterForFace(ftLibrary: Long, ftFace: FT_Face): FaceMaster {
            val axisMap = mutableMapOf<String, Double>()
            val rangesMap = mutableMapOf<String, ClosedFloatingPointRange<Double>>()

            // Check if the face is a variable font
            if ((ftFace.face_flags() and FT_FACE_FLAG_MULTIPLE_MASTERS.toLong()) == 0L) {
                return FaceMaster(axisMap, rangesMap)
            }

            MemoryStack.stackPush().use { stack ->
                val mmVarPtr = stack.mallocPointer(1)
                val result = FT_Get_MM_Var(ftFace, mmVarPtr)

                if (result != 0) {
                    return FaceMaster(axisMap, rangesMap)
                }

                val mmVar = FT_MM_Var.create(mmVarPtr.get(0))
                val numAxes = mmVar.num_axis()

                // Get current design coordinates
                val coords = stack.mallocCLong(numAxes)
                val coordResult = FT_Get_Var_Design_Coordinates(ftFace, coords)

                if (coordResult != 0) {
                    FT_Done_MM_Var(ftLibrary, mmVar)
                    return FaceMaster(axisMap, rangesMap)
                }

                // Collect all axis names and their current values
                for (i in 0 until numAxes) {
                    val mmAxis = mmVar.axis().get(i)
                    val axisName = mmAxis.nameString()
                    val axisValue = coords[i].toDouble() / 65536.0
                    axisMap[axisName] = axisValue

                    val minValue = mmAxis.minimum().toDouble() / 65536.0
                    val maxValue = mmAxis.maximum().toDouble() / 65536.0
                    rangesMap[axisName] = minValue..maxValue
                }

                FT_Done_MM_Var(ftLibrary, mmVar)
            }

            return FaceMaster(axisMap, rangesMap)
        }
    }
}