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
import org.lwjgl.util.freetype.FreeType.FT_FACE_FLAG_MULTIPLE_MASTERS
import org.lwjgl.util.freetype.FreeType.FT_Set_Char_Size
import org.lwjgl.util.freetype.FreeType.FT_Get_MM_Var
import org.lwjgl.util.freetype.FT_MM_Var
import org.lwjgl.util.freetype.FreeType.FT_Done_MM_Var
import org.lwjgl.util.freetype.FreeType.FT_Get_Var_Design_Coordinates
import org.lwjgl.util.freetype.FreeType.FT_Reference_Face
import org.lwjgl.util.freetype.FreeType.FT_Set_Var_Design_Coordinates
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
    override val contentScale: Double,
    private val realMaster: FaceMaster
) : Face {

    internal fun makeActive() {
        if (activeFace != this) {
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
        return FaceFreetype(ftLibrary, ftFace, sizeInPoints, contentScale, master.copy())
    }

    private var masterHash = realMaster.hashCode()


    private val staticHash = run {
        var hash = 0
        hash = hash * 31 + ftFace.hashCode()
        hash = hash * 31 + sizeInPoints.hashCode()
        hash = hash * 31 + contentScale.hashCode()
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
        get() = ftFace.size()?.metrics()?.x_ppem() ?: 0
    
    val ppyem : Short
        get() = ftFace.size()?.metrics()?.y_ppem() ?: 0

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

    override fun kernAdvance(left: Char, right: Char): Double {
        makeActive()
        val leftIndex = FT_Get_Char_Index(ftFace, left.code.toLong())
        val rightIndex = FT_Get_Char_Index(ftFace, right.code.toLong())
        val kerning = FT_Vector.malloc()
        FT_Get_Kerning(ftFace, leftIndex, rightIndex, FT_KERNING_DEFAULT, kerning)
        val result = kerning.x().toDouble()
        kerning.free()
        return result
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
     * @param rasterize The lambda function that performs the actual rasterization logic.
     * It will be executed while specific FreeType character size settings are applied.
     */
    fun rasterizing(rasterize: () -> Unit) {
        makeActive()
        FT_Set_Char_Size(
            ftFace, 0, (sizeInPoints * 64).toLong(), (72 * contentScale).toInt(),
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
        FT_Done_Face(ftFace)
    }

    override val isVariable: Boolean
        get() {
            return (ftFace.face_flags() and FT_FACE_FLAG_MULTIPLE_MASTERS.toLong()) != 0L
        }

    companion object {

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