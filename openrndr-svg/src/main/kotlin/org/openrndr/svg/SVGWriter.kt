package org.openrndr.svg

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix44
import org.openrndr.shape.*
import java.io.File

fun Composition.saveToFile(file: File) {
    if (file.extension == "svg") {
        val svg = writeSVG(this)
        file.writeText(svg)
    } else {
        throw IllegalArgumentException("can only write svg files, the extension '${file.extension}' is not supported")
    }
}

fun writeSVG(composition: Composition): String {
    val sb = StringBuilder()
    sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
    sb.append("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1 Tiny//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11-tiny.dtd\">\n")
    sb.append("<svg version=\"1.1\" baseProfile=\"tiny\" id=\"Layer_1\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"  x=\"0px\" y=\"0px\"\n width=\"2676px\" height=\"2048px\">")

    process(composition.root) {
        if (it == VisitStage.PRE) {
            when (this) {
                is GroupNode -> {
                    if (transform != Matrix44.IDENTITY) {
                        sb.append("<g transform=\"${transform.svg}\">\n")
                    } else {
                        sb.append("<g>\n")
                    }
                }
                is ShapeNode -> {
                    val fillAttribute = fill.let {
                        if (it is Color) it.color?.let { "fill=\"${it.svg}\"" } ?: "fill=\"none\"" else ""
                    }
                    val strokeAttribute = stroke.let {
                        if (it is Color) it.color?.let { "stroke=\"${it.svg}\"" } ?: "stroke=\"none\"" else ""
                    }
                    val strokeWidthAttribute = "stroke-weight=\"1.0\""
                    val pathAttribute = "d=\"${shape.svg}\""
                    sb.append("<path $fillAttribute $strokeAttribute $strokeWidthAttribute $pathAttribute/>\n")
                }
            }
        } else {
            when (this) {
                is GroupNode -> {
                    sb.append("</g>\n")
                }
            }
        }
    }
    sb.append("</svg>")
    return sb.toString()
}

private val ColorRGBa.svg: String
    get() {
        val ir = (r.coerceIn(0.0, 1.0) * 255.0).toInt()
        val ig = (g.coerceIn(0.0, 1.0) * 255.0).toInt()
        val ib = (b.coerceIn(0.0, 1.0) * 255.0).toInt()
        return String.format("#%02X%02x%02x", ir, ig, ib)
    }

private val Matrix44.svg get() = "matrix(${this.c0r0}, ${this.c0r1}, ${this.c1r0}, ${this.c1r1}, ${this.c3r0}, ${this.c3r1})"

private val Shape.svg: String
    get() {
        val sb = StringBuilder()
        contours.forEach {
            it.segments.forEachIndexed { index, segment ->
                if (index == 0) {
                    sb.append("M ${segment.start.x}, ${segment.start.y}")
                }
                sb.append(when (segment.control.size) {
                    1 -> "C${segment.control[0].x}, ${segment.control[0].y}, ${segment.end.x}, ${segment.end.y}"
                    2 -> "C${segment.control[0].x}, ${segment.control[0].y}, ${segment.control[1].x}, ${segment.control[1].y}, ${segment.end.x}, ${segment.end.y}"
                    else -> "L${segment.end.x}, ${segment.end.y}"
                })
            }
            if (it.closed) {
                sb.append("Z ")
            }
        }
        return sb.toString()
    }

private enum class VisitStage {
    PRE,
    POST
}

private fun process(compositionNode: CompositionNode, visitor: CompositionNode.(stage: VisitStage) -> Unit) {
    compositionNode.visitor(VisitStage.PRE)
    if (compositionNode is GroupNode) {
        compositionNode.children.forEach { process(it, visitor) }
    }
    compositionNode.visitor(VisitStage.POST)
}