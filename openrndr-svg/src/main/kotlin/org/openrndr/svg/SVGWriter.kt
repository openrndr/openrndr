package org.openrndr.svg

import org.jsoup.nodes.Entities
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

private val CompositionNode.svgId: String
    get() = if (id != null) {
        "id=${id ?: error("id = null")}"
    } else {
        ""
    }

private val CompositionNode.svgAttributes: String
    get() {
        return attributes.map {
            if (it.value != null) {
                "${it.key}=\"${Entities.escape(it.value)}\""
            } else {
                "${it.key}"
            }
        }.joinToString(" ")
    }

fun writeSVG(composition: Composition): String {
    val sb = StringBuilder()
    sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
    sb.append("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1 Tiny//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11-tiny.dtd\">\n")
    sb.append("<svg version=\"1.1\" baseProfile=\"tiny\" id=\"Layer_1\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"  x=\"0px\" y=\"0px\"\n width=\"2676px\" height=\"2048px\">")

    var textPathID = 0
    process(composition.root) {
        if (it == VisitStage.PRE) {
            when (this) {
                is GroupNode -> {
                    val attributes =
                            listOf(svgId, transform.svgTransform, svgAttributes)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" ")
                    sb.append("<g $attributes>\n")
                }
                is ShapeNode -> {
                    val fillAttribute = fill.let {
                        if (it is Color) it.color?.let { "fill=\"${it.svg}\"" } ?: "fill=\"none\"" else ""
                    }
                    val strokeAttribute = stroke.let {
                        if (it is Color) it.color?.let { "stroke=\"${it.svg}\"" } ?: "stroke=\"none\"" else ""
                    }
                    val strokeWidthAttribute = strokeWeight.let { if (it is StrokeWeight) "stroke-width=\"${it.weight}\"" else "" }
                    val transformAttribute = if (transform !== Matrix44.IDENTITY) "transform=\"${transform.svgTransform}\"" else ""
                    val pathAttribute = "d=\"${shape.svg}\""

                    val attributes = listOf(
                            svgId,
                            transformAttribute,
                            fillAttribute,
                            strokeAttribute,
                            strokeWidthAttribute,
                            svgAttributes,
                            pathAttribute)
                            .filter { it.isNotBlank() }
                            .joinToString(" ")

                    sb.append("<path $attributes/>\n")
                }

                is TextNode -> {
                    val fillAttribute = fill.let { color ->
                        if (color is Color) color.color?.let { "fill=\"${it.svg}\"" } ?: "fill=\"none\"" else ""
                    }
                    val contour = this.contour
                    val escapedText = Entities.escape(this.text)
                    if (contour == null) {
                        sb.append("<text $svgId $fillAttribute $svgAttributes>$escapedText</text>")
                    } else {
                        sb.append("<defs>")
                        sb.append("<path id=\"text$textPathID\" d=\"${contour.svg}\"/>")
                        sb.append("</defs>")
                        sb.append("<text $fillAttribute><textPath href=\"#text$textPathID\">$escapedText</textPath></text>")
                        textPathID++
                    }
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

private val Matrix44.svgTransform get() = if (this == Matrix44.IDENTITY) "" else "transform=\"matrix(${this.c0r0}, ${this.c0r1}, ${this.c1r0}, ${this.c1r1}, ${this.c3r0}, ${this.c3r1})\""

private val Shape.svg: String
    get() {
        val sb = StringBuilder()
        contours.forEach {
            it.segments.forEachIndexed { index, segment ->
                if (index == 0) {
                    sb.append("M ${segment.start.x}, ${segment.start.y}")
                }
                sb.append(when (segment.control.size) {
                    1 -> "Q${segment.control[0].x}, ${segment.control[0].y}, ${segment.end.x}, ${segment.end.y}"
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

private val ShapeContour.svg: String
    get() {
        val sb = StringBuilder()
        segments.forEachIndexed { index, segment ->
            if (index == 0) {
                sb.append("M ${segment.start.x}, ${segment.start.y}")
            }
            sb.append(when (segment.control.size) {
                1 -> "C${segment.control[0].x}, ${segment.control[0].y}, ${segment.end.x}, ${segment.end.y}"
                2 -> "C${segment.control[0].x}, ${segment.control[0].y}, ${segment.control[1].x}, ${segment.control[1].y}, ${segment.end.x}, ${segment.end.y}"
                else -> "L${segment.end.x}, ${segment.end.y}"
            })
        }
        if (closed) {
            sb.append("Z ")
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