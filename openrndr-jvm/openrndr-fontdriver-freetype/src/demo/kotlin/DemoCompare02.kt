import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.fontdriver.freetype.FontDriverFreetype
import org.openrndr.fontdriver.stb.FontDriverStbTt
import java.io.File
import kotlin.math.abs
import kotlin.math.max

/**
 * Renders the same text through [FontImageMap]s built by [FontDriverStbTt] and [FontDriverFreetype]
 * and writes the renders, an overlay (stb in red, freetype in cyan) and metric differences to `build/font-compare/<font>`.
 * The font can be set with `-Dfont=<file>`.
 */
fun main() {
    application {
        configure {
            width = 200
            height = 200
            hideWindowDecorations = true
        }
        program {
            val fontFile = System.getProperty("font", "data/fonts/default.otf")
            val outputDir = File("build/font-compare/${File(fontFile).nameWithoutExtension}").apply { mkdirs() }
            val sizes = listOf(10.0, 12.0, 16.0, 24.0, 36.0, 64.0)
            val lines = listOf(
                "The quick brown fox jumps over the lazy dog 0123456789",
                "AVATAR To Wa Yo LT Hamburgefonstiv (gjpqy) [ÅÉÎ]"
            )
            val drivers = listOf("stb" to FontDriverStbTt(), "freetype" to FontDriverFreetype())
            val panelWidth = 1400
            val panelHeight = sizes.sumOf { it * 1.6 * lines.size }.toInt() + 40
            val report = StringBuilder()

            for (contentScale in listOf(1.0, 2.0)) {
                val renders = mutableMapOf<String, ColorBuffer>()
                val fontMaps = mutableMapOf<String, Map<Double, FontImageMap>>()

                for ((name, driver) in drivers) {
                    FontDriver.driver = driver
                    tearDownFontImageMaps()
                    val maps = sizes.associateWith { loadFontImageMap(fontFile, it, contentScale = contentScale) }
                    fontMaps[name] = maps

                    val rt = renderTarget(panelWidth, panelHeight, contentScale = contentScale) {
                        colorBuffer()
                        depthBuffer()
                    }
                    drawer.isolatedWithTarget(rt) {
                        ortho(rt)
                        clear(ColorRGBa.BLACK)
                        fill = ColorRGBa.WHITE
                        var y = 20.0
                        for (size in sizes) {
                            fontMap = maps.getValue(size)
                            for (line in lines) {
                                y += size * 1.6
                                text(line, 10.0, y)
                            }
                        }
                    }
                    rt.colorBuffer(0).saveToFile(File(outputDir, "${name}-x${contentScale.toInt()}.png"), async = false)
                    maps.getValue(24.0).texture.saveToFile(
                        File(outputDir, "${name}-x${contentScale.toInt()}-atlas24.png"), async = false
                    )
                    renders[name] = rt.colorBuffer(0)
                }

                // pixel comparison and overlay
                val stb = renders.getValue("stb")
                val ft = renders.getValue("freetype")
                stb.shadow.download()
                ft.shadow.download()
                val overlay = colorBuffer(stb.effectiveWidth, stb.effectiveHeight, contentScale = 1.0)
                var sumDiff = 0.0
                var maxDiff = 0.0
                var differing = 0
                var inkStb = 0.0
                var inkFt = 0.0
                for (y in 0 until stb.effectiveHeight) {
                    for (x in 0 until stb.effectiveWidth) {
                        val a = stb.shadow[x, y].r
                        val b = ft.shadow[x, y].r
                        val d = abs(a - b)
                        sumDiff += d
                        maxDiff = max(maxDiff, d)
                        if (d > 0.1) differing++
                        inkStb += a
                        inkFt += b
                        overlay.shadow[x, y] = ColorRGBa(a, b, b, 1.0)
                    }
                }
                overlay.shadow.upload()
                overlay.saveToFile(File(outputDir, "overlay-x${contentScale.toInt()}.png"), async = false)
                val pixels = stb.effectiveWidth * stb.effectiveHeight

                report.appendLine("=== contentScale $contentScale ===")
                report.appendLine(
                    "pixels: mean |diff| %.5f, max |diff| %.3f, pixels differing >0.1: %d (%.3f%%), ink stb %.0f freetype %.0f (ratio ft/stb %.3f)".format(
                        sumDiff / pixels, maxDiff, differing, 100.0 * differing / pixels, inkStb, inkFt, inkFt / inkStb
                    )
                )

                // metric comparison
                for (size in sizes) {
                    val ms = fontMaps.getValue("stb").getValue(size)
                    val mf = fontMaps.getValue("freetype").getValue(size)
                    report.appendLine("-- size $size")
                    report.appendLine(
                        "   ascender  stb %8.3f  ft %8.3f\n   descender stb %8.3f  ft %8.3f\n   height    stb %8.3f  ft %8.3f\n   leading   stb %8.3f  ft %8.3f\n   atlas     stb %dx%d  ft %dx%d".format(
                            ms.ascenderLength, mf.ascenderLength,
                            ms.descenderLength, mf.descenderLength,
                            ms.height, mf.height,
                            ms.leading, mf.leading,
                            ms.texture.effectiveWidth, ms.texture.effectiveHeight,
                            mf.texture.effectiveWidth, mf.texture.effectiveHeight
                        )
                    )
                    val chars = lines.joinToString("").toSet().filter { it in ms.glyphMetrics && it in mf.glyphMetrics }
                    val advanceDiffs = chars.map { c ->
                        c to (mf.glyphMetrics.getValue(c).advanceWidth - ms.glyphMetrics.getValue(c).advanceWidth)
                    }
                    val lineWidths = lines.map { line ->
                        fun width(m: FontImageMap) = line.sumOf { m.glyphMetrics[it]?.advanceWidth ?: 0.0 } +
                                line.zipWithNext().sumOf { (l, r) -> m.kerning(l, r) }
                        width(ms) to width(mf)
                    }
                    report.appendLine(
                        "   advance diff (ft - stb): max |%.3f| mean %.3f; worst: %s".format(
                            advanceDiffs.maxOf { abs(it.second) },
                            advanceDiffs.sumOf { it.second } / advanceDiffs.size,
                            advanceDiffs.sortedByDescending { abs(it.second) }.take(4)
                                .joinToString { "'${it.first}' %.3f".format(it.second) }
                        )
                    )
                    for ((pair, w) in lines.zip(lineWidths)) {
                        report.appendLine("   line width stb %8.2f ft %8.2f  (\"%s…\")".format(w.first, w.second, pair.take(12)))
                    }
                    val kernPairs = listOf("AV", "VA", "To", "Wa", "Yo", "LT")
                    report.appendLine("   kerning " + kernPairs.joinToString { p ->
                        "$p stb %.2f ft %.2f".format(ms.kerning(p[0], p[1]), mf.kerning(p[0], p[1]))
                    })
                    val kernDiffs = lines.flatMap { it.zipWithNext() }.distinct().map { (l, r) ->
                        Triple("$l$r", ms.kerning(l, r), mf.kerning(l, r))
                    }.filter { abs(it.second - it.third) > 0.05 }.sortedByDescending { abs(it.second - it.third) }
                    report.appendLine("   kerning differences >0.05: ${kernDiffs.size}; worst: " + kernDiffs.take(8).joinToString {
                        "'${it.first}' stb %.2f ft %.2f".format(it.second, it.third)
                    })
                    val bitmapShifts = listOf('H', 'g', 'o').joinToString { c ->
                        val s = ms.glyphMetrics.getValue(c)
                        val f = mf.glyphMetrics.getValue(c)
                        "'$c' stb (%.0f,%.0f) ft (%.0f,%.0f) lsb stb %.2f ft %.2f".format(
                            s.xBitmapShift, s.yBitmapShift, f.xBitmapShift, f.yBitmapShift,
                            s.leftSideBearing, f.leftSideBearing
                        )
                    }
                    report.appendLine("   bitmap shift/lsb $bitmapShifts")
                }
            }
            File(outputDir, "report.txt").writeText(report.toString())
            println(report)
            application.exit()
        }
    }
}
