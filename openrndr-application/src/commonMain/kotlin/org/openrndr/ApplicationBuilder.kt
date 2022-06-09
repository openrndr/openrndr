package org.openrndr

@ApplicationDslMarker
expect class ApplicationBuilder internal constructor(){
    val application: Application
    internal val configuration: Configuration
    var program: Program

    fun configure(init: Configuration.() -> Unit)

    fun program(init: suspend Program.() -> Unit)
}

expect fun application(build: ApplicationBuilder.() -> Unit)

expect suspend fun applicationAsync(build: ApplicationBuilder.() -> Unit)
