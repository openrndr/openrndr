package org.openrndr

/**
 * Runs [programFunction] as an application using [configuration], this provides a more functional flavored way of
 * writing applications.
 */
actual fun application(build: ApplicationBuilder.() -> Unit) {
    error("use applicationAsync")
}

/**
 * Runs [programFunction] as an application using [configuration], this provides a more functional flavored way of
 * writing applications.
 */
actual suspend fun applicationAsync(build: ApplicationBuilder.() -> Unit) {
    val applicationBuilder = ApplicationBuilder().apply { build() }
    applicationAsync(applicationBuilder.program, applicationBuilder.configuration)
}