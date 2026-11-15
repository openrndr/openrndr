package org.openrndr.draw.font

/**
 * The OpenType MATH table of a face, the constants and glyph information a math layout engine needs: fractions,
 * scripts, radicals and stretchy operators. Lengths are in pixels at the size of the face, the units of the advances
 * of [shaping results][org.openrndr.draw.font.internal.ShapeResult], with y pointing up as in the font.
 *
 * @see org.openrndr.draw.font.internal.TextShapingDriver.mathTable
 */
interface MathTable {
    /**
     * The value of [constant]: a length, or for [MathConstant.SCRIPT_PERCENT_SCALE_DOWN],
     * [MathConstant.SCRIPT_SCRIPT_PERCENT_SCALE_DOWN] and [MathConstant.RADICAL_DEGREE_BOTTOM_RAISE_PERCENT] the
     * percentage as it is stored in the font
     */
    fun constant(constant: MathConstant): Double

    /** the italic correction of the glyph [glyphIndex], 0 when it has none */
    fun italicCorrection(glyphIndex: Int): Double

    /** the horizontal position at which accents attach to the glyph [glyphIndex], null when the font has none */
    fun topAccentAttachment(glyphIndex: Int): Double?

    /** is the glyph [glyphIndex] an extended shape, one whose height is not the height of its line? */
    fun isExtendedShape(glyphIndex: Int): Boolean

    /**
     * The kerning of the glyph [glyphIndex] at its [corner], for an attached glyph at [correctionHeight] above the
     * baseline
     */
    fun kern(glyphIndex: Int, corner: MathKernCorner, correctionHeight: Double): Double

    /**
     * The variants of the glyph [glyphIndex] in [direction], from small to large, as the font lists them, which
     * usually starts with the glyph itself
     */
    fun variants(glyphIndex: Int, direction: MathDirection): List<MathGlyphVariant>

    /** the assembly of parts that stretches the glyph [glyphIndex] in [direction], null when it has none */
    fun assembly(glyphIndex: Int, direction: MathDirection): MathGlyphAssembly?

    /** the minimum overlap of the connectors of the parts of assemblies in [direction] */
    fun minConnectorOverlap(direction: MathDirection): Double
}

/** The constants of the OpenType MATH table, in the order of the table */
enum class MathConstant {
    SCRIPT_PERCENT_SCALE_DOWN,
    SCRIPT_SCRIPT_PERCENT_SCALE_DOWN,
    DELIMITED_SUB_FORMULA_MIN_HEIGHT,
    DISPLAY_OPERATOR_MIN_HEIGHT,
    MATH_LEADING,
    AXIS_HEIGHT,
    ACCENT_BASE_HEIGHT,
    FLATTENED_ACCENT_BASE_HEIGHT,
    SUBSCRIPT_SHIFT_DOWN,
    SUBSCRIPT_TOP_MAX,
    SUBSCRIPT_BASELINE_DROP_MIN,
    SUPERSCRIPT_SHIFT_UP,
    SUPERSCRIPT_SHIFT_UP_CRAMPED,
    SUPERSCRIPT_BOTTOM_MIN,
    SUPERSCRIPT_BASELINE_DROP_MAX,
    SUB_SUPERSCRIPT_GAP_MIN,
    SUPERSCRIPT_BOTTOM_MAX_WITH_SUBSCRIPT,
    SPACE_AFTER_SCRIPT,
    UPPER_LIMIT_GAP_MIN,
    UPPER_LIMIT_BASELINE_RISE_MIN,
    LOWER_LIMIT_GAP_MIN,
    LOWER_LIMIT_BASELINE_DROP_MIN,
    STACK_TOP_SHIFT_UP,
    STACK_TOP_DISPLAY_STYLE_SHIFT_UP,
    STACK_BOTTOM_SHIFT_DOWN,
    STACK_BOTTOM_DISPLAY_STYLE_SHIFT_DOWN,
    STACK_GAP_MIN,
    STACK_DISPLAY_STYLE_GAP_MIN,
    STRETCH_STACK_TOP_SHIFT_UP,
    STRETCH_STACK_BOTTOM_SHIFT_DOWN,
    STRETCH_STACK_GAP_ABOVE_MIN,
    STRETCH_STACK_GAP_BELOW_MIN,
    FRACTION_NUMERATOR_SHIFT_UP,
    FRACTION_NUMERATOR_DISPLAY_STYLE_SHIFT_UP,
    FRACTION_DENOMINATOR_SHIFT_DOWN,
    FRACTION_DENOMINATOR_DISPLAY_STYLE_SHIFT_DOWN,
    FRACTION_NUMERATOR_GAP_MIN,
    FRACTION_NUM_DISPLAY_STYLE_GAP_MIN,
    FRACTION_RULE_THICKNESS,
    FRACTION_DENOMINATOR_GAP_MIN,
    FRACTION_DENOM_DISPLAY_STYLE_GAP_MIN,
    SKEWED_FRACTION_HORIZONTAL_GAP,
    SKEWED_FRACTION_VERTICAL_GAP,
    OVERBAR_VERTICAL_GAP,
    OVERBAR_RULE_THICKNESS,
    OVERBAR_EXTRA_ASCENDER,
    UNDERBAR_VERTICAL_GAP,
    UNDERBAR_RULE_THICKNESS,
    UNDERBAR_EXTRA_DESCENDER,
    RADICAL_VERTICAL_GAP,
    RADICAL_DISPLAY_STYLE_VERTICAL_GAP,
    RADICAL_RULE_THICKNESS,
    RADICAL_EXTRA_ASCENDER,
    RADICAL_KERN_BEFORE_DEGREE,
    RADICAL_KERN_AFTER_DEGREE,
    RADICAL_DEGREE_BOTTOM_RAISE_PERCENT;

    /** is the constant a percentage instead of a length? */
    val isPercent: Boolean
        get() = this == SCRIPT_PERCENT_SCALE_DOWN || this == SCRIPT_SCRIPT_PERCENT_SCALE_DOWN ||
                this == RADICAL_DEGREE_BOTTOM_RAISE_PERCENT
}

/** The corners of a glyph that math kerning applies to */
enum class MathKernCorner { TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT }

/** The direction in which math glyphs stretch */
enum class MathDirection { HORIZONTAL, VERTICAL }

/** A variant of a math glyph: the glyph [glyphIndex] with an [advance] in the direction it stretches in */
data class MathGlyphVariant(val glyphIndex: Int, val advance: Double)

/**
 * A part of a [MathGlyphAssembly]: the glyph [glyphIndex], the lengths of its connectors at its start and end, its
 * full advance in the direction of the assembly, and whether it is an extender, a part that can be repeated
 */
data class MathGlyphPart(
    val glyphIndex: Int,
    val startConnectorLength: Double,
    val endConnectorLength: Double,
    val fullAdvance: Double,
    val isExtender: Boolean
)

/**
 * The parts that stretch a glyph, from bottom to top or from left to right, and the italic correction of the
 * assembled glyph
 */
data class MathGlyphAssembly(val parts: List<MathGlyphPart>, val italicCorrection: Double)
