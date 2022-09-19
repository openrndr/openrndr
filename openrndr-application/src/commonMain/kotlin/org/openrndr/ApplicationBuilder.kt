package org.openrndr

abstract class ApplicationBuilder {
    abstract val configuration: Configuration
    abstract var program: Program

    abstract val applicationBase: ApplicationBase

    abstract fun configure(init: Configuration.() -> Unit)

    abstract fun program(init: suspend Program.() -> Unit): Program

    abstract val displays: List<Display>


    @Deprecated("Cannot construct application in an application block.", level = DeprecationLevel.ERROR)
    abstract fun application(build: ApplicationBuilder.() -> Unit): Nothing

    @Deprecated("Cannot construct application in an application block.", level = DeprecationLevel.ERROR)
    abstract fun applicationAsync(build: ApplicationBuilder.() -> Unit): Nothing


    @Deprecated("Cannot construct program in a program block.", level = DeprecationLevel.ERROR)
    abstract fun Program.program(init: Program.() -> Unit): Nothing
}

expect fun application(build: ApplicationBuilder.() -> Unit)

expect suspend fun applicationAsync(build: ApplicationBuilder.() -> Unit)
