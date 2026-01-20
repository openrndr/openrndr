package org.openrndr.webgl

import org.openrndr.*

expect class ApplicationBaseWebGL : ApplicationBase {
    override fun build(program: Program, configuration: Configuration): Application
}