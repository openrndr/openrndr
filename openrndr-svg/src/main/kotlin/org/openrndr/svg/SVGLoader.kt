package org.openrndr.svg

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.shape.*
import java.util.regex.Pattern

fun loadSVG(svgString: String): Composition {
    val doc = SVGLoader().loadSVG(svgString)
    return doc.composition()
}

internal class Command(val op: String, vararg val operands: Double) {
    fun vector(i0: Int, i1: Int): Vector2 {
        val x = if (i0 == -1) 0.0 else operands[i0]
        val y = if (i1 == -1) 0.0 else operands[i1]
        return Vector2(x, y)
    }

    fun vectors(): List<Vector2> = (0 until operands.size / 2).map { Vector2(operands[it * 2], operands[it * 2 + 1]) }
}

internal sealed class SVGElement {
    var transform = Matrix44.IDENTITY
    var id: String? = null

    fun parseTransform(e: Element) {
        val p = Pattern.compile("(matrix|translate|scale|rotate|skewX|skewY)\\(.+\\)")
        val m = p.matcher(e.attr("transform"))

        fun getTransformOperands(token: String): List<Double> {
            val number = Pattern.compile("-?[0-9\\.eE\\-]+")
            val nm = number.matcher(token)
            val operands = mutableListOf<Double>()
            while (nm.find()) {
                val n = nm.group().toDouble()
                operands.add(n)
            }
            return operands
        }

        while (m.find()) {
            val token = m.group()
            if (token.startsWith("matrix")) {
                val operands = getTransformOperands(token)
                val mat = Matrix44(
                        operands[0], operands[2], 0.0, operands[4],
                        operands[1], operands[3], 0.0, operands[5],
                        0.0, 0.0, 1.0, 0.0,
                        0.0, 0.0, 0.0, 1.0)
                transform = mat
            }
            if (token.startsWith("scale")) {
                val operands = getTransformOperands(token.substring(5))
                val mat = Matrix44(operands[0], 0.0, 0.0, 0.0,
                        0.0, operands[1], 0.0, 0.0,
                        0.0, 0.0, 1.0, 0.0,
                        0.0, 0.0, 0.0, 1.0)
                transform = mat
            }
        }
    }
}

internal class SVGImage(val url: String) : SVGElement()
internal class SVGGroup(val elements: MutableList<SVGElement> = mutableListOf()) : SVGElement()

internal fun parseColor(scolor: String): ColorRGBa? {
    if (scolor.isEmpty() || scolor == "none") return null
    val normalisedColor = normalizeColorHex(scolor)
    val v = java.lang.Long.decode(normalisedColor)
    val vi = v.toInt()
    val r = vi shr 16 and 0xff
    val g = vi shr 8 and 0xff
    val b = vi and 0xff
    return ColorRGBa(r / 255.0, g / 255.0, b / 255.0, 1.0)
}

fun normalizeColorHex(colorHex: String): String {
    val colorHexRegex = "#?([0-9a-f]{3,6})".toRegex(RegexOption.IGNORE_CASE)

    val matchResult = colorHexRegex.matchEntire(colorHex)
        ?: throw RuntimeException("The provided colorHex '$colorHex' is not a valid color hex for the SVG spec")

    val hexValue = matchResult.groups[1]!!.value.toLowerCase()
    val normalizedArgb = when (hexValue.length) {
        3 -> expandToTwoDigitsPerComponent("f$hexValue")
        6 -> "$hexValue"
        else -> throw RuntimeException("The provided colorHex '$colorHex' is not in a supported format")
    }

    return "#$normalizedArgb"
}

fun expandToTwoDigitsPerComponent(hexValue: String) =
    hexValue.asSequence()
        .map { "$it$it" }
        .reduce { accumulatedHex, component -> accumulatedHex + component }

internal class SVGPath : SVGElement() {
    val commands = mutableListOf<Command>()
    var fill: CompositionColor = InheritColor
    var stroke: CompositionColor = InheritColor
    var strokeWeight: Double? = null

    companion object {
        fun fromSVGPathString(svgPath: String): SVGPath {
            val path = SVGPath()
            val rawCommands = svgPath.split("(?=[MmZzLlHhVvCcSsQqTtAa])".toRegex()).dropLastWhile({ it.isEmpty() })
            val numbers = Pattern.compile("[-+]?[0-9]*[.]?[0-9]+(?:[eE][-+]?[0-9]+)?")

            for (rawCommand in rawCommands) {
                if (rawCommand.isNotEmpty()) {
                    val numberMatcher = numbers.matcher(rawCommand)
                    val operands = mutableListOf<Double>()
                    while (numberMatcher.find()) {
                        operands.add(numberMatcher.group().toDouble())
                    }
                    path.commands.add(Command(rawCommand[0].toString(), *(operands.toDoubleArray())))
                }
            }
            return path
        }
    }

    fun compounds(): List<SVGPath> {
        val compounds = mutableListOf<SVGPath>()
        val compoundIndices = mutableListOf<Int>()

        commands.forEachIndexed { index, it ->
            if (it.op == "M" || it.op == "m") {
                compoundIndices.add(index)
            }
        }

        compoundIndices.forEachIndexed { index, _ ->
            val cs = compoundIndices[index]
            val ce = if (index + 1 < compoundIndices.size) (compoundIndices[index + 1]) else commands.size

            val path = SVGPath()
            path.commands.addAll(commands.subList(cs, ce))

            if (compoundIndices.size > 1) {
                path.commands.add(Command("Z"))
            }
            compounds.add(path)
        }
        return compounds
    }

    fun shape(): Shape {
        var cursor = Vector2(0.0, 0.0)
        var anchor = cursor.copy()
        var relativeControl = Vector2(0.0, 0.0)

        val contours = compounds().mapIndexed { compoundIndex, compound ->
            val segments = mutableListOf<Segment>()
            var closed = false
            compound.commands.forEach { command ->

                when (command.op) {
                    "M" -> {
                        cursor = command.vector(0, 1)
                        anchor = cursor

                        val allPoints = command.vectors()

                        for (i in 1 until allPoints.size) {
                            val point = allPoints[i]
                            segments += Segment(cursor, point)
                            cursor = point
                        }
                    }
                    "m" -> {
                        val allPoints = command.vectors()
                        cursor += command.vector(0, 1)
                        anchor = cursor

                        for (i in 1 until allPoints.size) {
                            val point = allPoints[i]
                            segments += Segment(cursor, cursor + point)
                            cursor += point
                        }
                    }
                    "L" -> {
                        val allPoints = command.vectors()

                        for (point in allPoints) {
                            segments += Segment(cursor, point)
                            cursor = point
                        }
                    }
                    "l" -> {
                        val allPoints = command.vectors()

                        for (point in allPoints) {
                            val target = cursor + point
                            segments += Segment(cursor, target)
                            cursor = target
                        }
                    }
                    "h" -> {
                        for (i in 0 until command.operands.size) {
                            val startCursor = cursor
                            val target = startCursor + Vector2(command.operands[i], 0.0)
                            segments += Segment(cursor, target)
                            cursor = target
                        }
                    }
                    "H" -> {
                        for (i in 0 until command.operands.size) {

                            val target = Vector2(command.operands[i], cursor.y)
                            segments += Segment(cursor, target)
                            cursor = target
                        }
                    }
                    "v" -> {
                        for (i in 0 until command.operands.size) {
                            val target = cursor + Vector2(0.0, command.operands[i])
                            segments += Segment(cursor, target)
                            cursor = target
                        }
                    }
                    "V" -> {
                        for (i in 0 until command.operands.size) {

                            val target = Vector2(cursor.x, command.operands[i])
                            segments += Segment(cursor, target)
                            cursor = target
                        }
                    }
                    "C" -> {
                        val points = command.vectors()
                        segments += Segment(cursor, points[0], points[1], points[2])
                        cursor = points[2]
                        relativeControl = points[1] - points[2]
                    }
                    "c" -> {
                        val points = command.vectors()
                        segments += Segment(cursor, cursor + points[0], cursor + points[1], cursor.plus(points[2]))
                        relativeControl = (cursor + points[1]) - (cursor + points[2])
                        cursor += points[2]
                    }
                    "Q" -> {
                        val allPoints = command.vectors()
                        if ((allPoints.size) % 2 != 0) {
                            throw RuntimeException("invalid number of operands ${allPoints.size}")
                        }
                        for (c in 0 until allPoints.size / 2) {
                            val points = allPoints.subList(c * 2, c * 2 + 2)

                            segments += Segment(cursor, points[0], points[1])
                            cursor = points[1]
                            relativeControl = points[0] - points[1]
                        }
                    }
                    "q" -> {
                        val allPoints = command.vectors()
                        if ((allPoints.size) % 2 != 0) {
                            throw RuntimeException("invalid number of operands ${allPoints.size}")
                        }
                        for (c in 0 until allPoints.size / 2) {
                            val points = allPoints.subList(c * 2, c * 2 + 2)
                            val target = cursor + points[1]
                            segments += Segment(cursor, cursor + points[0], target)
                            relativeControl = (cursor + points[0]) - (cursor + points[1])
                            cursor = target
                        }
                    }
                    "s" -> {
                        val reflected = relativeControl * -1.0
                        val cp0 = cursor + reflected
                        val cp1 = cursor + command.vector(0, 1)
                        val target = cursor + command.vector(2, 3)
                        segments += Segment(cursor, cp0, cp1, target)
                        cursor = target
                        relativeControl = cp1 - target
                    }
                    "S" -> {
                        val reflected = relativeControl * -1.0
                        val cp0 = cursor + reflected
                        val cp1 = command.vector(0, 1)
                        val target = command.vector(2, 3)
                        segments += Segment(cursor, cp0, cp1, target)
                        cursor = target
                        relativeControl = cp1 - target
                    }
                    "Z", "z" -> {
                        if ((cursor - anchor).length >= 0.001) {
                            segments += Segment(cursor, anchor)
                        }
                        closed = true
                    }
                    else -> {
                        throw RuntimeException("unsupported op: ${command.op}")
                    }
                }
            }
            ShapeContour(segments, closed).let {
                if (compoundIndex == 0) it.counterClockwise else it.clockwise
            }
        }
        return Shape(contours)
    }

    fun parseDrawAttributes(e: Element) {
        if (e.hasAttr("fill")) {
            fill = Color(parseColor(e.attr("fill")))
        }

        if (e.hasAttr("stroke")) {
            stroke = Color(parseColor(e.attr("stroke")))
        }
        strokeWeight = e.attr("stroke-width").let {
            if (it.isEmpty()) null else it.toDouble()
        }

        e.attr("style").split(";").forEach {
            val tokens = it.split(":")
            val attribute = tokens[0].toLowerCase().trim()

            fun value(): String = if (tokens.size >= 2) {
                tokens[1].trim()
            } else {
                ""
            }

            when (attribute) {
                "fill" -> fill = Color(parseColor(value()))
                "stroke" -> stroke = Color(parseColor(value()))
                "stroke-width" -> strokeWeight = value().toDouble()
            }
        }
    }
}

internal class SVGDocument(private val root: SVGElement) {
    fun composition(): Composition = Composition(convertElement(root))

    private fun convertElement(e: SVGElement): CompositionNode = when (e) {
        is SVGGroup -> GroupNode().apply { e.elements.mapTo(children) { convertElement(it).also { it.parent = this@apply } } }
        is SVGPath -> {
            ShapeNode(e.shape()).apply {
                fill = e.fill
                stroke = e.stroke
            }
        }
        is SVGImage -> {
            val group = GroupNode()
            group
        }
    }.apply {
        transform = e.transform
    }
}

internal class SVGLoader {
    fun loadSVG(svg: String): SVGDocument {
        val doc = Jsoup.parse(svg, "", Parser.xmlParser())
        val root = doc.select("svg").first()
        val version = root.attr("version")
        val baseProfile = root.attr("baseProfile")
        val supportedVersions = setOf("1.0", "1.1", "1.2")

        if (version !in supportedVersions) {
            throw IllegalArgumentException("SVG version `$version` is not supported")
        }

        if (baseProfile != "tiny") {
            throw IllegalArgumentException("SVG base-profile `$baseProfile` is not supported")
        }

        val rootGroup = SVGGroup()
        handleGroup(rootGroup, root)
        return SVGDocument(rootGroup)
    }

    private fun handlePolygon(group: SVGGroup, e: Element) {
        val tokens = e.attr("points").split("[ ,\n]+".toRegex()).map { it.trim() }.filter { it.isNotEmpty() }
        val points = (0 until tokens.size / 2).map { Vector2(tokens[it * 2].toDouble(), tokens[it * 2 + 1].toDouble()) }
        val path = SVGPath().apply {
            id = e.id()
            parseDrawAttributes(e)
            parseTransform(e)
            commands.add(Command("M", points[0].x, points[0].y))
            (1 until points.size).mapTo(commands) { Command("L", points[it].x, points[it].y) }
            commands.add(Command("Z"))
        }
        group.elements.add(path)
    }

    private fun handlePolyline(group: SVGGroup, e: Element) {
        val tokens = e.attr("points").split("[ ,\n]+".toRegex()).map { it.trim() }.filter { it.isNotEmpty() }
        val points = (0 until tokens.size / 2).map { Vector2(tokens[it * 2].toDouble(), tokens[it * 2 + 1].toDouble()) }
        val path = SVGPath().apply {
            id = e.id()
            parseDrawAttributes(e)
            parseTransform(e)
            commands.add(Command("M", points[0].x, points[0].y))
            (1 until points.size).mapTo(commands) { Command("L", points[it].x, points[it].y) }
        }
        group.elements.add(path)
    }

    private fun handleGroup(parent: SVGGroup, e: Element) {
        val group = SVGGroup().apply {
            id = e.id()
            parseTransform(e)
        }
        parent.elements.add(group)
        e.children().forEach { c ->
            when (c.tagName()) {
                "g" -> handleGroup(group, c)
                "path" -> handlePath(group, c)
                "line" -> handleLine(group, c)
                "rect" -> handleRectangle(group, c)
                "ellipse" -> handleEllipse(group, c)
                "circle" -> handleCircle(group, c)
                "polygon" -> handlePolygon(group, c)
                "polyline" -> handlePolyline(group, c)
//                "image" -> TODO()
            }
        }
    }

    private fun handleEllipse(group: SVGGroup, e: Element) {
        var x = e.attr("cx").let { if (it.isEmpty()) 0.0 else it.toDouble() }
        var y = e.attr("cy").let { if (it.isEmpty()) 0.0 else it.toDouble() }
        val width = e.attr("rx").let { if (it.isEmpty()) 0.0 else it.toDouble() } * 2.0
        val height = e.attr("ry").let { if (it.isEmpty()) 0.0 else it.toDouble() } * 2.0
        x -= width / 2
        y -= height / 2

        val kappa = 0.5522848
        val ox = width / 2 * kappa
        // control point offset horizontal
        val oy = height / 2 * kappa
        // control point offset vertical
        val xe = x + width
        // x-end
        val ye = y + height
        // y-end
        val xm = x + width / 2
        // x-middle
        val ym = y + height / 2       // y-middle

        val path = SVGPath()
        path.id = e.id()
        path.parseDrawAttributes(e)
        path.parseTransform(e)
        path.commands.add(Command("M", x, ym))
        path.commands.add(Command("C", x, ym - oy, xm - ox, y, xm, y))
        path.commands.add(Command("C", xm + ox, y, xe, ym - oy, xe, ym))
        path.commands.add(Command("C", xe, ym + oy, xm + ox, ye, xm, ye))
        path.commands.add(Command("C", xm - ox, ye, x, ym + oy, x, ym))
        path.commands.add(Command("z"))
        group.elements.add(path)
    }

    private fun handleCircle(group: SVGGroup, e: Element) {
        var x = e.attr("cx").let { if (it.isEmpty()) 0.0 else it.toDouble() }
        var y = e.attr("cy").let { if (it.isEmpty()) 0.0 else it.toDouble() }
        val width = e.attr("r").let { if (it.isEmpty()) 0.0 else it.toDouble() } * 2.0
        val height = e.attr("r").let { if (it.isEmpty()) 0.0 else it.toDouble() } * 2.0
        x -= width / 2
        y -= height / 2

        val kappa = 0.5522848
        val ox = width / 2 * kappa
        // control point offset horizontal
        val oy = height / 2 * kappa
        // control point offset vertical
        val xe = x + width
        // x-end
        val ye = y + height
        // y-end
        val xm = x + width / 2
        // x-middle
        val ym = y + height / 2       // y-middle

        val path = SVGPath()
        path.id = e.id()
        path.parseDrawAttributes(e)
        path.parseTransform(e)
        path.commands.add(Command("M", x, ym))
        path.commands.add(Command("C", x, ym - oy, xm - ox, y, xm, y))
        path.commands.add(Command("C", xm + ox, y, xe, ym - oy, xe, ym))
        path.commands.add(Command("C", xe, ym + oy, xm + ox, ye, xm, ye))
        path.commands.add(Command("C", xm - ox, ye, x, ym + oy, x, ym))
        path.commands.add(Command("z"))
        group.elements.add(path)
    }

//    private fun handleImage(group: SVGGroup, e: Element) {
//        val width = e.attr("width").toDouble()
//        val height = e.attr("height").toDouble()
//        val url = e.attr("xlink:href")
//        val image = SVGImage(url).apply {
//            id = e.id()
//            parseTransform(e)
//        }
//        image.id = e.id()
//    }

    private fun handleRectangle(group: SVGGroup, e: Element) {
        val x = e.attr("x").let { if (it.isEmpty()) 0.0 else it.toDouble() }
        val y = e.attr("y").let { if (it.isEmpty()) 0.0 else it.toDouble() }
        val width = e.attr("width").toDouble()
        val height = e.attr("height").toDouble()

        val path = SVGPath().apply {
            id = e.id()
            parseTransform(e)
            parseDrawAttributes(e)
            commands.add(Command("M", x, y))
            commands.add(Command("h", width))
            commands.add(Command("v", height))
            commands.add(Command("h", -width))
            commands.add(Command("z"))
        }
        group.elements.add(path)
    }

    private fun handleLine(group: SVGGroup, e: Element) {
        val x1 = e.attr("x1").toDouble()
        val x2 = e.attr("x2").toDouble()
        val y1 = e.attr("y1").toDouble()
        val y2 = e.attr("y2").toDouble()

        val path = SVGPath().apply {
            parseDrawAttributes(e)
            commands.add(Command("M", x1, y1))
            commands.add(Command("L", x2, y2))
        }
        group.elements.add(path)
    }

    private fun handlePath(group: SVGGroup, e: Element) {
        val path = SVGPath.fromSVGPathString(e.attr("d")).apply {
            id = e.id()
            parseDrawAttributes(e)
            parseTransform(e)
        }
        group.elements.add(path)
    }
}