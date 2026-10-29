package org.openrndr.internal.glcommon

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.binpack.IntPacker
import org.openrndr.binpack.PackNode
import org.openrndr.draw.*
import org.openrndr.draw.font.loadFace
import org.openrndr.internal.FontMapManager
import org.openrndr.math.IntVector2
import org.openrndr.shape.IntRectangle
import org.openrndr.utils.buffer.MPPBuffer

private val logger = KotlinLogging.logger {}

/**
 * Creates [FontImageMap]s, maps of glyph bitmaps rasterized by the font driver, packed in a single color buffer
 */
class FontImageMapManager : FontMapManager() {
    override fun fontMapFromUrl(
        url: String,
        sizeInPoints: Double,
        characterSet: Set<Char>,
        contentScale: Double
    ): FontImageMap {
        logger.debug { "content scale $contentScale" }
        var packSize = 256

        val face = loadFace(url).withSize(sizeInPoints)

        val glyphDimensions = characterSet.associateWith { c ->
            val bounds = face.glyphForCharacter(c).bitmapBounds(contentScale)
            IntVector2(bounds.width, bounds.height)
        }
        val sanding = 3

        while (true) {
            val root = PackNode(IntRectangle(0, 0, packSize, packSize))
            val packer = IntPacker()
            if (attemptPack(root, packer, glyphDimensions, sanding)) {
                break
            } else {
                packSize *= 2
            }
        }
        logger.debug { "final map size ${packSize}x${packSize}" }

        val image = colorBuffer(packSize, packSize, 1.0, ColorFormat.R, ColorType.UINT8, session = Session.root)
        val map = mutableMapOf<Char, IntRectangle>()

        val root = PackNode(IntRectangle(0, 0, packSize, packSize))
        val packer = IntPacker()

        val glyphMetrics = mutableMapOf<Char, GlyphMetrics>()

        // the space between glyphs must be empty
        val bitmap = MPPBuffer.allocate(packSize * packSize)
        val empty: Byte = 0
        for (i in 0 until packSize * packSize) {
            bitmap.put(empty)
        }
        bitmap.rewind()


        logger.debug { "creating font bitmap" }
        glyphDimensions.entries.sortedByDescending { it.value.squaredLength }.forEach {
            val target = packer.insert(root, IntRectangle(0, 0, it.value.x + 2 * sanding, it.value.y + 2 * sanding))

            target?.let { t ->
                map[it.key] = IntRectangle(
                    t.area.x + sanding,
                    t.area.y + sanding,
                    t.area.width - 2 * sanding,
                    t.area.height - 2 * sanding
                )

                val glyph = face.glyphForCharacter(it.key)

                val bitmapBounds = glyph.bitmapBounds(contentScale)

                glyphMetrics[it.key] = GlyphMetrics(
                    advanceWidth = glyph.advanceWidth(),
                    leftSideBearing = glyph.leftSideBearing(),
                    xBitmapShift = bitmapBounds.x.toDouble(),
                    yBitmapShift = bitmapBounds.y.toDouble()
                )
                bitmap.rewind()
                bitmap.position((sanding + t.area.y) * packSize + sanding + t.area.x)
                glyph.rasterize(contentScale, bitmap, packSize, true)
            }
        }
        logger.debug { "uploading bitmap to color buffer" }
        bitmap.rewind()
        image.write(bitmap)

        val leading = face.lineGap
        return FontImageMap(
            image,
            map,
            glyphMetrics,
            sizeInPoints,
            contentScale,
            face.ascent,
            face.descent,
            face.height,
            leading,
            url
        ).apply {
            for (outer in characterSet) {
                for (inner in characterSet) {
                    kerningTable[CharacterPair(outer, inner)] = face.kernAdvance(outer, inner)
                }
            }
        }
    }
}

internal fun attemptPack(root: PackNode, packer: IntPacker, map: Map<Char, IntVector2>, sanding: Int = 2): Boolean {
    for (entry in map.values.sortedByDescending { it.squaredLength }) {
        val rectangle = IntRectangle(0, 0, entry.x + 2 * sanding, entry.y + 2 * sanding)
        if (rectangle.width <= 0 || rectangle.height <= 0) {
            throw RuntimeException("area < 0")
        }
        packer.insert(root, rectangle) ?: return false
    }
    return true
}
