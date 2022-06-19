package org.openrndr

/**
 * This is accessible before finalizing the application in ApplicationBuilder.
 */
expect abstract class ApplicationBase {
    abstract fun build(program: Program, configuration: Configuration): Application
}