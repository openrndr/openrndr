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
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.shadeStyle
import org.openrndr.draw.slug.SlugCommand
import org.openrndr.draw.slug.SlugDrawer
import org.openrndr.draw.slug.SlugGlyphMap
import org.openrndr.draw.slug.SlugMap
import org.openrndr.draw.slug.SlugTextDrawer
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
            TextShapingDriver.driver = TextShapingDriverHarfBuzz()

            val pface = loadFace("data/fonts/NotoSansKR-VariableFont_wght.ttf", 32.0, 1.0)
//            val m = pface.master

            val faces = (0 until 9
                    ).map {
                val m = pface.master
                m["Weight"] = ((it+1) * 100).toDouble()
                pface.withMaster(m)
            }


            val slugTextDrawer = SlugTextDrawer()

            val texts = listOf(
                "BRING THE FAMILY",
                "『가족과 함께 오세요"  ,
                "GET READY FOR PART 2",
                "파트 2를 기대하세요⟨。⟩",
                "ㄱㄴㄷㄹㅁㅂㅅㅇㅈㅊㅋㅌㅍㅎㅏㅑㅓㅕㅗㅛㅜㅠㅡㅣ",
                "ㅇㅅㅇ (ㅎㅅㅎ)",
                "○○○",
                "배운 사람 입에서 어찌 ○○○란 말이 나올 수 있느냐?"


            )

            var y = 0.0
            for (text in texts.withIndex()) {
                y += faces[text.index.mod(faces.size)].height * 1.2
                slugTextDrawer.addText(faces[text.index.mod(faces.size)], text.value,
                    Vector2(10.0, y))
            }

            extend {
                slugTextDrawer.draw(drawer)
            }
        }
    }
}