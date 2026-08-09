package slug

import org.openrndr.application
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.slug.SlugTextDrawer
import org.openrndr.draw.slug.TextStyle
import org.openrndr.fontdriver.freetype.FontDriverFreetype
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.textshapingdriver.harfbuzz.TextShapingDriverHarfBuzz

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

            slugTextDrawer.addText("Hey hoe gaat het?",
                Rectangle(10.0, 360.0, 350.0, 720.0), TextStyle(face = pface)
            )
            extend {
                slugTextDrawer.draw(drawer)
            }
        }
    }
}