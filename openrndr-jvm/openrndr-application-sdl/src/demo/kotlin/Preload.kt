package org.openrndr

//import org.openrndr.draw.BufferMultisample
//import org.openrndr.extensions.Screenshots
//import org.openrndr.ffmpeg.ScreenRecorder

/**
 * To test Preload with SDL:
 *
 * - Uncomment the imports above and the code below.
 * - In `openrndr-application-sdl/build.gradle.kt`, uncomment
 *   ```
 *   //demoImplementation(project(":openrndr-extensions"))
 *   //demoImplementation(project(":openrndr-jvm:openrndr-ffmpeg"))
 *   ```
 * - Reload Gradle.
 * - Run `DemoApplicationSdl01.kt`.
 *
 * You should be able to take screenshots by pressing the space key,
 * toggle video recording by pressing `v` and
 * quit by pressing the Escape key.
 */

class Preload : ApplicationPreload() {
//    override fun onProgramSetup(program: Program) {
//        val screenRecorder = ScreenRecorder().apply {
//            outputToVideo = false
//            frameClock = false
//            frameRate = 60
//        }
//        program.extend(screenRecorder)
//        program.extend(Screenshots()) {
//            contentScale = 2.0
//            multisample = BufferMultisample.SampleCount(4)
//        }
//        program.keyboard.keyDown.listen {
//            when {
//                it.key == KEY_ESCAPE -> program.application.exit()
//                it.name == "v" && it.modifiers.isEmpty() -> {
//                    screenRecorder.outputToVideo = !screenRecorder.outputToVideo
//                    program.application.configuration.vsync = !screenRecorder.outputToVideo
//                    println("ScreenRecorder: ${if (screenRecorder.outputToVideo) "ON" else "OFF"}")
//                }
//            }
//        }
//    }
}