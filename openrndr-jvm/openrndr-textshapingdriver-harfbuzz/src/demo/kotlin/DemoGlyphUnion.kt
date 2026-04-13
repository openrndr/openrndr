import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.selfUnion
import org.openrndr.shape.union
import kotlin.math.roundToInt

fun Shape.selfUnion() : Shape {
    return selfUnion(this)
}

fun Shape.unionCompounds(): Shape {

    if (this.contours.size <= 1) return this.selfUnion()
    val compounds = this.splitCompounds()
    if (compounds.size <= 1) return this


    val s0 = compounds.first()
    val s2 = Shape(compounds.drop(1).flatMap { it.contours })
    val head = compounds.first()
    val a =  compounds.drop(1).fold(head) { acc, shape -> acc.union(shape) }
    return a.selfUnion()
}

fun main() {
    application {
        program {

            val fontDriver = FontDriverFreetype()
            FontDriver.driver = fontDriver
            val face = fontDriver.loadFace("data/fonts/NotoSansKR-VariableFont_wght.ttf", 128.0, RenderTarget.active.contentScale)

            val driver = TextShapingDriverHarfBuzz()
            val text = "오픈RNDR"
            val result = driver.shape(face, text)

            extend {
                drawer.clear(ColorRGBa.PINK)

                drawer.stroke = ColorRGBa.BLACK
                drawer.strokeWeight = 0.25
                drawer.translate(50.0, 200.0)

                val r2 = Rectangle.fromCenter(Vector2.ZERO, 10.0, 100.0).shape.transform(
                    buildTransform {
                        translate(mouse.position)
                        rotate(seconds * 45.0)
                    }
                )


                drawer.fontMap = loadFont("data/fonts/Platypi-Regular.ttf", 16.0, contentScale = 2.0)
                for (i in 0 until result.size) {
                    val glyph = face.glyphForIndex(result[i].glyphIndex)
                    val og = glyph.shape()
                    val comps = og.splitCompounds()
                    val su = og.unionCompounds()

                    drawer.isolated {
                        drawer.translate(result[i].offset)

                        drawer.shape(su)

                        drawer.translate(0.0, 200.0)
                        drawer.fill = ColorRGBa.WHITE.opacify(0.25)
                        for ((index, c) in comps.withIndex()) {
                            drawer.shape(c)
                            drawer.rectangle(c.bounds)
                            for ((cindex, contour) in c.contours.withIndex()) {
                                val pt = contour.equidistantPositionsWithT((contour.length/5.0).roundToInt())
                                for (p in pt) {
                                    drawer.lineSegment(p.first, p.first + contour.normal(p.second) * 10.0)
                                }

                            }
                            drawer.isolated {
                                drawer.fill = ColorRGBa.BLACK
                                drawer.text("${c.contours.size}", c.bounds.center)
                            }

                        }
                    }
                    drawer.isolated {

                        drawer.fill = ColorRGBa.BLACK
                        drawer.text("${og.contours.size}, ${og.splitCompounds().size}")
                        drawer.text("${su.contours.size}, ${su.splitCompounds().size}", 0.0, 20.0)
                    }
                    drawer.translate(result[i].advance)
                }
            }
        }
    }
}