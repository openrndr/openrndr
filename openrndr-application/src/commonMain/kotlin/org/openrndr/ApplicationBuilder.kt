package org.openrndr

expect class ApplicationBuilder internal constructor(){
    internal val configuration: Configuration
    var program: Program
    internal val application: ApplicationBase

    fun configure(init: Configuration.() -> Unit)

    fun program(init: suspend Program.() -> Unit)

    @Deprecated("Cannot construct application in an application block.", level = DeprecationLevel.ERROR)
    fun application(build: ApplicationBuilder.() -> Unit): Nothing

    @Deprecated("Cannot construct application in an application block.", level = DeprecationLevel.ERROR)
    fun applicationAsync(build: ApplicationBuilder.() -> Unit): Nothing

    @Deprecated("Cannot construct program in a program block.", level = DeprecationLevel.ERROR)
    fun Program.program(init: Program.() -> Unit): Nothing
}

expect fun application(build: ApplicationBuilder.() -> Unit)

expect suspend fun applicationAsync(build: ApplicationBuilder.() -> Unit)
