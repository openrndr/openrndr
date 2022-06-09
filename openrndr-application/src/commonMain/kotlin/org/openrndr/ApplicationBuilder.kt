package org.openrndr

@ApplicationDslMarker
expect class ApplicationBuilder internal constructor(){
    val application: Application
    internal val configuration: Configuration
    var program: Program

    fun configure(init: Configuration.() -> Unit)

    fun program(init: suspend Program.() -> Unit)
}

/**
 * Runs [programFunction] as an application using [configuration], this provides a more functional flavored way of
 * writing applications.
 */
expect fun application(build: ApplicationBuilder.() -> Unit)


/**
 * Runs [programFunction] as an application using [configuration], this provides a more functional flavored way of
 * writing applications.
 */
expect suspend fun applicationAsync(build: ApplicationBuilder.() -> Unit)

