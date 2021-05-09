package org.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa

suspend fun main() {
    application {
        program {
            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.circle(width/2.0, height/2.0, 100.0)
            }
        }
    }
}