package org.openrndr.internal.gl3

import mu.KotlinLogging
import org.lwjgl.system.MemoryUtil
import org.openrndr.binpack.IntPacker
import org.openrndr.binpack.PackNode
import org.openrndr.draw.*
import org.openrndr.draw.font.loadFace
import org.openrndr.internal.FontMapManager
import org.openrndr.math.IntVector2
import org.openrndr.shape.IntRectangle
import org.openrndr.utils.buffer.MPPBuffer
import java.nio.Buffer

private val logger = KotlinLogging.logger {}

class FontImageMapManager : FontMapManager() {
    override fun fontMapFromUrl(
        url: String,
        size: Double,
        characterSet: Set<Char>,
        contentScale: Double
    ): FontImageMap {
        logger.debug { "content scale $contentScale" }
        var packSize = 256

        val face = loadFace(url)

        val glyphDimensions = characterSet.associateWith { c ->
            val bounds = face.glyphForCharacter(c).bitmapBounds(size * contentScale)
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

        val image = colorBuffer(packSize, packSize, 1.0, ColorFormat.R, session = Session.root)
        val map = mutableMapOf<Char, IntRectangle>()

        val root = PackNode(IntRectangle(0, 0, packSize, packSize))
        val packer = IntPacker()

        val glyphMetrics = mutableMapOf<Char, GlyphMetrics>()

        val bitmap = MemoryUtil.memAlloc(packSize * packSize)

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

                val bitmapBounds = glyph.bitmapBounds(size * contentScale)

                glyphMetrics[it.key] = GlyphMetrics(
                    advanceWidth = glyph.advanceWidth(size),
                    leftSideBearing = glyph.leftSideBearing(size),
                    xBitmapShift = bitmapBounds.x.toDouble(),
                    yBitmapShift = bitmapBounds.y.toDouble()
                )
                (bitmap as Buffer).rewind()
                (bitmap as Buffer).position((sanding + t.area.y) * packSize + sanding + t.area.x)
                glyph.rasterize(size * contentScale, MPPBuffer(bitmap), packSize, true)
            }
        }
        logger.debug { "uploading bitmap to color buffer" }
        (bitmap as Buffer).rewind()
        image.write(bitmap)
        MemoryUtil.memFree(bitmap)

        val ascent = face.ascent(size * contentScale)
        val descent = face.descent(size * contentScale)
        val leading = face.lineSpace(size * contentScale)
        return FontImageMap(
            image,
            map,
            glyphMetrics,
            size,
            contentScale,
            ascent / contentScale,
            descent / contentScale,
            (ascent + descent) / contentScale,
            leading / contentScale,
            url
        ).apply {
            for (outer in characterSet) {
                for (inner in characterSet) {
                    kerningTable[CharacterPair(outer, inner)] = face.kernAdvance(size * contentScale, outer, inner)
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
