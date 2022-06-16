package org.openrndr

/**
 * This is accessible before finalizing the application in ApplicationBuilder.
 */
actual abstract class ApplicationBase {
    actual abstract fun build(program: Program, configuration: Configuration): Application
}