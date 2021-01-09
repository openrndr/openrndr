package org.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa

fun main() {
    application {
        program {
            extend {
                drawer.clear(ColorRGBa.PINK)
            }
        }
    }
}