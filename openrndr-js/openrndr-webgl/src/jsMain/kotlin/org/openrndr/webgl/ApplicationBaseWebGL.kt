package org.openrndr.webgl

import org.openrndr.*

// This is a hack to get [applicationFunc] initialized. Perhaps there's a better way to do this,
// but this ensures the property is not removed by DCE and gets initialized eagerly.
@OptIn(ExperimentalStdlibApi::class, ExperimentalJsExport::class)
@EagerInitialization
@JsExport
val applicationBaseWebGLInitializer = object {
    init {
        console.log("setting up ApplicationBaseWebGL")
        applicationBaseFunc = ::ApplicationBaseWebGL
    }
}

class ApplicationBaseWebGL : ApplicationBase() {
    override fun build(program: Program, configuration: Configuration): Application {
        // We need this here to make sure [applicationBaseFunc] is initialized.
        applicationBaseWebGLInitializer
        return ApplicationWebGL(program, configuration)
    }
}