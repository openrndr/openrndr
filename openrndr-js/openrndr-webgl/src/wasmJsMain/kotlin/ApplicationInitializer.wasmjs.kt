package org.openrndr.webgl

import org.openrndr.applicationBaseFunc

// This is a hack to get [applicationFunc] initialized. Perhaps there's a better way to do this,
// but this ensures the property is not removed by DCE and gets initialized eagerly.
@OptIn(ExperimentalStdlibApi::class, ExperimentalJsExport::class)
@EagerInitialization
val applicationBaseWebGLInitializer = object {
    init {
        applicationBaseFunc = ::ApplicationBaseWebGL
    }
}
