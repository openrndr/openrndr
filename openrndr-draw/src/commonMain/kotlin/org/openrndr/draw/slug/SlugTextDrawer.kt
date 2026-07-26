package org.openrndr.draw.slug

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.Drawer
import org.openrndr.draw.MagnifyingFilter
import org.openrndr.draw.MinifyingFilter
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform

class SlugTextDrawer {

    val shaper = TextShapingDriver.instance

    val slugMap = SlugMap(
        colorBuffer(4096, 64, type = ColorType.FLOAT32, format = ColorFormat.RG),
        colorBuffer(4096, 64, type = ColorType.UINT16_INT, format = ColorFormat.RGBa)
    ).apply {
        bands.filter(MinifyingFilter.NEAREST, MagnifyingFilter.NEAREST)
    }

    val slugGlyphMap = SlugGlyphMap(slugMap)

    val slugDrawer = SlugDrawer()


    val commands = mutableListOf<SlugCommand>()

    fun addText(face: Face, text: String, position: Vector2) {
        shaper.shape(face, text)

        val shapeResult = shaper.shape(face, text)

        for (item in shapeResult) {
            slugGlyphMap.getSlugForGlyphIndex(face, item.glyphIndex)
        }

        var cursor = position

        for ((sindex, i) in shapeResult.withIndex()) {
            val slugIndex = slugGlyphMap.getSlugForGlyphIndex(face, i.glyphIndex)
            val command = SlugCommand(
                slugIndex,
                transform {
                    translate(cursor + i.offset * 0.0)
                },
                ColorRGBa.WHITE,
                ColorRGBa.TRANSPARENT,
                0.0,

            )
            commands.add(command)
            cursor += shapeResult[sindex].advance
        }
    }

    fun clear() {
        commands.clear()
    }

    fun draw(drawer: Drawer) {
        slugDrawer.prepare(slugMap, commands)
        slugDrawer.draw(drawer, slugMap)
    }

}