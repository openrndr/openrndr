package org.openrndr.webgl

import org.openrndr.Application
import org.openrndr.ApplicationBase
import org.openrndr.Configuration
import org.openrndr.Program
import web.console.console

actual class ApplicationBaseWebGL : ApplicationBase() {
    actual override fun build(program: Program, configuration: Configuration): Application {
        // We need this here to make sure [applicationBaseFunc] is initialized.
        applicationBaseWebGLInitializer
        console.log("building wasm application")
        return ApplicationWebGL(program, configuration)
    }
}