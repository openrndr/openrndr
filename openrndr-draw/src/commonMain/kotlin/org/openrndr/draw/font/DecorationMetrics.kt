package org.openrndr.draw.font

/**
 * The positions and thicknesses of the lines that decorate text: underlines and strikeouts (line-through), in pixels
 * like [Face.ascent]. Positions are those of the centers of the lines, positive above the baseline, so underlines
 * usually have a negative position. Overlines have no metrics in fonts, they are usually drawn at [Face.ascent].
 *
 * @property underlineFromFont are the underline metrics those of the font? Otherwise they are a fallback
 * @property strikeoutFromFont are the strikeout metrics those of the font? Otherwise they are a fallback
 */
data class DecorationMetrics(
    val underlinePosition: Double,
    val underlineThickness: Double,
    val strikeoutPosition: Double,
    val strikeoutThickness: Double,
    val underlineFromFont: Boolean,
    val strikeoutFromFont: Boolean
) {
    companion object {
        /**
         * The decoration metrics of [face] from the centers and thicknesses of the lines of the font, in pixels. Lines
         * the font has no metrics for, null values or thicknesses that are not positive, get the fallback: an
         * underline centered 0.075 em below the baseline, a strikeout centered on half of the x-height, both 0.05 em
         * thick.
         */
        fun of(
            face: Face,
            underlinePosition: Double? = null,
            underlineThickness: Double? = null,
            strikeoutPosition: Double? = null,
            strikeoutThickness: Double? = null
        ): DecorationMetrics {
            val em = face.sizeInPoints
            val underline = underlinePosition != null && underlineThickness != null && underlineThickness > 0.0
            val strikeout = strikeoutPosition != null && strikeoutThickness != null && strikeoutThickness > 0.0
            val fallbackThickness = 0.05 * em
            return DecorationMetrics(
                underlinePosition = if (underline) underlinePosition!! else -0.075 * em,
                underlineThickness = if (underline) underlineThickness!! else fallbackThickness,
                strikeoutPosition = if (strikeout) strikeoutPosition!! else {
                    val xHeight = face.xHeight
                    if (xHeight > 0.0) xHeight / 2.0 else 0.25 * em
                },
                strikeoutThickness = if (strikeout) strikeoutThickness!! else fallbackThickness,
                underlineFromFont = underline,
                strikeoutFromFont = strikeout
            )
        }
    }
}
