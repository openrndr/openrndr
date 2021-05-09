package org.openrndr

/**
 * Runs [programFunction] as an application using [configuration], this provides a more functional flavored way of
 * writing applications.
 */
actual suspend fun application(build: ApplicationBuilder.() -> Unit) {
    val applicationBuilder = ApplicationBuilder().apply { build() }
    application(applicationBuilder.program, applicationBuilder.configuration)
}