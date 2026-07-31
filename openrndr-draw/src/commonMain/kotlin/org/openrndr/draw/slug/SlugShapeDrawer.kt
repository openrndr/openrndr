package org.openrndr.draw.slug

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.Drawer
import org.openrndr.draw.MagnifyingFilter
import org.openrndr.draw.MinifyingFilter
import org.openrndr.draw.colorBuffer
import org.openrndr.math.Matrix44
import org.openrndr.shape.Shape

class SlugShapeDrawer {

    val slugDrawer = SlugDrawer()
    val shapes: MutableMap<Int, Int> = mutableMapOf()
    val commands = mutableListOf<SlugCommand>()


    val slugMap = SlugMap(
        colorBuffer(4096, 64, type = ColorType.FLOAT32, format = ColorFormat.RG),
        colorBuffer(4096, 64, type = ColorType.UINT16_INT, format = ColorFormat.RGBa)
    ).apply {
        bands.filter(MinifyingFilter.NEAREST, MagnifyingFilter.NEAREST)
    }

    fun clear() {
        commands.clear()
    }
    fun addShape(shape: Shape, transform: Matrix44 = Matrix44.IDENTITY, fill: ColorRGBa, stroke: ColorRGBa = ColorRGBa.TRANSPARENT, strokeWeight: Double = 0.0) {
        val shapeHash = shape.hashCode()
        val slugIndex = shapes.getOrPut(shapeHash) { slugMap.addShape(shape) }

        commands.add(SlugCommand(slugIndex, transform, fill, stroke, strokeWeight))
    }

    fun addShape(slugIndex: Int, transform: Matrix44 = Matrix44.IDENTITY, fill: ColorRGBa, stroke: ColorRGBa = ColorRGBa.TRANSPARENT, strokeWeight: Double = 0.0) {
        commands.add(SlugCommand(slugIndex, transform, fill, stroke, strokeWeight))
    }
    fun draw(drawer: Drawer) {
        slugDrawer.prepare(slugMap, commands)
        slugDrawer.draw(drawer, slugMap)
    }

}