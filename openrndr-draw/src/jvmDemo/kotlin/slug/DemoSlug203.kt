package slug

import FaceFreetype
import FontDriverFreetype
import TextShapingDriverHarfBuzz
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.MagnifyingFilter
import org.openrndr.draw.MinifyingFilter
import org.openrndr.draw.VertexElementType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.shadeStyle
import org.openrndr.draw.slug.SlugCommand
import org.openrndr.draw.slug.SlugDrawer
import org.openrndr.draw.slug.SlugGlyphMap
import org.openrndr.draw.slug.SlugMap
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            FontDriver.driver = FontDriverFreetype()

            val pface = loadFace("data/fonts/NotoSansKR-VariableFont_wght.ttf", 32.0, 1.0)
//            val m = pface.master

            val faces = (0 until 9
                    ).map {
                val m = pface.master
                m["Weight"] = ((it+1) * 100).toDouble()
                pface.withMaster(m)
            }

            val slugMap = SlugMap(
                colorBuffer(4096, 64, type = ColorType.FLOAT32, format = ColorFormat.RG),
                colorBuffer(4096, 64, type = ColorType.UINT16_INT, format = ColorFormat.RGBa)
            )

            val slugGlyphMap = SlugGlyphMap(slugMap)

            val slugDrawer = SlugDrawer()

            val texts = listOf(
                "ENTERTAIN US",
                "こんにちは世界！",
                "你好世界！",
                "안안안안안안녕하세요! 안녕하세요!",
                "안안안안안안녕하세요! 안녕하세요!",
                "세상에, 저런 달팽이 상형",
                "HERE WE ARE NOWaaa",
                "ENTERTAIN US",
                "Hello, hello, hello, how low?\n" +
                        "Hello, hello, hello, how low?\n" +
                        "Hello, hello, hello, how low?\n" +
                        "Hello, hello, hello",
                "HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE",
                "HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE \"HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE \"HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE \"HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE \"HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE \"HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE \"HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHEHELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE \"HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE \"HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHEHELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE \"HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE \"HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE",
            )

            val shaper = TextShapingDriverHarfBuzz()
            val shapeResults = texts.mapIndexed { index, it ->  shaper.shape(faces[index.mod(faces.size)], it) }


            for ((index, shapeResult) in shapeResults.withIndex()) {
                for (item in shapeResult) {
                    slugGlyphMap.getSlugForGlyphIndex(faces[index.mod(faces.size)], item.glyphIndex)
                }
            }

            val commands = mutableListOf<SlugCommand>()

            for ((index, shapeResult) in shapeResults.withIndex()) {
                var cursor = Vector2(0.0, (index + 1) * faces[index.mod(faces.size)].height)

                for ((sindex, i) in shapeResult.withIndex()) {
                    val slugIndex = slugGlyphMap.getSlugForGlyphIndex(faces[index.mod(faces.size)], i.glyphIndex)
                    val command = SlugCommand(
                        slugIndex,
                        transform {
                            translate(cursor + i.offset * 0.0)
                        },
                        drawer.fill ?: ColorRGBa.TRANSPARENT,
                        drawer.stroke ?: ColorRGBa.TRANSPARENT,
                        drawer.strokeWeight

                    )
                    commands.add(command)
                    cursor += shapeResult[sindex].advance
                }
            }
            slugMap.bands.filter(MinifyingFilter.NEAREST, MagnifyingFilter.NEAREST)

            slugDrawer.prepare(slugMap, commands)
            extend {
                slugDrawer.draw(drawer, slugMap)
            }
        }
    }
}