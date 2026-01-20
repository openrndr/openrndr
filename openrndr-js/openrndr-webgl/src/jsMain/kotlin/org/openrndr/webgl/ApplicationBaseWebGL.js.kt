package org.openrndr.webgl

import org.openrndr.Application
import org.openrndr.ApplicationBase
import org.openrndr.Configuration
import org.openrndr.Program

actual class ApplicationBaseWebGL : ApplicationBase() {
    actual override fun build(program: Program, configuration: Configuration): Application {
        // We need this here to make sure [applicationBaseFunc] is initialized.
        applicationBaseWebGLInitializer
        console.log("building application")
        return ApplicationWebGL(program, configuration)
    }
}